package pub.galactic.chorahost.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    @Inject
    SeasonService seasonService;

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
        seasonService.startFirstSeason(team);

        return team;
    }

    @Transactional
    public TeamMemberResponse assignRole(UUID teamId, User requester, UUID targetUserId, TeamRole role) {
        requireTeamAdmin(teamId, requester);

        if (role == TeamRole.TEAM_ADMIN) {
            throw ApiException.badRequest("TEAM_ADMIN cannot be assigned through this endpoint");
        }

        if (targetUserId.equals(requester.id)) {
            throw ApiException.badRequest("You cannot change your own role");
        }

        TeamMembership targetMembership = membershipRepository.findByUserAndTeam(targetUserId, teamId)
                .orElseThrow(() -> ApiException.badRequest("User is not a member of this team"));
        targetMembership.role = role;

        User target = userRepository.findByIdOptional(targetUserId)
                .orElseThrow(() -> ApiException.notFound("User not found: " + targetUserId));
        return toMemberResponse(target, targetMembership);
    }

    @Transactional
    public TeamMemberResponse setGold(UUID teamId, User requester, UUID targetUserId, int gold) {
        requireAdminOrTeamAdmin(teamId, requester);

        TeamMembership targetMembership = membershipRepository.findByUserAndTeam(targetUserId, teamId)
                .orElseThrow(() -> ApiException.badRequest("User is not a member of this team"));
        targetMembership.gold = gold;

        User target = userRepository.findByIdOptional(targetUserId)
                .orElseThrow(() -> ApiException.notFound("User not found: " + targetUserId));
        return toMemberResponse(target, targetMembership);
    }

    public List<TeamMemberResponse> getLeaderboard(UUID teamId) {
        requireTeamExists(teamId);

        List<TeamMembership> memberships = membershipRepository.findByTeamOrderByLevelDescCurrentXpDesc(teamId);
        Map<UUID, User> usersById = userRepository
                .list("id in ?1", memberships.stream().map(m -> m.userId).toList()).stream()
                .collect(Collectors.toMap(u -> u.id, u -> u));

        return memberships.stream()
                .map(m -> toMemberResponse(usersById.get(m.userId), m))
                .toList();
    }

    /**
     * Every team a user belongs to, for their profile view.
     */
    public List<TeamMembershipResponse> getMembershipsForUser(UUID userId) {
        return toMembershipResponses(membershipRepository.findByUser(userId));
    }

    /**
     * The subset of a target user's team memberships that the requester also
     * belongs to, for looking up another user's profile. Denies access
     * entirely (rather than returning an empty list) when the two users
     * share no team, since there is otherwise no relationship justifying
     * exposing the target user's data at all.
     */
    public List<TeamMembershipResponse> getSharedMembershipsForUser(UUID targetUserId, UUID requesterUserId) {
        Set<UUID> requesterTeamIds = membershipRepository.findByUser(requesterUserId).stream()
                .map(m -> m.teamId)
                .collect(Collectors.toSet());

        List<TeamMembership> sharedMemberships = membershipRepository.findByUser(targetUserId).stream()
                .filter(m -> requesterTeamIds.contains(m.teamId))
                .toList();
        if (sharedMemberships.isEmpty()) {
            throw ApiException.forbidden("No shared team with this user");
        }

        return toMembershipResponses(sharedMemberships);
    }

    private List<TeamMembershipResponse> toMembershipResponses(List<TeamMembership> memberships) {
        if (memberships.isEmpty()) {
            return List.of();
        }

        Map<UUID, String> teamNames = teamRepository
                .list("id in ?1", memberships.stream().map(m -> m.teamId).toList()).stream()
                .collect(Collectors.toMap(t -> t.id, t -> t.name));

        return memberships.stream()
                .map(m -> new TeamMembershipResponse(m.teamId, teamNames.get(m.teamId), m.role, m.level,
                        m.currentXp, m.level * 1000, m.gold, m.joinedAt))
                .toList();
    }

    public Optional<User> findUser(UUID userId) {
        return userRepository.findByIdOptional(userId);
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

    private TeamMemberResponse toMemberResponse(User user, TeamMembership membership) {
        return new TeamMemberResponse(user.id, user.name, user.givenName, user.familyName, user.email,
                membership.level, membership.currentXp, membership.level * 1000, membership.gold, membership.role);
    }
}
