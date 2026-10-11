package pub.galactic.chorahost.dto;

import jakarta.validation.constraints.Min;

/** Body for buying an item; quantity defaults to 1 when omitted. */
public record PurchaseRequest(@Min(1) Integer quantity) {

    public int quantityOrOne() {
        return quantity == null ? 1 : quantity;
    }
}
