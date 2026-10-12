package pub.galactic.chorahost.dto;

import java.time.Instant;

/** A freshly created session: the bearer token plus the profile details only the Google token carries. */
public record AuthSessionResponse(
        String token,
        Instant expiresAt,
        String name,
        String email,
        String picture) {
}
