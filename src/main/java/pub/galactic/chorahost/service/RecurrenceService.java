package pub.galactic.chorahost.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import pub.galactic.chorahost.entity.ChoreCard;
import pub.galactic.chorahost.entity.ScheduleRule;
import pub.galactic.chorahost.enums.CardStatus;
import pub.galactic.chorahost.enums.RecurrenceType;
import pub.galactic.chorahost.repository.ChoreCardRepository;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

/**
 * Respawns recurring chore cards. Recurrence is initiated when a card is
 * verified ({@link #nextSpawnAt}); a background job then creates a fresh card
 * once that time has arrived.
 */
@ApplicationScoped
public class RecurrenceService {

    private static final Logger LOG = Logger.getLogger(RecurrenceService.class);

    @Inject
    ChoreCardRepository choreCardRepository;

    /**
     * When a just-verified card should respawn, or null if it does not recur.
     * SCHEDULE: the next calendar occurrence (anchored to the card's creation)
     * strictly after {@code now}; missed occurrences are skipped rather than caught up.
     * AFTER_COMPLETION: {@code now} plus the interval.
     */
    public Instant nextSpawnAt(ChoreCard card, Instant now) {
        if (card.recurrenceType == RecurrenceType.SCHEDULE && card.recurrenceSchedule != null) {
            return card.recurrenceSchedule.nextAfter(card.createdAt, now);
        }
        if (card.recurrenceType == RecurrenceType.AFTER_COMPLETION && card.recurrenceIntervalMinutes != null) {
            return now.plus(Duration.ofMinutes(card.recurrenceIntervalMinutes));
        }
        return null;
    }

    @Scheduled(every = "{chorahost.recurrence.check-interval:30s}", concurrentExecution = ConcurrentExecution.SKIP)
    @Transactional
    public void respawnDueCards() {
        List<ChoreCard> due = choreCardRepository.findDueForRespawn(Instant.now());
        for (ChoreCard source : due) {
            respawn(source);
        }
    }

    private void respawn(ChoreCard source) {
        ChoreCard fresh = new ChoreCard();
        fresh.teamId = source.teamId;
        fresh.templateId = source.templateId;
        fresh.title = source.title;
        fresh.description = source.description;
        fresh.rarity = source.rarity;
        fresh.complexity = source.complexity;
        fresh.estimatedMinutes = source.estimatedMinutes;
        fresh.xpReward = source.xpReward;
        fresh.goldReward = source.goldReward;
        fresh.itemDrop = source.itemDrop;
        fresh.imageUrl = source.imageUrl;
        fresh.dueDateIntervalHours = source.dueDateIntervalHours;
        fresh.recurrenceType = source.recurrenceType;
        fresh.recurrenceIntervalMinutes = source.recurrenceIntervalMinutes;
        fresh.recurrenceSchedule = ScheduleRule.copyOf(source.recurrenceSchedule);
        fresh.status = CardStatus.AVAILABLE;
        // Schedule cards are stamped with their slot time so the cadence doesn't drift with job latency.
        if (source.recurrenceType == RecurrenceType.SCHEDULE) {
            fresh.createdAt = source.recurrenceNextSpawnAt;
        }

        choreCardRepository.persist(fresh);
        source.recurrenceNextSpawnAt = null;
        LOG.debugf("Respawned card %s as %s", source.id, fresh.id);
    }
}
