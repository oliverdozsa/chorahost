package pub.galactic.chorahost.dev;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Dev-mode-only auth bypass: a request carrying an {@code X-Dev-User} header
 * (an email address) is authenticated as that user, no Google ID token
 * needed. Lets you impersonate any seeded user locally (e.g. via curl or
 * Swagger UI) to exercise cross-user scenarios such as an admin verifying a
 * different user's card. Only registered under the dev profile, so it never
 * runs in prod or test.
 */
@ApplicationScoped
@IfBuildProfile("dev")
public class DevHeaderAuthMechanism implements HttpAuthenticationMechanism {

    public static final String HEADER = "X-Dev-User";

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
        String email = context.request().getHeader(HEADER);
        if (email == null || email.isBlank()) {
            return Uni.createFrom().nullItem();
        }

        SecurityIdentity identity = QuarkusSecurityIdentity.builder()
                .setPrincipal(new DevUserPrincipal(email))
                .build();
        return Uni.createFrom().item(identity);
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        return Uni.createFrom().nullItem();
    }

    @Override
    public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context) {
        return Uni.createFrom().item(new HttpCredentialTransport(HttpCredentialTransport.Type.OTHER_HEADER, HEADER));
    }
}
