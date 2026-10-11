package pub.galactic.chorahost.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import pub.galactic.chorahost.dto.ShopItemRequest;
import pub.galactic.chorahost.entity.ShopItem;
import pub.galactic.chorahost.entity.ShopPurchase;
import pub.galactic.chorahost.entity.TeamMembership;
import pub.galactic.chorahost.entity.User;
import pub.galactic.chorahost.exception.ApiException;
import pub.galactic.chorahost.repository.ShopItemRepository;
import pub.galactic.chorahost.repository.ShopPurchaseRepository;
import pub.galactic.chorahost.repository.TeamMembershipRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ShopService {

    @Inject
    ShopItemRepository itemRepository;

    @Inject
    ShopPurchaseRepository purchaseRepository;

    @Inject
    TeamMembershipRepository membershipRepository;

    @Inject
    TeamService teamService;

    public List<ShopItem> listItems(UUID teamId, User requester) {
        teamService.requireMembership(teamId, requester.id);
        return itemRepository.findActiveByTeam(teamId);
    }

    @Transactional
    public ShopItem createItem(UUID teamId, User requester, ShopItemRequest request) {
        teamService.requireAdminOrTeamAdmin(teamId, requester);

        ShopItem item = new ShopItem();
        item.teamId = teamId;
        apply(item, request);
        itemRepository.persist(item);
        return item;
    }

    @Transactional
    public ShopItem updateItem(UUID teamId, UUID itemId, User requester, ShopItemRequest request) {
        teamService.requireAdminOrTeamAdmin(teamId, requester);
        ShopItem item = itemRepository.findActiveInTeamForUpdate(itemId, teamId)
                .orElseThrow(() -> ApiException.notFound("Shop item not found: " + itemId));

        apply(item, request);
        return item;
    }

    /** Soft delete: past purchases keep referencing the item. */
    @Transactional
    public void deleteItem(UUID teamId, UUID itemId, User requester) {
        teamService.requireAdminOrTeamAdmin(teamId, requester);
        ShopItem item = itemRepository.findActiveInTeam(itemId, teamId)
                .orElseThrow(() -> ApiException.notFound("Shop item not found: " + itemId));

        item.active = false;
    }

    /**
     * Spends the buyer's gold in this team on {@code quantity} units. The item
     * and membership rows are locked so concurrent purchases can neither
     * oversell the stock nor spend the same gold twice.
     */
    @Transactional
    public ShopPurchase purchase(UUID teamId, UUID itemId, User buyer, int quantity) {
        teamService.requireMembership(teamId, buyer.id);
        ShopItem item = itemRepository.findActiveInTeamForUpdate(itemId, teamId)
                .orElseThrow(() -> ApiException.notFound("Shop item not found: " + itemId));
        TeamMembership membership = membershipRepository.findByUserAndTeamForUpdate(buyer.id, teamId)
                .orElseThrow(() -> ApiException.forbidden("Not a member of this team"));

        if (item.stock != null && item.stock < quantity) {
            throw ApiException.conflict("Not enough stock: " + item.stock + " left");
        }
        long total = (long) item.price * quantity;
        if (membership.gold < total) {
            throw ApiException.conflict("Not enough gold: need " + total + ", have " + membership.gold);
        }

        membership.gold -= (int) total;
        if (item.stock != null) {
            item.stock -= quantity;
        }

        ShopPurchase purchase = new ShopPurchase();
        purchase.teamId = teamId;
        purchase.userId = buyer.id;
        purchase.itemId = item.id;
        purchase.itemName = item.name;
        purchase.unitPrice = item.price;
        purchase.quantity = quantity;
        purchase.totalPrice = total;
        purchaseRepository.persist(purchase);
        return purchase;
    }

    /** Admins see every purchase in the team when {@code all} is set; everyone else sees their own. */
    public List<ShopPurchase> listPurchases(UUID teamId, User requester, boolean all) {
        if (all) {
            teamService.requireAdminOrTeamAdmin(teamId, requester);
            return purchaseRepository.findByTeam(teamId);
        }
        teamService.requireMembership(teamId, requester.id);
        return purchaseRepository.findByTeamAndUser(teamId, requester.id);
    }

    @Transactional
    public ShopPurchase setFulfilled(UUID teamId, UUID purchaseId, User requester, boolean fulfilled) {
        teamService.requireAdminOrTeamAdmin(teamId, requester);
        ShopPurchase purchase = purchaseRepository.findByIdOptional(purchaseId)
                .filter(p -> p.teamId.equals(teamId))
                .orElseThrow(() -> ApiException.notFound("Purchase not found: " + purchaseId));

        purchase.fulfilled = fulfilled;
        purchase.fulfilledAt = fulfilled ? Instant.now() : null;
        return purchase;
    }

    private static void apply(ShopItem item, ShopItemRequest request) {
        item.name = request.name();
        item.price = request.price();
        item.stock = request.stock();
        item.imageUrl = request.imageUrl();
    }
}
