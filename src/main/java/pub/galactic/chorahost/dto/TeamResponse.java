package pub.galactic.chorahost.dto;

import java.time.Instant;
import java.util.UUID;

import pub.galactic.chorahost.entity.Team;

public record TeamResponse(UUID id, String name, int seasonLengthDays, Instant createdAt) {

    public static TeamResponse from(Team team) {
        return new TeamResponse(team.id, team.name, team.seasonLengthDays, team.createdAt);
    }
}
