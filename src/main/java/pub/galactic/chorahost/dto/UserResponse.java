package pub.galactic.chorahost.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import pub.galactic.chorahost.entity.User;

public record UserResponse(
        UUID id,
        String name,
        String email,
        Integer level,
        Integer currentXp,
        Integer nextLevelXp,
        Integer gold,
        Instant createdAt,
        List<TeamMembershipResponse> teams) {

    public static UserResponse from(User user, List<TeamMembershipResponse> teams) {
        return new UserResponse(
                user.id,
                user.name,
                user.email,
                user.level,
                user.currentXp,
                user.level * 1000,
                user.gold,
                user.createdAt,
                teams);
    }
}
