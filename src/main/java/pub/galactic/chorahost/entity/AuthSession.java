package pub.galactic.chorahost.entity;

import java.time.Instant;
import java.util.UUID;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.UuidGenerator;

/**
 * A long-lived login session for a user. The raw session token is never
 * stored, only its SHA-256 hash.
 */
@Entity
@Table(name = "auth_sessions")
public class AuthSession extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    public UUID userId;

    @NotNull
    @Column(name = "token_hash", nullable = false, unique = true)
    public String tokenHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @NotNull
    @Column(name = "last_used_at", nullable = false)
    public Instant lastUsedAt;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    public Instant expiresAt;

    @jakarta.persistence.PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
