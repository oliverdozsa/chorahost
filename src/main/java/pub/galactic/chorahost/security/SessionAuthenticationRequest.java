package pub.galactic.chorahost.security;

import io.quarkus.security.identity.request.BaseAuthenticationRequest;

/** Carries a raw session token from {@link SessionAuthMechanism} to {@link SessionIdentityProvider}. */
public class SessionAuthenticationRequest extends BaseAuthenticationRequest {

    private final String token;

    public SessionAuthenticationRequest(String token) {
        this.token = token;
    }

    public String token() {
        return token;
    }
}
