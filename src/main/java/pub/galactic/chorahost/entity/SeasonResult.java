package pub.galactic.chorahost.entity;

import java.util.UUID;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

/** A member's final standing in an ended season. */
@Entity
@Table(name = "season_results")
public class SeasonResult extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    @Column(name = "season_id", nullable = false)
    public UUID seasonId;

    @Column(name = "user_id", nullable = false)
    public UUID userId;

    @Column(nullable = false)
    public Integer rank;

    @Column(nullable = false)
    public Integer level;

    @Column(name = "current_xp", nullable = false)
    public Integer currentXp;

    @Column(nullable = false)
    public Integer gold;

    @Column(name = "cards_verified", nullable = false)
    public Integer cardsVerified;
}
