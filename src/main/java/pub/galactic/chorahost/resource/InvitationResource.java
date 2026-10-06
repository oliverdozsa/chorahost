package pub.galactic.chorahost.resource;

import java.util.List;
import java.util.UUID;

import pub.galactic.chorahost.dto.AcceptInvitationRequest;
import pub.galactic.chorahost.dto.InvitationResponse;
import pub.galactic.chorahost.dto.TeamResponse;
import pub.galactic.chorahost.entity.User;
import pub.galactic.chorahost.security.CurrentUserService;
import pub.galactic.chorahost.service.InvitationService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * The invitee's side of team invitations. Inviting and revoking live on
 * {@link TeamResource} since they are scoped to a team.
 */
@Path("/api/v1/invitations")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InvitationResource {

    @Inject
    InvitationService invitationService;

    @Inject
    CurrentUserService currentUserService;

    @GET
    public List<InvitationResponse> myInvitations() {
        return invitationService.listForUser(currentUserService.getOrCreateCurrentUser());
    }

    @POST
    @Path("/accept")
    public TeamResponse acceptByCode(@Valid AcceptInvitationRequest request) {
        User requester = currentUserService.getOrCreateCurrentUser();
        return TeamResponse.from(invitationService.acceptByCode(requester, request.code()));
    }

    @POST
    @Path("/{invitationId}/accept")
    public TeamResponse acceptById(@PathParam("invitationId") UUID invitationId) {
        User requester = currentUserService.getOrCreateCurrentUser();
        return TeamResponse.from(invitationService.acceptById(requester, invitationId));
    }
}
