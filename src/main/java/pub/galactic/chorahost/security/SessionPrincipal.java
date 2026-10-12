package pub.galactic.chorahost.security;

import java.security.Principal;
import java.util.UUID;

/** Principal for a request authenticated with a chorahost session token. */
public record SessionPrincipal(UUID userId, String email) implements Principal {

    @Override
    public String getName() {
        return email;
    }
}
