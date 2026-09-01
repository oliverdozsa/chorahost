package pub.galactic.chorahost.entity;

import java.time.Instant;
import java.util.UUID;

import pub.galactic.chorahost.enums.TeamRole;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.UuidGenerator;

/**
 * A user's membership in a team, carrying that user's role within that
 * specific team. A user may hold one membership (and role) per team, and may
 * belong to many teams at once.
 */
@Entity
@Table(name = "team_memberships", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "team_id" }))
public class TeamMembership extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    public UUID userId;

    @NotNull
    @Column(name = "team_id", nullable = false)
    public UUID teamId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public TeamRole role;

    @Column(nullable = false)
    public Integer level = 1;

    @Column(name = "current_xp", nullable = false)
    public Integer currentXp = 0;

    @Column(nullable = false)
    public Integer gold = 0;

    @Column(name = "joined_at", nullable = false, updatable = false)
    public Instant joinedAt;

    @jakarta.persistence.PrePersist
    void prePersist() {
        if (joinedAt == null) {
            joinedAt = Instant.now();
        }
    }
}
