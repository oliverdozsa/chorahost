package pub.galactic.chorahost.resource;

import pub.galactic.chorahost.dto.AuthSessionResponse;
import pub.galactic.chorahost.entity.User;
import pub.galactic.chorahost.exception.ApiException;
import pub.galactic.chorahost.security.CurrentUserService;
import pub.galactic.chorahost.security.SessionPrincipal;
import pub.galactic.chorahost.service.AuthSessionService;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/api/v1/auth/session")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    JsonWebToken idToken;

    @Inject
    CurrentUserService currentUserService;

    @Inject
    AuthSessionService sessionService;

    /**
     * Trades the caller's Google ID token for a long-lived session token. The
     * ID token is only good for an hour, so clients keep the session token
     * instead and send it as their bearer token from then on. A session token
     * cannot mint further sessions, so renewal always goes back through Google.
     */
    @POST
    public AuthSessionResponse createSession() {
        if (securityIdentity.getPrincipal() instanceof SessionPrincipal) {
            throw ApiException.forbidden("Sign in with Google to start a new session");
        }
        User user = currentUserService.getOrCreateCurrentUser();
        AuthSessionService.CreatedSession session = sessionService.create(user);
        return new AuthSessionResponse(session.token(), session.expiresAt(), user.name, user.email, pictureClaim());
    }

    /** Ends the caller's session. A no-op for requests authenticated with a Google ID token. */
    @DELETE
    public Response endSession(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        if (securityIdentity.getPrincipal() instanceof SessionPrincipal && authorization != null) {
            sessionService.revoke(authorization.substring("Bearer ".length()).trim());
        }
        return Response.noContent().build();
    }

    private String pictureClaim() {
        // Dev-header identities have no ID token to read claims from.
        return securityIdentity.getPrincipal() instanceof JsonWebToken ? idToken.getClaim("picture") : null;
    }
}
