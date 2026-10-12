package pub.galactic.chorahost.security;

import pub.galactic.chorahost.service.AuthSessionService;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Authenticates {@code Authorization: Bearer chs_...} session tokens. Any
 * other bearer token (a Google ID token) is left to the OIDC mechanism: this
 * one runs first, and returns nothing unless the token carries the session
 * prefix.
 */
@ApplicationScoped
public class SessionAuthMechanism implements HttpAuthenticationMechanism {

    private static final String BEARER = "Bearer ";

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
        String header = context.request().getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER)) {
            return Uni.createFrom().nullItem();
        }
        String token = header.substring(BEARER.length()).trim();
        if (!token.startsWith(AuthSessionService.TOKEN_PREFIX)) {
            return Uni.createFrom().nullItem();
        }
        return identityProviderManager.authenticate(new SessionAuthenticationRequest(token));
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        return Uni.createFrom().item(new ChallengeData(401, "WWW-Authenticate", "Bearer"));
    }

    @Override
    public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context) {
        return Uni.createFrom().item(new HttpCredentialTransport(HttpCredentialTransport.Type.AUTHORIZATION, "bearer"));
    }

    @Override
    public int getPriority() {
        return 2000;
    }
}
