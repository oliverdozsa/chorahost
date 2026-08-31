package pub.galactic.chorahost.entity;

import java.time.Instant;
import java.util.UUID;

import pub.galactic.chorahost.enums.Rarity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.UuidGenerator;

/**
 * A reusable blueprint for a repeating chore. Team admins instantiate
 * {@link ChoreCard}s from a template instead of re-entering the same chore.
 */
@Entity
@Table(name = "chore_card_templates")
public class ChoreCardTemplate extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    @NotNull
    @Column(name = "team_id", nullable = false)
    public UUID teamId;

    @NotBlank
    @Column(nullable = false)
    public String title;

    @Column(columnDefinition = "text")
    public String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Rarity rarity;

    @Min(1)
    @Max(10)
    @Column(nullable = false)
    public Integer complexity;

    @Column(name = "estimated_minutes", nullable = false)
    public Integer estimatedMinutes;

    @Column(name = "xp_reward", nullable = false)
    public Integer xpReward;

    @Column(name = "gold_reward", nullable = false)
    public Integer goldReward;

    @Column(name = "item_drop")
    public String itemDrop;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @jakarta.persistence.PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
