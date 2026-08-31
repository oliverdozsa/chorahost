package pub.galactic.chorahost.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import pub.galactic.chorahost.dto.TeamMemberResponse;
import pub.galactic.chorahost.dto.TeamMembershipResponse;
import pub.galactic.chorahost.entity.Team;
import pub.galactic.chorahost.entity.TeamMembership;
import pub.galactic.chorahost.entity.User;
import pub.galactic.chorahost.enums.TeamRole;
import pub.galactic.chorahost.exception.ApiException;
import pub.galactic.chorahost.repository.TeamMembershipRepository;
import pub.galactic.chorahost.repository.TeamRepository;
import pub.galactic.chorahost.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TeamService {

    @Inject
    TeamRepository teamRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    TeamMembershipRepository membershipRepository;

    @Transactional
    public Team createTeam(String name, User creator) {
        Team team = new Team();
        team.name = name;
        teamRepository.persist(team);

        TeamMembership membership = new TeamMembership();
        membership.userId = creator.id;
        membership.teamId = team.id;
        membership.role = TeamRole.TEAM_ADMIN;
        membershipRepository.persist(membership);

        return team;
    }

    @Transactional
    public TeamMemberResponse assignRole(UUID teamId, User requester, UUID targetUserId, TeamRole role) {
        requireTeamAdmin(teamId, requester);

        if (role == TeamRole.TEAM_ADMIN) {
            throw ApiException.badRequest("TEAM_ADMIN cannot be assigned through this endpoint");
        }

        TeamMembership targetMembership = membershipRepository.findByUserAndTeam(targetUserId, teamId)
                .orElseThrow(() -> ApiException.badRequest("User is not a member of this team"));
        targetMembership.role = role;

        User target = userRepository.findByIdOptional(targetUserId)
                .orElseThrow(() -> ApiException.notFound("User not found: " + targetUserId));
        return toMemberResponse(target, role);
    }

    @Transactional
    public Team joinTeamByEmail(UUID teamId, String email) {
        Team team = teamRepository.findByIdOptional(teamId)
                .orElseThrow(() -> ApiException.notFound("Team not found: " + teamId));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("No user registered with email: " + email));

        if (membershipRepository.findByUserAndTeam(user.id, teamId).isPresent()) {
            throw ApiException.conflict("User is already a member of this team");
        }

        TeamMembership membership = new TeamMembership();
        membership.userId = user.id;
        membership.teamId = team.id;
        membership.role = TeamRole.USER;
        membershipRepository.persist(membership);

        return team;
    }

    public List<TeamMemberResponse> getLeaderboard(UUID teamId) {
        requireTeamExists(teamId);

        List<TeamMembership> memberships = membershipRepository.findByTeam(teamId);
        Map<UUID, TeamRole> roleByUserId = memberships.stream()
                .collect(Collectors.toMap(m -> m.userId, m -> m.role));

        return userRepository.findByIdsOrderByLevelDescCurrentXpDesc(List.copyOf(roleByUserId.keySet())).stream()
                .map(user -> toMemberResponse(user, roleByUserId.get(user.id)))
                .toList();
    }

    /**
     * Every team a user belongs to, for their profile view.
     */
    public List<TeamMembershipResponse> getMembershipsForUser(UUID userId) {
        List<TeamMembership> memberships = membershipRepository.findByUser(userId);
        if (memberships.isEmpty()) {
            return List.of();
        }

        Map<UUID, String> teamNames = teamRepository
                .list("id in ?1", memberships.stream().map(m -> m.teamId).toList()).stream()
                .collect(Collectors.toMap(t -> t.id, t -> t.name));

        return memberships.stream()
                .map(m -> new TeamMembershipResponse(m.teamId, teamNames.get(m.teamId), m.role, m.joinedAt))
                .toList();
    }

    public Optional<TeamMembership> findMembership(UUID teamId, UUID userId) {
        return membershipRepository.findByUserAndTeam(userId, teamId);
    }

    public TeamMembership requireMembership(UUID teamId, UUID userId) {
        return findMembership(teamId, userId)
                .orElseThrow(() -> ApiException.forbidden("Not a member of this team"));
    }

    public void requireTeamAdmin(UUID teamId, User user) {
        TeamMembership membership = requireMembership(teamId, user.id);
        if (membership.role != TeamRole.TEAM_ADMIN) {
            throw ApiException.forbidden("TEAM_ADMIN role required for this team");
        }
    }

    public void requireAdminOrTeamAdmin(UUID teamId, User user) {
        TeamMembership membership = requireMembership(teamId, user.id);
        if (membership.role != TeamRole.ADMIN && membership.role != TeamRole.TEAM_ADMIN) {
            throw ApiException.forbidden("ADMIN or TEAM_ADMIN role required for this team");
        }
    }

    private void requireTeamExists(UUID teamId) {
        if (!teamRepository.findByIdOptional(teamId).isPresent()) {
            throw ApiException.notFound("Team not found: " + teamId);
        }
    }

    private TeamMemberResponse toMemberResponse(User user, TeamRole role) {
        return new TeamMemberResponse(user.id, user.name, user.email, user.level, user.currentXp, user.gold, role);
    }
}
