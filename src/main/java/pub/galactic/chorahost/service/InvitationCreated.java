package pub.galactic.chorahost.service;

import java.time.Instant;

/**
 * Fired when an invitation has been created. {@code code} is the plaintext
 * invitation code, which is only available at creation time.
 */
public record InvitationCreated(String email, String teamName, String inviterName, String code,
        Instant expiresAt) {
}
