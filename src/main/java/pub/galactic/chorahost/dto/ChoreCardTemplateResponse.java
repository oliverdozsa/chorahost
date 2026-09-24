package pub.galactic.chorahost.dto;

import java.time.Instant;
import java.util.UUID;

import pub.galactic.chorahost.entity.ChoreCardTemplate;
import pub.galactic.chorahost.enums.Rarity;

public record ChoreCardTemplateResponse(
        UUID id,
        UUID teamId,
        String title,
        String description,
        Rarity rarity,
        Integer complexity,
        Integer estimatedMinutes,
        Integer xpReward,
        Integer goldReward,
        String itemDrop,
        String imageUrl,
        Instant createdAt,
        Integer dueDateIntervalHours,
        RecurrenceDto recurrence) {

    public static ChoreCardTemplateResponse from(ChoreCardTemplate template) {
        return new ChoreCardTemplateResponse(
                template.id,
                template.teamId,
                template.title,
                template.description,
                template.rarity,
                template.complexity,
                template.estimatedMinutes,
                template.xpReward,
                template.goldReward,
                template.itemDrop,
                template.imageUrl,
                template.createdAt,
                template.dueDateIntervalHours,
                RecurrenceDto.of(template.recurrenceType, template.recurrenceIntervalMinutes, template.recurrenceSchedule));
    }
}
