package pub.galactic.chorahost.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import pub.galactic.chorahost.dto.CreateChoreCardRequest;
import pub.galactic.chorahost.dto.UpdateChoreCardRequest;
import pub.galactic.chorahost.entity.ChoreCard;
import pub.galactic.chorahost.entity.ScheduleRule;
import pub.galactic.chorahost.entity.TeamMembership;
import pub.galactic.chorahost.entity.User;
import pub.galactic.chorahost.enums.CardStatus;
import pub.galactic.chorahost.enums.TeamRole;
import pub.galactic.chorahost.exception.ApiException;
import pub.galactic.chorahost.repository.ChoreCardRepository;
import pub.galactic.chorahost.service.GamificationService.LevelUpResult;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class ChoreCardService {

    @Inject
    ChoreCardRepository choreCardRepository;

    @Inject
    TeamService teamService;

    @Inject
    RecurrenceService recurrenceService;

    @Inject
    GamificationService gamificationService;

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    public PagedResult<ChoreCard> getCardsForTeam(UUID teamId, List<CardStatus> statuses, User requester,
            int page, int size) {
        teamService.requireMembership(teamId, requester.id);
        if (page < 0) {
            throw ApiException.badRequest("page must be >= 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw ApiException.badRequest("size must be between 1 and " + MAX_PAGE_SIZE);
        }

        List<CardStatus> effectiveStatuses = (statuses == null || statuses.isEmpty())
                ? List.of(CardStatus.AVAILABLE)
                : statuses;
        PanacheQuery<ChoreCard> query = choreCardRepository.findForTeamByStatuses(teamId, effectiveStatuses, page, size);
        return new PagedResult<>(query.list(), page, size, query.count());
    }

    public List<ChoreCard> getHand(UUID userId) {
        return choreCardRepository.findHand(userId);
    }

    @Transactional
    public ChoreCard createCard(CreateChoreCardRequest request, User requester) {
        teamService.requireAdminOrTeamAdmin(request.teamId(), requester);

        ChoreCard card = new ChoreCard();
        card.teamId = request.teamId();
        card.title = request.title();
        card.description = request.description();
        card.rarity = request.rarity();
        card.complexity = request.complexity();
        card.estimatedMinutes = request.estimatedMinutes();
        card.xpReward = request.xpReward();
        card.goldReward = request.goldReward();
        card.itemDrop = request.itemDrop();
        card.imageUrl = request.imageUrl();
        card.userImageUrls.clear();
        card.userImageUrls.putAll(teamService.validateUserImageUrls(card.teamId, request.userImageUrls()));
        card.dueDateIntervalHours = request.dueDateIntervalHours();
        if (request.recurrence() != null) {
            request.recurrence().validate();
            card.recurrenceType = request.recurrence().type();
            card.recurrenceIntervalMinutes = request.recurrence().intervalMinutes();
            card.recurrenceSchedule = ScheduleRule.from(request.recurrence().schedule());
        }
        card.status = CardStatus.AVAILABLE;

        choreCardRepository.persist(card);
        return card;
    }

    @Transactional
    public ChoreCard updateCard(UUID cardId, UpdateChoreCardRequest request, User requester) {
        ChoreCard card = requireCard(cardId);
        teamService.requireAdminOrTeamAdmin(card.teamId, requester);

        card.title = request.title();
        card.description = request.description();
        card.rarity = request.rarity();
        card.complexity = request.complexity();
        card.estimatedMinutes = request.estimatedMinutes();
        card.xpReward = request.xpReward();
        card.goldReward = request.goldReward();
        card.itemDrop = request.itemDrop();
        card.imageUrl = request.imageUrl();
        card.userImageUrls.clear();
        card.userImageUrls.putAll(teamService.validateUserImageUrls(card.teamId, request.userImageUrls()));
        card.dueDateIntervalHours = request.dueDateIntervalHours();
        if (request.recurrence() != null) {
            request.recurrence().validate();
            card.recurrenceType = request.recurrence().type();
            card.recurrenceIntervalMinutes = request.recurrence().intervalMinutes();
            card.recurrenceSchedule = ScheduleRule.from(request.recurrence().schedule());
        } else {
            card.recurrenceType = null;
            card.recurrenceIntervalMinutes = null;
            card.recurrenceSchedule = null;
        }

        return card;
    }

    @Transactional
    public ChoreCard claimCard(UUID cardId, User requester) {
        ChoreCard card = requireCard(cardId);
        teamService.requireMembership(card.teamId, requester.id);
        if (card.status != CardStatus.AVAILABLE) {
            throw ApiException.conflict("Card is not available to claim: " + card.status);
        }

        card.status = CardStatus.CLAIMED;
        card.assignedUserId = requester.id;
        card.claimedAt = Instant.now();
        card.dueAt = card.dueDateIntervalHours != null
                ? card.claimedAt.plus(Duration.ofHours(card.dueDateIntervalHours))
                : null;
        return card;
    }

    @Transactional
    public ChoreCard unclaimCard(UUID cardId, User requester) {
        ChoreCard card = requireCard(cardId);
        if (card.status != CardStatus.CLAIMED) {
            throw ApiException.conflict("Card must be CLAIMED to be given back: " + card.status);
        }
        if (!requester.id.equals(card.assignedUserId)) {
            throw new ApiException(Response.Status.FORBIDDEN, "Only the assignee can give this card back");
        }

        card.status = CardStatus.AVAILABLE;
        card.assignedUserId = null;
        card.claimedAt = null;
        card.dueAt = null;
        return card;
    }

    @Transactional
    public ChoreCard completeCard(UUID cardId, User requester) {
        ChoreCard card = requireCard(cardId);
        if (card.status != CardStatus.CLAIMED) {
            throw ApiException.conflict("Card must be CLAIMED to be completed: " + card.status);
        }
        if (!requester.id.equals(card.assignedUserId)) {
            throw new ApiException(Response.Status.FORBIDDEN, "Only the claiming user can complete this card");
        }

        card.status = CardStatus.COMPLETED;
        card.completedAt = Instant.now();
        card.dueAt = null;
        return card;
    }

    @Transactional
    public VerificationResult verifyCard(UUID cardId, User requester) {
        ChoreCard card = requireCard(cardId);
        if (card.status != CardStatus.COMPLETED) {
            throw ApiException.conflict("Card must be COMPLETED to be verified: " + card.status);
        }

        // ADMIN/TEAM_ADMIN can verify any card on their team, including their own.
        TeamRole role = teamService.findMembership(card.teamId, requester.id).map(m -> m.role).orElse(null);
        if (role != TeamRole.ADMIN && role != TeamRole.TEAM_ADMIN) {
            throw ApiException.forbidden("ADMIN or TEAM_ADMIN role required to verify this card");
        }

        card.status = CardStatus.VERIFIED;
        card.verifiedAt = Instant.now();
        card.recurrenceNextSpawnAt = recurrenceService.nextSpawnAt(card, card.verifiedAt);

        TeamMembership assigneeMembership = teamService.findMembership(card.teamId, card.assignedUserId)
                .orElseThrow(() -> ApiException.notFound("Assigned user is not a member of this team"));

        LevelUpResult levelUpResult = gamificationService.awardRewards(assigneeMembership, card.xpReward, card.goldReward);

        return new VerificationResult(card, card.xpReward, card.goldReward, levelUpResult.leveledUp(), levelUpResult.newLevel());
    }

    @Transactional
    public ChoreCard acknowledgeReward(UUID cardId, User requester) {
        ChoreCard card = requireCard(cardId);
        if (card.status != CardStatus.VERIFIED) {
            throw ApiException.conflict("Card must be VERIFIED to acknowledge its reward: " + card.status);
        }
        if (!requester.id.equals(card.assignedUserId)) {
            throw new ApiException(Response.Status.FORBIDDEN, "Only the assignee can acknowledge this card's reward");
        }

        if (card.rewardAcknowledgedAt == null) {
            card.rewardAcknowledgedAt = Instant.now();
        }
        return card;
    }

    @Transactional
    public void deleteCard(UUID cardId, User requester) {
        ChoreCard card = requireCard(cardId);
        teamService.requireAdminOrTeamAdmin(card.teamId, requester);

        choreCardRepository.delete(card);
    }

    private ChoreCard requireCard(UUID cardId) {
        return choreCardRepository.findByIdOptional(cardId)
                .orElseThrow(() -> ApiException.notFound("Card not found: " + cardId));
    }

    public record VerificationResult(ChoreCard card, int xpGained, int goldGained, boolean leveledUp, int newLevel) {
    }
}
