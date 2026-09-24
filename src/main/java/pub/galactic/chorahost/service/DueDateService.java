package pub.galactic.chorahost.service;

import java.time.Instant;
import java.util.List;

import pub.galactic.chorahost.entity.ChoreCard;
import pub.galactic.chorahost.enums.CardStatus;
import pub.galactic.chorahost.repository.ChoreCardRepository;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

/**
 * Returns claimed chore cards to the available pool once their due date has
 * passed without being completed.
 */
@ApplicationScoped
public class DueDateService {

    private static final Logger LOG = Logger.getLogger(DueDateService.class);

    @Inject
    ChoreCardRepository choreCardRepository;

    @Scheduled(every = "{chorahost.due-date.check-interval:1m}", concurrentExecution = ConcurrentExecution.SKIP)
    @Transactional
    public void expireOverdueClaims() {
        List<ChoreCard> expired = choreCardRepository.findExpiredClaims(Instant.now());
        for (ChoreCard card : expired) {
            expire(card);
        }
    }

    private void expire(ChoreCard card) {
        card.status = CardStatus.AVAILABLE;
        card.assignedUserId = null;
        card.claimedAt = null;
        card.dueAt = null;
        LOG.debugf("Card %s missed its due date and was returned to the available pool", card.id);
    }
}
