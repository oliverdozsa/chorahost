package pub.galactic.chorahost.resource;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import pub.galactic.chorahost.dto.AssignRoleRequest;
import pub.galactic.chorahost.dto.ChoreCardResponse;
import pub.galactic.chorahost.dto.ChoreCardTemplateResponse;
import pub.galactic.chorahost.dto.CreateInvitationRequest;
import pub.galactic.chorahost.dto.CreateTeamRequest;
import pub.galactic.chorahost.dto.InvitationResponse;
import pub.galactic.chorahost.dto.PagedResponse;
import pub.galactic.chorahost.dto.SetGoldRequest;
import pub.galactic.chorahost.dto.TeamMemberResponse;
import pub.galactic.chorahost.dto.TeamResponse;
import pub.galactic.chorahost.entity.ChoreCard;
import pub.galactic.chorahost.entity.Team;
import pub.galactic.chorahost.entity.User;
import pub.galactic.chorahost.enums.CardStatus;
import pub.galactic.chorahost.exception.ApiException;
import pub.galactic.chorahost.security.CurrentUserService;
import pub.galactic.chorahost.service.ChoreCardService;
import pub.galactic.chorahost.service.ChoreCardTemplateService;
import pub.galactic.chorahost.service.InvitationService;
import pub.galactic.chorahost.service.PagedResult;
import pub.galactic.chorahost.service.TeamService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/teams")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TeamResource {

    @Inject
    TeamService teamService;

    @Inject
    CurrentUserService currentUserService;

    @Inject
    ChoreCardService choreCardService;

    @Inject
    ChoreCardTemplateService templateService;

    @Inject
    InvitationService invitationService;

    @GET
    @Path("/{teamId}/leaderboard")
    public List<TeamMemberResponse> getLeaderboard(@PathParam("teamId") UUID teamId) {
        return teamService.getLeaderboard(teamId);
    }

    @GET
    @Path("/{teamId}/cards")
    public PagedResponse<ChoreCardResponse> getTeamCards(@PathParam("teamId") UUID teamId,
            @QueryParam("status") String status,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("" + ChoreCardService.DEFAULT_PAGE_SIZE) int size) {
        User requester = currentUserService.getOrCreateCurrentUser();
        List<CardStatus> statuses = parseStatuses(status);
        PagedResult<ChoreCard> result = choreCardService.getCardsForTeam(teamId, statuses, requester, page, size);
        return PagedResponse.from(result, ChoreCardResponse::from);
    }

    private static List<CardStatus> parseStatuses(String status) {
        if (status == null || status.isBlank()) {
            return List.of();
        }
        return Arrays.stream(status.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return CardStatus.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw ApiException.badRequest("Invalid card status: " + s);
                    }
                })
                .toList();
    }

    @GET
    @Path("/{teamId}/card-templates")
    public List<ChoreCardTemplateResponse> getTeamCardTemplates(@PathParam("teamId") UUID teamId) {
        return templateService.getTemplatesForTeam(teamId).stream().map(ChoreCardTemplateResponse::from).toList();
    }

    @POST
    public Response createTeam(@Valid CreateTeamRequest request) {
        User requester = currentUserService.getOrCreateCurrentUser();
        Team team = teamService.createTeam(request.name(), requester);
        return Response.created(URI.create("/api/v1/teams/" + team.id))
                .entity(TeamResponse.from(team))
                .build();
    }

    @POST
    @Path("/{teamId}/role")
    public TeamMemberResponse assignRole(@PathParam("teamId") UUID teamId, @Valid AssignRoleRequest request) {
        User requester = currentUserService.getOrCreateCurrentUser();
        return teamService.assignRole(teamId, requester, request.userId(), request.role());
    }

    @PUT
    @Path("/{teamId}/members/{userId}/gold")
    public TeamMemberResponse setMemberGold(@PathParam("teamId") UUID teamId, @PathParam("userId") UUID userId,
            @Valid SetGoldRequest request) {
        User requester = currentUserService.getOrCreateCurrentUser();
        return teamService.setGold(teamId, requester, userId, request.gold());
    }

    @POST
    @Path("/{teamId}/invitations")
    public Response invite(@PathParam("teamId") UUID teamId, @Valid CreateInvitationRequest request) {
        User requester = currentUserService.getOrCreateCurrentUser();
        InvitationResponse invitation = invitationService.invite(teamId, requester, request.email());
        return Response.created(URI.create("/api/v1/teams/" + teamId + "/invitations/" + invitation.id()))
                .entity(invitation)
                .build();
    }

    @GET
    @Path("/{teamId}/invitations")
    public List<InvitationResponse> getInvitations(@PathParam("teamId") UUID teamId) {
        User requester = currentUserService.getOrCreateCurrentUser();
        return invitationService.listForTeam(teamId, requester);
    }

    @DELETE
    @Path("/{teamId}/invitations/{invitationId}")
    public Response revokeInvitation(@PathParam("teamId") UUID teamId,
            @PathParam("invitationId") UUID invitationId) {
        User requester = currentUserService.getOrCreateCurrentUser();
        invitationService.revoke(teamId, requester, invitationId);
        return Response.noContent().build();
    }
}
