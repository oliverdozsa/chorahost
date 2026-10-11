package pub.galactic.chorahost.entity;

import java.time.Instant;
import java.util.UUID;

import pub.galactic.chorahost.enums.SeasonStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.UuidGenerator;

/**
 * One run of a team's season. Members' level/XP/gold live on their team
 * membership and are archived into {@link SeasonResult} rows when the
 * season ends. For an ended season, {@code endsAt} is when it actually ended.
 */
@Entity
@Table(name = "seasons")
public class Season extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    @NotNull
    @Column(name = "team_id", nullable = false)
    public UUID teamId;

    @Column(nullable = false)
    public Integer number;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public SeasonStatus status = SeasonStatus.ACTIVE;

    @Column(name = "starts_at", nullable = false)
    public Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    public Instant endsAt;
}
