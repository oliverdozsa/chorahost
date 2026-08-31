package pub.galactic.chorahost.security;

import pub.galactic.chorahost.entity.User;
import pub.galactic.chorahost.exception.ApiException;
import pub.galactic.chorahost.repository.UserRepository;
import io.quarkus.oidc.IdToken;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

/**
 * Resolves the {@link User} behind the authenticated Google OIDC principal,
 * auto-provisioning a local user record on first sight of a given subject.
 */
@RequestScoped
public class CurrentUserService {

    private static final Logger LOG = Logger.getLogger(CurrentUserService.class);

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    @IdToken
    JsonWebToken idToken;

    @Inject
    UserRepository userRepository;

    private User cached;

    @Transactional
    public User getOrCreateCurrentUser() {
        if (cached != null) {
            return cached;
        }
        if (securityIdentity == null || securityIdentity.isAnonymous()) {
            throw new ApiException(jakarta.ws.rs.core.Response.Status.UNAUTHORIZED, "Authentication required");
        }

        String subject = idToken.getSubject();
        String email = idToken.getClaim("email");
        String name = idToken.getClaim("name");
        if (email == null) {
            throw ApiException.badRequest("Google ID token did not include an email claim");
        }

        cached = userRepository.findByOidcSubject(subject).orElseGet(() -> {
            LOG.infof("Provisioning new user for subject %s (%s)", subject, email);
            User user = new User();
            user.oidcSubject = subject;
            user.email = email;
            user.name = name != null ? name : email;
            userRepository.persist(user);
            return user;
        });
        return cached;
    }
}
