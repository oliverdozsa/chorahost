package pub.galactic.chorahost.repository;

import java.util.List;
import java.util.UUID;

import pub.galactic.chorahost.entity.ShopPurchase;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShopPurchaseRepository implements PanacheRepositoryBase<ShopPurchase, UUID> {

    public List<ShopPurchase> findByTeam(UUID teamId) {
        return find("teamId = ?1 order by createdAt desc", teamId).list();
    }

    public List<ShopPurchase> findByTeamAndUser(UUID teamId, UUID userId) {
        return find("teamId = ?1 and userId = ?2 order by createdAt desc", teamId, userId).list();
    }
}
