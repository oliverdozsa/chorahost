package pub.galactic.chorahost.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

import pub.galactic.chorahost.enums.ScheduleFrequency;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * A calendar rule: every {@code interval} days/weeks/months, at {@code time} in
 * {@code timeZone}. WEEKLY needs {@code daysOfWeek}; MONTHLY needs
 * {@code dayOfMonth} (clamped to the month's last day, so 31 means "month end"
 * in short months). {@code interval} defaults to 1 and {@code timeZone} to UTC.
 */
public record ScheduleDto(
        @NotNull ScheduleFrequency frequency,
        @Min(1) @Max(100) Integer interval,
        Set<DayOfWeek> daysOfWeek,
        @Min(1) @Max(31) Integer dayOfMonth,
        @NotNull LocalTime time,
        String timeZone) {
}
