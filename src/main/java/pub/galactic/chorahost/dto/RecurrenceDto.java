package pub.galactic.chorahost.dto;

import pub.galactic.chorahost.entity.ScheduleRule;
import pub.galactic.chorahost.enums.RecurrenceType;
import pub.galactic.chorahost.exception.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Recurrence settings. SCHEDULE requires {@code schedule} (a calendar rule);
 * AFTER_COMPLETION requires {@code intervalMinutes}, the delay after verification.
 */
public record RecurrenceDto(
        @NotNull RecurrenceType type,
        @Min(1) Integer intervalMinutes,
        @Valid ScheduleDto schedule) {

    public static RecurrenceDto of(RecurrenceType type, Integer intervalMinutes, ScheduleRule schedule) {
        return type == null ? null : new RecurrenceDto(type, intervalMinutes, ScheduleRule.toDto(schedule));
    }

    /** Rejects combinations the bean-validation annotations can't express. */
    public void validate() {
        if (type == RecurrenceType.AFTER_COMPLETION && intervalMinutes == null) {
            throw ApiException.badRequest("recurrence.intervalMinutes is required for AFTER_COMPLETION");
        }
        if (type == RecurrenceType.SCHEDULE && schedule == null) {
            throw ApiException.badRequest("recurrence.schedule is required for SCHEDULE");
        }
    }
}
