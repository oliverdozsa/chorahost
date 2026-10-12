package pub.galactic.chorahost.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import pub.galactic.chorahost.entity.AuthSession;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AuthSessionRepository implements PanacheRepositoryBase<AuthSession, UUID> {

    public Optional<AuthSession> findByTokenHash(String tokenHash) {
        return find("tokenHash", tokenHash).firstResultOptional();
    }

    public long deleteExpired(Instant now) {
        return delete("expiresAt < ?1", now);
    }
}
