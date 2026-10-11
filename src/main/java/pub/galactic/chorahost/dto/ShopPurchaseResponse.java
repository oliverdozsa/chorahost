package pub.galactic.chorahost.dto;

import java.time.Instant;
import java.util.UUID;

import pub.galactic.chorahost.entity.ShopPurchase;

public record ShopPurchaseResponse(
        UUID id,
        UUID teamId,
        UUID userId,
        UUID itemId,
        String itemName,
        Integer unitPrice,
        Integer quantity,
        Long totalPrice,
        boolean fulfilled,
        Instant fulfilledAt,
        Instant createdAt) {

    public static ShopPurchaseResponse from(ShopPurchase p) {
        return new ShopPurchaseResponse(p.id, p.teamId, p.userId, p.itemId, p.itemName, p.unitPrice, p.quantity,
                p.totalPrice, p.fulfilled, p.fulfilledAt, p.createdAt);
    }
}
