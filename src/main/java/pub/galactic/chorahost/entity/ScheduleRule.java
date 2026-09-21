package pub.galactic.chorahost.entity;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import pub.galactic.chorahost.dto.ScheduleDto;
import pub.galactic.chorahost.enums.ScheduleFrequency;
import pub.galactic.chorahost.exception.ApiException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * A calendar rule for SCHEDULE recurrence, embedded in cards and templates.
 * "Every N" periods are counted from an anchor instant (the first card's
 * creation), so e.g. every 2 weeks stays on the same alternating weeks.
 */
@Embeddable
public class ScheduleRule {

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_frequency")
    public ScheduleFrequency frequency;

    @Column(name = "schedule_interval")
    public Integer interval;

    /** Comma-separated {@link DayOfWeek} names; WEEKLY only. */
    @Column(name = "schedule_days_of_week")
    public String daysOfWeek;

    @Column(name = "schedule_day_of_month")
    public Integer dayOfMonth;

    @Column(name = "schedule_time")
    public LocalTime time;

    @Column(name = "schedule_time_zone")
    public String timeZone;

    public static ScheduleRule from(ScheduleDto dto) {
        if (dto == null) {
            return null;
        }
        ScheduleRule rule = new ScheduleRule();
        rule.frequency = dto.frequency();
        rule.interval = dto.interval() == null ? 1 : dto.interval();
        rule.time = dto.time();
        rule.timeZone = dto.timeZone() == null || dto.timeZone().isBlank() ? "UTC" : dto.timeZone();
        try {
            ZoneId.of(rule.timeZone);
        } catch (DateTimeException e) {
            throw ApiException.badRequest("Unknown time zone: " + rule.timeZone);
        }

        switch (rule.frequency) {
            case WEEKLY -> {
                if (dto.daysOfWeek() == null || dto.daysOfWeek().isEmpty()) {
                    throw ApiException.badRequest("schedule.daysOfWeek is required for WEEKLY");
                }
                rule.daysOfWeek = dto.daysOfWeek().stream().sorted().map(Enum::name)
                        .collect(Collectors.joining(","));
            }
            case MONTHLY -> {
                if (dto.dayOfMonth() == null) {
                    throw ApiException.badRequest("schedule.dayOfMonth is required for MONTHLY");
                }
                rule.dayOfMonth = dto.dayOfMonth();
            }
            case DAILY -> {
            }
        }
        return rule;
    }

    /** A separate instance, so a respawned card never shares its embedded state with the source. */
    public static ScheduleRule copyOf(ScheduleRule other) {
        if (other == null) {
            return null;
        }
        ScheduleRule rule = new ScheduleRule();
        rule.frequency = other.frequency;
        rule.interval = other.interval;
        rule.daysOfWeek = other.daysOfWeek;
        rule.dayOfMonth = other.dayOfMonth;
        rule.time = other.time;
        rule.timeZone = other.timeZone;
        return rule;
    }

    public static ScheduleDto toDto(ScheduleRule rule) {
        if (rule == null || rule.frequency == null) {
            return null;
        }
        return new ScheduleDto(rule.frequency, rule.interval, rule.days(), rule.dayOfMonth, rule.time, rule.timeZone);
    }

    private Set<DayOfWeek> days() {
        if (daysOfWeek == null || daysOfWeek.isBlank()) {
            return null;
        }
        return Arrays.stream(daysOfWeek.split(",")).map(DayOfWeek::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
    }

    /**
     * The first occurrence strictly after {@code after}, counting "every N"
     * periods from {@code anchor}. Local times that don't exist (DST gap) shift
     * forward, as {@link java.time.ZonedDateTime} does.
     */
    public Instant nextAfter(Instant anchor, Instant after) {
        ZoneId zone = ZoneId.of(timeZone);
        LocalDate anchorDate = anchor.atZone(zone).toLocalDate();
        LocalDate date = after.atZone(zone).toLocalDate();
        Set<DayOfWeek> weekdays = frequency == ScheduleFrequency.WEEKLY ? days() : null;

        // A match always exists within one full "every N" cycle, plus a day for today's already-passed slot.
        long maxDays = 400L * interval + 1;
        for (long i = 0; i <= maxDays; i++, date = date.plusDays(1)) {
            if (!matches(date, anchorDate, weekdays)) {
                continue;
            }
            Instant candidate = date.atTime(time).atZone(zone).toInstant();
            if (candidate.isAfter(after)) {
                return candidate;
            }
        }
        throw new IllegalStateException("No occurrence found for schedule " + frequency + "/" + interval);
    }

    private boolean matches(LocalDate date, LocalDate anchorDate, Set<DayOfWeek> weekdays) {
        return switch (frequency) {
            case DAILY -> Math.floorMod(ChronoUnit.DAYS.between(anchorDate, date), interval) == 0;
            case WEEKLY -> {
                LocalDate anchorWeek = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate week = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                yield Math.floorMod(ChronoUnit.WEEKS.between(anchorWeek, week), interval) == 0
                        && weekdays.contains(date.getDayOfWeek());
            }
            case MONTHLY -> Math.floorMod(
                    ChronoUnit.MONTHS.between(YearMonth.from(anchorDate), YearMonth.from(date)), interval) == 0
                    && date.getDayOfMonth() == Math.min(dayOfMonth, date.lengthOfMonth());
        };
    }
}
