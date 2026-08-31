package pub.galactic.chorahost.dto;

import java.util.UUID;

import pub.galactic.chorahost.enums.TeamRole;

/**
 * A team-centric view of one of its members, e.g. a leaderboard row or the
 * result of a role assignment.
 */
public record TeamMemberResponse(
        UUID userId,
        String name,
        String email,
        Integer level,
        Integer currentXp,
        Integer gold,
        TeamRole role) {
}
