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

    /** Claimed cards past their due date, row-locked so concurrent instances don't double-process. */
    public List<ChoreCard> findExpiredClaims(Instant now) {
        return find("status = ?1 and dueAt <= ?2", CardStatus.CLAIMED, now)
                .withLock(LockModeType.PESSIMISTIC_WRITE).list();
    }

    /** Cards a user holds in a team that are not yet verified (claimed or completed), row-locked for reassignment. */
    public List<ChoreCard> findUnverifiedByUserInTeam(UUID userId, UUID teamId) {
        return find("assignedUserId = ?1 and teamId = ?2 and status in (?3, ?4)",
                userId, teamId, CardStatus.CLAIMED, CardStatus.COMPLETED)
                .withLock(LockModeType.PESSIMISTIC_WRITE).list();
    }

    public List<ChoreCard> findHand(UUID userId) {
        return find("assignedUserId = ?1 and status in (?2, ?3)",
                userId, CardStatus.CLAIMED, CardStatus.COMPLETED).list();
    }
}
