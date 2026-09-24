package pub.galactic.chorahost.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import pub.galactic.chorahost.entity.ChoreCard;
import pub.galactic.chorahost.entity.Team;
import pub.galactic.chorahost.entity.User;
import pub.galactic.chorahost.enums.CardStatus;
import pub.galactic.chorahost.enums.Rarity;
import pub.galactic.chorahost.repository.ChoreCardRepository;
import pub.galactic.chorahost.service.DueDateService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DueDateExpiryTest extends AbstractResourceTest {

    @Inject
    ChoreCardRepository cards;

    @Inject
    DueDateService dueDateService;

    @Test
    void overdueClaimedCardIsReturnedToThePool() {
        UUID[] ids = QuarkusTransaction.requiringNew().call(() -> {
            Team team = new Team();
            team.name = "Due Date Team";
            team.persist();

            User assigneeUser = new User();
            assigneeUser.email = "due-date-expired@chorahost.test";
            assigneeUser.name = "Overdue Assignee";
            assigneeUser.persist();

            ChoreCard card = new ChoreCard();
            card.teamId = team.id;
            card.title = "Take out the trash";
            card.rarity = Rarity.COMMON;
            card.complexity = 1;
            card.estimatedMinutes = 5;
            card.xpReward = 5;
            card.goldReward = 1;
            card.dueDateIntervalHours = 3;
            card.status = CardStatus.CLAIMED;
            card.assignedUserId = assigneeUser.id;
            card.claimedAt = Instant.now().minus(4, ChronoUnit.HOURS);
            card.dueAt = card.claimedAt.plus(3, ChronoUnit.HOURS);
            cards.persist(card);
            return new UUID[] { card.id, assigneeUser.id };
        });
        UUID cardId = ids[0];

        dueDateService.expireOverdueClaims();

        ChoreCard reverted = cards.findById(cardId);
        assertEquals(CardStatus.AVAILABLE, reverted.status);
        assertNull(reverted.assignedUserId);
        assertNull(reverted.claimedAt);
        assertNull(reverted.dueAt);
    }

    @Test
    void claimedCardStillWithinDueDateIsUntouched() {
        UUID[] ids = QuarkusTransaction.requiringNew().call(() -> {
            Team team = new Team();
            team.name = "Due Date Team Not Expired";
            team.persist();

            User assigneeUser = new User();
            assigneeUser.email = "due-date-not-expired@chorahost.test";
            assigneeUser.name = "Current Assignee";
            assigneeUser.persist();

            ChoreCard card = new ChoreCard();
            card.teamId = team.id;
            card.title = "Mow the lawn";
            card.rarity = Rarity.COMMON;
            card.complexity = 1;
            card.estimatedMinutes = 5;
            card.xpReward = 5;
            card.goldReward = 1;
            card.dueDateIntervalHours = 3;
            card.status = CardStatus.CLAIMED;
            card.assignedUserId = assigneeUser.id;
            card.claimedAt = Instant.now();
            card.dueAt = card.claimedAt.plus(3, ChronoUnit.HOURS);
            cards.persist(card);
            return new UUID[] { card.id, assigneeUser.id };
        });
        UUID cardId = ids[0];
        UUID assignee = ids[1];

        dueDateService.expireOverdueClaims();

        ChoreCard untouched = cards.findById(cardId);
        assertEquals(CardStatus.CLAIMED, untouched.status);
        assertEquals(assignee, untouched.assignedUserId);
    }
}
