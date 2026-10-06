package pub.galactic.chorahost.dto;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import pub.galactic.chorahost.entity.TeamInvitation;
import pub.galactic.chorahost.enums.InvitationStatus;

/**
 * {@code code} is only populated in the response to creating an invitation;
 * it cannot be recovered afterwards.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InvitationResponse(UUID id, UUID teamId, String teamName, String email, InvitationStatus status,
        Instant createdAt, Instant expiresAt, String code) {

    public static InvitationResponse from(TeamInvitation invitation, String teamName, String code) {
        return new InvitationResponse(invitation.id, invitation.teamId, teamName, invitation.email,
                invitation.status, invitation.createdAt, invitation.expiresAt, code);
    }
}
