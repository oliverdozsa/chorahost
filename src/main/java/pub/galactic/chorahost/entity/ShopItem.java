package pub.galactic.chorahost.entity;

import java.time.Instant;
import java.util.UUID;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.UuidGenerator;

/**
 * Something a team's members can buy with gold. Shops are per team, so the
 * same item name in two teams is two unrelated rows.
 */
@Entity
@Table(name = "shop_items")
public class ShopItem extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    @NotNull
    @Column(name = "team_id", nullable = false)
    public UUID teamId;

    @NotBlank
    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public Integer price;

    /** Units left to sell; null means unlimited. */
    public Integer stock;

    @Column(name = "image_url")
    public String imageUrl;

    /** False once an admin has deleted the item; kept for purchase history. */
    @Column(nullable = false)
    public boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @jakarta.persistence.PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
