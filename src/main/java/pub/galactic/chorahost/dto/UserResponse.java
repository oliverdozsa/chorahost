package pub.galactic.chorahost.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import pub.galactic.chorahost.entity.User;

public record UserResponse(
        UUID id,
        String name,
        String givenName,
        String familyName,
        String email,
        Instant createdAt,
        List<TeamMembershipResponse> teams) {

    public static UserResponse from(User user, List<TeamMembershipResponse> teams) {
        return new UserResponse(
                user.id,
                user.name,
                user.givenName,
                user.familyName,
                user.email,
                user.createdAt,
                teams);
    }
}
