package pub.galactic.chorahost.dev;

import java.security.Principal;

/**
 * Principal produced by {@link DevHeaderAuthMechanism} for a dev-mode
 * impersonated user, identified only by email (there is no real ID token to
 * pull a display name from).
 */
public record DevUserPrincipal(String email) implements Principal {

    @Override
    public String getName() {
        return email;
    }
}
