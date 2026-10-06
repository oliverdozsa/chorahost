package pub.galactic.chorahost.entity;

import java.time.Instant;
import java.util.UUID;

import pub.galactic.chorahost.enums.InvitationStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.UuidGenerator;

/**
 * An invitation for a specific email address to join a team. The raw invite
 * code is never stored, only its SHA-256 hash.
 */
@Entity
@Table(name = "team_invitations")
public class TeamInvitation extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @UuidGenerator
    public UUID id;

    @NotNull
    @Column(name = "team_id", nullable = false)
    public UUID teamId;

    /** Lowercased. */
    @NotNull
    @Column(nullable = false)
    public String email;

    @NotNull
    @Column(name = "code_hash", nullable = false, unique = true)
    public String codeHash;

    @NotNull
    @Column(name = "invited_by", nullable = false)
    public UUID invitedBy;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public InvitationStatus status = InvitationStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    public Instant expiresAt;

    @Column(name = "accepted_at")
    public Instant acceptedAt;

    @jakarta.persistence.PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
