package pub.galactic.chorahost.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Create/replace body for a shop item. A null {@code stock} means unlimited. */
public record ShopItemRequest(
        @NotBlank String name,
        @NotNull @Min(0) Integer price,
        @Min(0) Integer stock,
        @Pattern(regexp = "^https?://\\S+$", message = "must be an http(s) URL") String imageUrl) {
}
