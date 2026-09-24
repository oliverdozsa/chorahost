package pub.galactic.chorahost.dto;

import java.time.Instant;
import java.util.UUID;

import pub.galactic.chorahost.entity.ChoreCard;
import pub.galactic.chorahost.enums.CardStatus;
import pub.galactic.chorahost.enums.Rarity;

public record ChoreCardResponse(
        UUID id,
        String title,
        String description,
        Rarity rarity,
        Integer complexity,
        Integer estimatedMinutes,
        Integer xpReward,
        Integer goldReward,
        String itemDrop,
        String imageUrl,
        CardStatus status,
        UUID assignedUserId,
        UUID teamId,
        UUID templateId,
        Instant createdAt,
        Instant claimedAt,
        Instant completedAt,
        Instant verifiedAt,
        Instant rewardAcknowledgedAt,
        Integer dueDateIntervalHours,
        Instant dueAt,
        RecurrenceDto recurrence) {

    public static ChoreCardResponse from(ChoreCard card) {
        return new ChoreCardResponse(
                card.id,
                card.title,
                card.description,
                card.rarity,
                card.complexity,
                card.estimatedMinutes,
                card.xpReward,
                card.goldReward,
                card.itemDrop,
                card.imageUrl,
                card.status,
                card.assignedUserId,
                card.teamId,
                card.templateId,
                card.createdAt,
                card.claimedAt,
                card.completedAt,
                card.verifiedAt,
                card.rewardAcknowledgedAt,
                card.dueDateIntervalHours,
                card.dueAt,
                RecurrenceDto.of(card.recurrenceType, card.recurrenceIntervalMinutes, card.recurrenceSchedule));
    }
}
