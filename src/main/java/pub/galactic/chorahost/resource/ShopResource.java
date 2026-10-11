package pub.galactic.chorahost.resource;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import pub.galactic.chorahost.dto.PurchaseRequest;
import pub.galactic.chorahost.dto.SetFulfilledRequest;
import pub.galactic.chorahost.dto.ShopItemRequest;
import pub.galactic.chorahost.dto.ShopItemResponse;
import pub.galactic.chorahost.dto.ShopPurchaseResponse;
import pub.galactic.chorahost.entity.ShopItem;
import pub.galactic.chorahost.entity.ShopPurchase;
import pub.galactic.chorahost.entity.User;
import pub.galactic.chorahost.security.CurrentUserService;
import pub.galactic.chorahost.service.ShopService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/teams/{teamId}/shop")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ShopResource {

    @Inject
    ShopService shopService;

    @Inject
    CurrentUserService currentUserService;

    @GET
    @Path("/items")
    public List<ShopItemResponse> getItems(@PathParam("teamId") UUID teamId) {
        User requester = currentUserService.getOrCreateCurrentUser();
        return shopService.listItems(teamId, requester).stream().map(ShopItemResponse::from).toList();
    }

    @POST
    @Path("/items")
    public Response createItem(@PathParam("teamId") UUID teamId, @Valid ShopItemRequest request) {
        User requester = currentUserService.getOrCreateCurrentUser();
        ShopItem item = shopService.createItem(teamId, requester, request);
        return Response.created(URI.create("/api/v1/teams/" + teamId + "/shop/items/" + item.id))
                .entity(ShopItemResponse.from(item))
                .build();
    }

    @PUT
    @Path("/items/{itemId}")
    public ShopItemResponse updateItem(@PathParam("teamId") UUID teamId, @PathParam("itemId") UUID itemId,
            @Valid ShopItemRequest request) {
        User requester = currentUserService.getOrCreateCurrentUser();
        return ShopItemResponse.from(shopService.updateItem(teamId, itemId, requester, request));
    }

    @DELETE
    @Path("/items/{itemId}")
    public Response deleteItem(@PathParam("teamId") UUID teamId, @PathParam("itemId") UUID itemId) {
        User requester = currentUserService.getOrCreateCurrentUser();
        shopService.deleteItem(teamId, itemId, requester);
        return Response.noContent().build();
    }

    @POST
    @Path("/items/{itemId}/purchase")
    public Response purchase(@PathParam("teamId") UUID teamId, @PathParam("itemId") UUID itemId,
            @Valid PurchaseRequest request) {
        User requester = currentUserService.getOrCreateCurrentUser();
        int quantity = request == null ? 1 : request.quantityOrOne();
        ShopPurchase purchase = shopService.purchase(teamId, itemId, requester, quantity);
        return Response.created(URI.create("/api/v1/teams/" + teamId + "/shop/purchases/" + purchase.id))
                .entity(ShopPurchaseResponse.from(purchase))
                .build();
    }

    /** The caller's own purchases; admins can pass {@code all=true} for the whole team. */
    @GET
    @Path("/purchases")
    public List<ShopPurchaseResponse> getPurchases(@PathParam("teamId") UUID teamId,
            @QueryParam("all") @DefaultValue("false") boolean all) {
        User requester = currentUserService.getOrCreateCurrentUser();
        return shopService.listPurchases(teamId, requester, all).stream().map(ShopPurchaseResponse::from).toList();
    }

    @PUT
    @Path("/purchases/{purchaseId}/fulfilled")
    public ShopPurchaseResponse setFulfilled(@PathParam("teamId") UUID teamId,
            @PathParam("purchaseId") UUID purchaseId, @Valid SetFulfilledRequest request) {
        User requester = currentUserService.getOrCreateCurrentUser();
        return ShopPurchaseResponse.from(shopService.setFulfilled(teamId, purchaseId, requester, request.fulfilled()));
    }
}
