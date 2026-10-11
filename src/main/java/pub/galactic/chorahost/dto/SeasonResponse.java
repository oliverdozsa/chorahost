package pub.galactic.chorahost.dto;

import java.time.Instant;
import java.util.UUID;

import pub.galactic.chorahost.entity.Season;
import pub.galactic.chorahost.enums.SeasonStatus;

public record SeasonResponse(UUID id, UUID teamId, int number, SeasonStatus status, Instant startsAt,
        Instant endsAt) {

    public static SeasonResponse from(Season season) {
        return new SeasonResponse(season.id, season.teamId, season.number, season.status, season.startsAt,
                season.endsAt);
    }
}
