package pub.galactic.chorahost.dto;

import java.time.Instant;
import java.util.UUID;

import pub.galactic.chorahost.enums.TeamRole;

/**
 * A user-centric view of one of their team memberships, e.g. as part of a
 * user profile listing every team they belong to. Level/XP/gold are that
 * user's stats within this specific team.
 */
public record TeamMembershipResponse(
        UUID teamId,
        String teamName,
        TeamRole role,
        Integer level,
        Integer currentXp,
        Integer nextLevelXp,
        Integer gold,
        Instant joinedAt) {
}
