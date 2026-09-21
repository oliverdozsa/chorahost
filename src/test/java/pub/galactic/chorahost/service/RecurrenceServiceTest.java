package pub.galactic.chorahost.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import pub.galactic.chorahost.dto.ScheduleDto;
import pub.galactic.chorahost.entity.ChoreCard;
import pub.galactic.chorahost.entity.ScheduleRule;
import pub.galactic.chorahost.enums.RecurrenceType;
import pub.galactic.chorahost.enums.ScheduleFrequency;
import org.junit.jupiter.api.Test;

class RecurrenceServiceTest {

    private final RecurrenceService service = new RecurrenceService();

    private static ChoreCard scheduled(Instant createdAt, ScheduleDto dto) {
        ChoreCard card = new ChoreCard();
        card.recurrenceType = RecurrenceType.SCHEDULE;
        card.recurrenceSchedule = ScheduleRule.from(dto);
        card.createdAt = createdAt;
        return card;
    }

    private Instant next(ChoreCard card, String now) {
        return service.nextSpawnAt(card, Instant.parse(now));
    }

    @Test
    void nonRecurringCardHasNoNextSpawn() {
        assertNull(service.nextSpawnAt(new ChoreCard(), Instant.now()));
    }

    @Test
    void afterCompletionIsIntervalAfterVerification() {
        Instant now = Instant.parse("2026-01-10T12:00:00Z");
        ChoreCard card = new ChoreCard();
        card.recurrenceType = RecurrenceType.AFTER_COMPLETION;
        card.recurrenceIntervalMinutes = 90;
        assertEquals(now.plus(90, ChronoUnit.MINUTES), service.nextSpawnAt(card, now));
    }

    @Test
    void dailyFiresAtTimeOfDayInZone() {
        // 08:00 Budapest (CET, UTC+1) in winter
        ChoreCard card = scheduled(Instant.parse("2026-01-01T00:00:00Z"),
                new ScheduleDto(ScheduleFrequency.DAILY, null, null, null, LocalTime.of(8, 0), "Europe/Budapest"));
        assertEquals(Instant.parse("2026-01-05T07:00:00Z"), next(card, "2026-01-04T20:00:00Z"));
        // before today's slot -> today
        assertEquals(Instant.parse("2026-01-05T07:00:00Z"), next(card, "2026-01-05T05:00:00Z"));
    }

    @Test
    void everyThreeDaysStaysAlignedToAnchor() {
        ChoreCard card = scheduled(Instant.parse("2026-01-01T10:00:00Z"),
                new ScheduleDto(ScheduleFrequency.DAILY, 3, null, null, LocalTime.of(9, 0), null));
        // anchor day Jan 1 -> Jan 4, 7, 10 ...
        assertEquals(Instant.parse("2026-01-07T09:00:00Z"), next(card, "2026-01-04T12:00:00Z"));
    }

    @Test
    void weeklyPicksNextListedWeekday() {
        ChoreCard card = scheduled(Instant.parse("2026-01-05T10:00:00Z"), // a Monday
                new ScheduleDto(ScheduleFrequency.WEEKLY, null, Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), null,
                        LocalTime.of(18, 30), null));
        assertEquals(Instant.parse("2026-01-08T18:30:00Z"), next(card, "2026-01-05T19:00:00Z")); // -> Thursday
        assertEquals(Instant.parse("2026-01-12T18:30:00Z"), next(card, "2026-01-08T19:00:00Z")); // -> next Monday
    }

    @Test
    void everyOtherWeekSkipsTheOffWeek() {
        ChoreCard card = scheduled(Instant.parse("2026-01-05T10:00:00Z"),
                new ScheduleDto(ScheduleFrequency.WEEKLY, 2, Set.of(DayOfWeek.MONDAY), null, LocalTime.of(9, 0), null));
        assertEquals(Instant.parse("2026-01-19T09:00:00Z"), next(card, "2026-01-05T12:00:00Z"));
    }

    @Test
    void monthlyClampsToShortMonths() {
        ChoreCard card = scheduled(Instant.parse("2026-01-01T00:00:00Z"),
                new ScheduleDto(ScheduleFrequency.MONTHLY, null, null, 31, LocalTime.of(12, 0), null));
        assertEquals(Instant.parse("2026-01-31T12:00:00Z"), next(card, "2026-01-15T00:00:00Z"));
        assertEquals(Instant.parse("2026-02-28T12:00:00Z"), next(card, "2026-01-31T13:00:00Z"));
    }

    @Test
    void missedOccurrencesAreSkipped() {
        ChoreCard card = scheduled(Instant.parse("2026-01-01T08:00:00Z"),
                new ScheduleDto(ScheduleFrequency.DAILY, null, null, null, LocalTime.of(8, 0), null));
        assertEquals(Instant.parse("2026-01-05T08:00:00Z"), next(card, "2026-01-04T20:00:00Z"));
    }
}
