package pub.galactic.chorahost.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import pub.galactic.chorahost.entity.AuthSession;
import pub.galactic.chorahost.entity.User;
import pub.galactic.chorahost.repository.AuthSessionRepository;
import pub.galactic.chorahost.repository.UserRepository;
import pub.galactic.chorahost.security.SessionPrincipal;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Long-lived login sessions. A client trades a verified Google ID token for a
 * session token once, then uses that as its bearer token. Sessions slide:
 * using one pushes its expiry out again, at most once per renew interval so
 * ordinary requests stay read-only.
 */
@ApplicationScoped
public class AuthSessionService {

    /** Marks a bearer token as a session token rather than a Google ID token. */
    public static final String TOKEN_PREFIX = "chs_";

    private static final Logger LOG = Logger.getLogger(AuthSessionService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    @Inject
    AuthSessionRepository sessionRepository;

    @Inject
    UserRepository userRepository;

    @ConfigProperty(name = "chorahost.session.ttl", defaultValue = "30d")
    Duration ttl;

    @ConfigProperty(name = "chorahost.session.renew-after", defaultValue = "1d")
    Duration renewAfter;

    public record CreatedSession(String token, Instant expiresAt) {
    }

    @Transactional
    public CreatedSession create(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Instant now = Instant.now();
        AuthSession session = new AuthSession();
        session.userId = user.id;
        session.tokenHash = hash(token);
        session.lastUsedAt = now;
        session.expiresAt = now.plus(ttl);
        sessionRepository.persist(session);
        return new CreatedSession(token, session.expiresAt);
    }

    /** Resolves a live session token to its user, sliding the expiry; empty if unknown or expired. */
    @Transactional
    public Optional<SessionPrincipal> authenticate(String token) {
        Instant now = Instant.now();
        return sessionRepository.findByTokenHash(hash(token))
                .filter(session -> session.expiresAt.isAfter(now))
                .flatMap(session -> userRepository.findByIdOptional(session.userId).map(user -> {
                    if (session.lastUsedAt.plus(renewAfter).isBefore(now)) {
                        session.lastUsedAt = now;
                        session.expiresAt = now.plus(ttl);
                    }
                    return new SessionPrincipal(user.id, user.email);
                }));
    }

    @Transactional
    public void revoke(String token) {
        sessionRepository.findByTokenHash(hash(token)).ifPresent(sessionRepository::delete);
    }

    @Scheduled(every = "{chorahost.session.purge-interval:1h}", concurrentExecution = ConcurrentExecution.SKIP)
    @Transactional
    public void purgeExpired() {
        long removed = sessionRepository.deleteExpired(Instant.now());
        if (removed > 0) {
            LOG.debugf("Purged %d expired sessions", removed);
        }
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
