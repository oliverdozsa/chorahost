package pub.galactic.chorahost.dto;

import java.time.Instant;
import java.util.UUID;

import pub.galactic.chorahost.entity.ShopItem;

/** {@code stock} is null for unlimited items. */
public record ShopItemResponse(
        UUID id,
        UUID teamId,
        String name,
        Integer price,
        Integer stock,
        String imageUrl,
        Instant createdAt) {

    public static ShopItemResponse from(ShopItem item) {
        return new ShopItemResponse(item.id, item.teamId, item.name, item.price, item.stock, item.imageUrl,
                item.createdAt);
    }
}
