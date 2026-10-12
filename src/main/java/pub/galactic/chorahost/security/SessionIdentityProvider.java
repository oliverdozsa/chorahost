package pub.galactic.chorahost.security;

import pub.galactic.chorahost.service.AuthSessionService;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Resolves a session token to a {@link SecurityIdentity}; runs on a worker thread since it hits the database. */
@ApplicationScoped
public class SessionIdentityProvider implements IdentityProvider<SessionAuthenticationRequest> {

    @Inject
    AuthSessionService sessionService;

    @Override
    public Class<SessionAuthenticationRequest> getRequestType() {
        return SessionAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(SessionAuthenticationRequest request, AuthenticationRequestContext context) {
        return context.runBlocking(() -> {
            SessionPrincipal principal = sessionService.authenticate(request.token())
                    .orElseThrow(() -> new AuthenticationFailedException("Invalid or expired session"));
            return QuarkusSecurityIdentity.builder().setPrincipal(principal).build();
        });
    }
}
