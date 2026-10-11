package pub.galactic.chorahost.entity;

import java.time.Instant;
import java.util.UUID;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.UuidGenerator;

/**
 * A member's purchase of a shop item. The item name and price are copied in
 * so later edits to the item don't rewrite history. Admins mark purchases
 * {@code fulfilled} once the player has actually received them.
 */
@Entity
@Table(name = "shop_purchases")
public class ShopPurchase extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    @NotNull
    @Column(name = "team_id", nullable = false)
    public UUID teamId;

    @NotNull
    @Column(name = "user_id", nullable = false)
    public UUID userId;

    @NotNull
    @Column(name = "item_id", nullable = false)
    public UUID itemId;

    @Column(name = "item_name", nullable = false)
    public String itemName;

    @Column(name = "unit_price", nullable = false)
    public Integer unitPrice;

    @Column(nullable = false)
    public Integer quantity;

    @Column(name = "total_price", nullable = false)
    public Long totalPrice;

    @Column(nullable = false)
    public boolean fulfilled = false;

    @Column(name = "fulfilled_at")
    public Instant fulfilledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @jakarta.persistence.PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
