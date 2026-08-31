package pub.galactic.chorahost.entity;

import java.time.Instant;
import java.util.UUID;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "users")
public class User extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    /**
     * The 'sub' claim of the Google-issued OIDC ID token. Used to resolve the
     * authenticated principal to a local user record.
     */
    @Column(name = "oidc_subject", nullable = false, unique = true)
    public String oidcSubject;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    public String email;

    @NotBlank
    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public Integer level = 1;

    @Column(name = "current_xp", nullable = false)
    public Integer currentXp = 0;

    @Column(nullable = false)
    public Integer gold = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @jakarta.persistence.PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
