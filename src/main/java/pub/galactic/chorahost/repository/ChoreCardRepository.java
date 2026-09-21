package pub.galactic.chorahost.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import pub.galactic.chorahost.entity.ChoreCard;
import pub.galactic.chorahost.enums.CardStatus;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

@ApplicationScoped
public class ChoreCardRepository implements PanacheRepositoryBase<ChoreCard, UUID> {

    /**
     * Cards for a team, most recently "active" first. Sorts by whichever "at"
     * timestamp is most relevant to a terminal-ish status (verified, then
     * completed, then claimed), falling back to createdAt for cards still
     * AVAILABLE - a single ordering that stays sensible even when statuses is
     * a mix (e.g. CLAIMED and VERIFIED together).
     */
    public PanacheQuery<ChoreCard> findForTeamByStatuses(UUID teamId, List<CardStatus> statuses, int page, int size) {
        return find("teamId = ?1 and status in ?2 "
                + "order by coalesce(verifiedAt, completedAt, claimedAt, createdAt) desc", teamId, statuses)
                .page(Page.of(page, size));
    }

    /** Recurring cards due for respawn, row-locked so concurrent instances don't double-spawn. */
    public List<ChoreCard> findDueForRespawn(Instant now) {
        return find("recurrenceNextSpawnAt <= ?1 order by recurrenceNextSpawnAt", now)
                .withLock(LockModeType.PESSIMISTIC_WRITE).list();
    }

    public List<ChoreCard> findHand(UUID userId) {
        return find("assignedUserId = ?1 and status in (?2, ?3)",
                userId, CardStatus.CLAIMED, CardStatus.COMPLETED).list();
    }
}
