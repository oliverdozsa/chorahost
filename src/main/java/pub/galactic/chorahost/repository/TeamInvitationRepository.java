package pub.galactic.chorahost.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import pub.galactic.chorahost.entity.TeamInvitation;
import pub.galactic.chorahost.enums.InvitationStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TeamInvitationRepository implements PanacheRepositoryBase<TeamInvitation, UUID> {

    public Optional<TeamInvitation> findByCodeHash(String codeHash) {
        return find("codeHash", codeHash).firstResultOptional();
    }

    public Optional<TeamInvitation> findPending(UUID teamId, String email) {
        return find("teamId = ?1 and email = ?2 and status = ?3", teamId, email, InvitationStatus.PENDING)
                .firstResultOptional();
    }

    public List<TeamInvitation> findPendingByTeam(UUID teamId) {
        return find("teamId = ?1 and status = ?2 order by createdAt desc", teamId, InvitationStatus.PENDING).list();
    }

    public List<TeamInvitation> findPendingByEmail(String email) {
        return find("email = ?1 and status = ?2 order by createdAt desc", email, InvitationStatus.PENDING).list();
    }
}
