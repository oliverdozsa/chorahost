package pub.galactic.chorahost.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import pub.galactic.chorahost.entity.TeamMembership;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TeamMembershipRepository implements PanacheRepositoryBase<TeamMembership, UUID> {

    public Optional<TeamMembership> findByUserAndTeam(UUID userId, UUID teamId) {
        return find("userId = ?1 and teamId = ?2", userId, teamId).firstResultOptional();
    }

    public List<TeamMembership> findByTeam(UUID teamId) {
        return find("teamId", teamId).list();
    }

    public List<TeamMembership> findByUser(UUID userId) {
        return find("userId", userId).list();
    }
}
