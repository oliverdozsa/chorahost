package pub.galactic.chorahost.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import pub.galactic.chorahost.dto.InvitationResponse;
import pub.galactic.chorahost.entity.Team;
import pub.galactic.chorahost.entity.TeamInvitation;
import pub.galactic.chorahost.entity.TeamMembership;
import pub.galactic.chorahost.entity.User;
import pub.galactic.chorahost.enums.InvitationStatus;
import pub.galactic.chorahost.enums.TeamRole;
import pub.galactic.chorahost.exception.ApiException;
import pub.galactic.chorahost.repository.TeamInvitationRepository;
import pub.galactic.chorahost.repository.TeamMembershipRepository;
import pub.galactic.chorahost.repository.TeamRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class InvitationService {

    static final Duration INVITATION_TTL = Duration.ofDays(7);

    private static final SecureRandom RANDOM = new SecureRandom();

    @Inject
    TeamInvitationRepository invitationRepository;

    @Inject
    TeamRepository teamRepository;

    @Inject
    TeamMembershipRepository membershipRepository;

    @Inject
    TeamService teamService;

    @Inject
    Event<InvitationCreated> invitationCreated;

    /**
     * Invites an email address to a team. Any still-pending invitation for the
     * same team and email is revoked, so re-inviting issues a fresh code.
     */
    @Transactional
    public InvitationResponse invite(UUID teamId, User requester, String email) {
        teamService.requireAdminOrTeamAdmin(teamId, requester);
        Team team = teamRepository.findByIdOptional(teamId)
                .orElseThrow(() -> ApiException.notFound("Team not found: " + teamId));

        String normalizedEmail = normalize(email);
        boolean alreadyMember = membershipRepository.findByTeam(teamId).stream()
                .map(m -> m.userId)
                .anyMatch(userId -> isUserWithEmail(userId, normalizedEmail));
        if (alreadyMember) {
            throw ApiException.conflict("User is already a member of this team");
        }

        invitationRepository.findPending(teamId, normalizedEmail).ifPresent(existing -> {
            existing.status = InvitationStatus.REVOKED;
            // Flush so the partial unique index sees the revocation before the insert.
            invitationRepository.flush();
        });

        String code = generateCode();
        TeamInvitation invitation = new TeamInvitation();
        invitation.teamId = teamId;
        invitation.email = normalizedEmail;
        invitation.codeHash = hash(code);
        invitation.invitedBy = requester.id;
        invitation.expiresAt = Instant.now().plus(INVITATION_TTL);
        invitationRepository.persist(invitation);

        // Observed after the transaction commits, so a rolled-back invitation is never mailed.
        invitationCreated.fire(new InvitationCreated(normalizedEmail, team.name, requester.name, code,
                invitation.expiresAt));

        return InvitationResponse.from(invitation, team.name, code);
    }

    public List<InvitationResponse> listForTeam(UUID teamId, User requester) {
        teamService.requireAdminOrTeamAdmin(teamId, requester);
        String teamName = teamRepository.findByIdOptional(teamId)
                .orElseThrow(() -> ApiException.notFound("Team not found: " + teamId)).name;
        return invitationRepository.findPendingByTeam(teamId).stream()
                .map(i -> InvitationResponse.from(i, teamName, null))
                .toList();
    }

    @Transactional
    public void revoke(UUID teamId, User requester, UUID invitationId) {
        teamService.requireAdminOrTeamAdmin(teamId, requester);
        TeamInvitation invitation = invitationRepository.findByIdOptional(invitationId)
                .filter(i -> i.teamId.equals(teamId) && i.status == InvitationStatus.PENDING)
                .orElseThrow(() -> ApiException.notFound("Pending invitation not found: " + invitationId));
        invitation.status = InvitationStatus.REVOKED;
    }

    /**
     * Pending, unexpired invitations addressed to the requester's email.
     */
    public List<InvitationResponse> listForUser(User requester) {
        Instant now = Instant.now();
        return invitationRepository.findPendingByEmail(normalize(requester.email)).stream()
                .filter(i -> i.expiresAt.isAfter(now))
                .map(i -> InvitationResponse.from(i, teamName(i.teamId), null))
                .toList();
    }

    @Transactional
    public Team acceptByCode(User requester, String code) {
        TeamInvitation invitation = invitationRepository.findByCodeHash(hash(code.trim()))
                .orElseThrow(InvitationService::invalidInvitation);
        return accept(requester, invitation);
    }

    @Transactional
    public Team acceptById(User requester, UUID invitationId) {
        TeamInvitation invitation = invitationRepository.findByIdOptional(invitationId)
                .orElseThrow(InvitationService::invalidInvitation);
        return accept(requester, invitation);
    }

    private Team accept(User requester, TeamInvitation invitation) {
        // Deliberately one generic error for unknown, wrong-email, used,
        // revoked and expired invitations, so nothing about them leaks.
        if (invitation.status != InvitationStatus.PENDING
                || invitation.expiresAt.isBefore(Instant.now())
                || !invitation.email.equals(normalize(requester.email))) {
            throw invalidInvitation();
        }
        Team team = teamRepository.findByIdOptional(invitation.teamId)
                .orElseThrow(InvitationService::invalidInvitation);

        if (membershipRepository.findByUserAndTeam(requester.id, team.id).isPresent()) {
            throw ApiException.conflict("User is already a member of this team");
        }

        TeamMembership membership = new TeamMembership();
        membership.userId = requester.id;
        membership.teamId = team.id;
        membership.role = TeamRole.USER;
        membershipRepository.persist(membership);

        invitation.status = InvitationStatus.ACCEPTED;
        invitation.acceptedAt = Instant.now();
        return team;
    }

    private String teamName(UUID teamId) {
        return teamRepository.findByIdOptional(teamId).map(t -> t.name).orElse(null);
    }

    private boolean isUserWithEmail(UUID userId, String normalizedEmail) {
        return teamService.findUser(userId).map(u -> normalize(u.email).equals(normalizedEmail)).orElse(false);
    }

    private static ApiException invalidInvitation() {
        return ApiException.notFound("Invitation not found, expired or not addressed to you");
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase();
    }

    private static String generateCode() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
