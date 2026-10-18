package pub.galactic.chorahost.entity;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import pub.galactic.chorahost.enums.CardStatus;
import pub.galactic.chorahost.enums.Rarity;
import pub.galactic.chorahost.enums.RecurrenceType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "chore_cards")
public class ChoreCard extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

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

    @Column(name = "image_url")
    public String imageUrl;

    /**
     * Per-user image overrides, keyed by user id. {@link #imageUrl} is the
     * fallback for users without an entry.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "chore_card_user_images", joinColumns = @JoinColumn(name = "card_id"))
    @MapKeyColumn(name = "user_id")
    @Column(name = "image_url", nullable = false)
    public Map<UUID, String> userImageUrls = new HashMap<>();

    /**
     * The image to show: the assignee's override if there is one, else the
     * viewer's override, else the fallback {@link #imageUrl}.
     */
    public String resolvedImageUrl(UUID viewerId) {
        String custom = assignedUserId == null ? null : userImageUrls.get(assignedUserId);
        if (custom == null && viewerId != null) {
            custom = userImageUrls.get(viewerId);
        }
        return custom != null ? custom : imageUrl;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type")
    public RecurrenceType recurrenceType;

    @Column(name = "recurrence_interval_minutes")
    public Integer recurrenceIntervalMinutes;

    @Embedded
    public ScheduleRule recurrenceSchedule;

    /**
     * When the background job should respawn this card. Set on verification for
     * recurring cards and cleared once the respawn has happened.
     */
    @Column(name = "recurrence_next_spawn_at")
    public Instant recurrenceNextSpawnAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public CardStatus status = CardStatus.AVAILABLE;

    @Column(name = "assigned_user_id")
    public UUID assignedUserId;

    /**
     * Hours the assignee has to complete the card after claiming it. Null
     * means the card has no due date.
     */
    @Column(name = "due_date_interval_hours")
    public Integer dueDateIntervalHours;

    /**
     * When the card must be completed by. Set from {@link #dueDateIntervalHours}
     * at claim time and cleared when the card is unclaimed (completed, or
     * expired back to AVAILABLE).
     */
    @Column(name = "due_at")
    public Instant dueAt;

    @NotNull
    @Column(name = "team_id", nullable = false)
    public UUID teamId;

    /**
     * The template this card was instantiated from, if any.
     */
    @Column(name = "template_id")
    public UUID templateId;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "claimed_at")
    public Instant claimedAt;

    @Column(name = "completed_at")
    public Instant completedAt;

    @Column(name = "verified_at")
    public Instant verifiedAt;

    @Column(name = "reward_acknowledged_at")
    public Instant rewardAcknowledgedAt;

    @jakarta.persistence.PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
