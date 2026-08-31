package pub.galactic.chorahost.dto;

import java.time.Instant;
import java.util.UUID;

import pub.galactic.chorahost.enums.TeamRole;

/**
 * A user-centric view of one of their team memberships, e.g. as part of a
 * user profile listing every team they belong to.
 */
public record TeamMembershipResponse(UUID teamId, String teamName, TeamRole role, Instant joinedAt) {
}
