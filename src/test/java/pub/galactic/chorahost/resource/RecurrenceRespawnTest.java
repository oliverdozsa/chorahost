package pub.galactic.chorahost.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.LocalTime;
import java.util.List;

import pub.galactic.chorahost.dto.ScheduleDto;
import pub.galactic.chorahost.entity.ChoreCard;
import pub.galactic.chorahost.entity.ScheduleRule;
import pub.galactic.chorahost.entity.Team;
import pub.galactic.chorahost.enums.CardStatus;
import pub.galactic.chorahost.enums.Rarity;
import pub.galactic.chorahost.enums.RecurrenceType;
import pub.galactic.chorahost.enums.ScheduleFrequency;
import pub.galactic.chorahost.repository.ChoreCardRepository;
import pub.galactic.chorahost.service.RecurrenceService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RecurrenceRespawnTest extends AbstractResourceTest {

    @Inject
    ChoreCardRepository cards;

    @Inject
    RecurrenceService recurrence;

    @Test
    void dueCardIsRespawnedFreshAndOnlyOnce() {
        Instant slot = Instant.now().minus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MILLIS);
        var ids = QuarkusTransaction.requiringNew().call(() -> {
            Team team = new Team();
            team.name = "Recurrence Team";
            team.persist();

            ChoreCard done = new ChoreCard();
            done.teamId = team.id;
            done.title = "Water plants";
            done.rarity = Rarity.COMMON;
            done.complexity = 2;
            done.estimatedMinutes = 10;
            done.xpReward = 5;
            done.goldReward = 3;
            done.status = CardStatus.VERIFIED;
            done.claimedAt = done.completedAt = done.verifiedAt = slot.minusSeconds(60);
            done.recurrenceType = RecurrenceType.SCHEDULE;
            done.recurrenceSchedule = ScheduleRule.from(new ScheduleDto(ScheduleFrequency.DAILY, null, null, null,
                    LocalTime.of(8, 0), "UTC"));
            done.recurrenceNextSpawnAt = slot;
            cards.persist(done);
            return new java.util.UUID[] { team.id, done.id };
        });

        recurrence.respawnDueCards();
        recurrence.respawnDueCards();

        List<ChoreCard> teamCards = cards.list("teamId", ids[0]);
        assertEquals(2, teamCards.size());
        ChoreCard original = teamCards.stream().filter(c -> c.id.equals(ids[1])).findFirst().orElseThrow();
        ChoreCard fresh = teamCards.stream().filter(c -> !c.id.equals(ids[1])).findFirst().orElseThrow();

        assertNull(original.recurrenceNextSpawnAt);
        assertEquals("Water plants", fresh.title);
        assertEquals(CardStatus.AVAILABLE, fresh.status);
        assertNull(fresh.assignedUserId);
        assertNull(fresh.claimedAt);
        assertNull(fresh.completedAt);
        assertNull(fresh.verifiedAt);
        assertNull(fresh.rewardAcknowledgedAt);
        assertNull(fresh.recurrenceNextSpawnAt);
        assertEquals(RecurrenceType.SCHEDULE, fresh.recurrenceType);
        assertEquals(ScheduleFrequency.DAILY, fresh.recurrenceSchedule.frequency);
        assertEquals(LocalTime.of(8, 0), fresh.recurrenceSchedule.time);
        assertEquals(slot, fresh.createdAt);
    }
}
