package pub.galactic.chorahost.repository;

import java.util.List;
import java.util.UUID;

import pub.galactic.chorahost.entity.ChoreCard;
import pub.galactic.chorahost.enums.CardStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ChoreCardRepository implements PanacheRepositoryBase<ChoreCard, UUID> {

    public List<ChoreCard> findForTeamByStatuses(UUID teamId, List<CardStatus> statuses) {
        return find("teamId = ?1 and status in ?2", teamId, statuses).list();
    }

    public List<ChoreCard> findHand(UUID userId) {
        return find("assignedUserId = ?1 and status in (?2, ?3)",
                userId, CardStatus.CLAIMED, CardStatus.COMPLETED).list();
    }
}
