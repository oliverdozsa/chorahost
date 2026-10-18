package pub.galactic.chorahost.dto;

import java.util.Map;
import java.util.UUID;

import pub.galactic.chorahost.enums.Rarity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateChoreCardTemplateRequest(
        @NotBlank String title,
        String description,
        @NotNull Rarity rarity,
        @Min(1) @Max(10) @NotNull Integer complexity,
        @NotNull Integer estimatedMinutes,
        @NotNull Integer xpReward,
        @NotNull Integer goldReward,
        String itemDrop,
        String imageUrl,
        Map<UUID, String> userImageUrls,
        @Min(1) Integer dueDateIntervalHours,
        @Valid RecurrenceDto recurrence) {
}
