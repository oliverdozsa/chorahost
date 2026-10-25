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
import pub.galactic.chorahost.dto.ExtendSeasonRequest;
import pub.galactic.chorahost.dto.SeasonResponse;
import pub.galactic.chorahost.dto.SeasonResultResponse;
import pub.galactic.chorahost.dto.SetGoldRequest;
import pub.galactic.chorahost.dto.SetSeasonLengthRequest;
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
import pub.galactic.chorahost.service.SeasonService;
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

    @Inject
    SeasonService seasonService;

    @GET
    @Path("/{teamId}/leaderboard")
    public List<TeamMemberResponse> getLeaderboard(@PathParam("teamId") UUID teamId) {
        User requester = currentUserService.getOrCreateCurrentUser();
        teamService.requireMembership(teamId, requester.id);
        return teamService.getLeaderboard(teamId);
    }

    @GET
    @Path("/{teamId}/seasons")
    public List<SeasonResponse> getSeasons(@PathParam("teamId") UUID teamId) {
        User requester = currentUserService.getOrCreateCurrentUser();
        return seasonService.list(teamId, requester).stream().map(SeasonResponse::from).toList();
    }

    @GET
    @Path("/{teamId}/seasons/current")
    public SeasonResponse getCurrentSeason(@PathParam("teamId") UUID teamId) {
        User requester = currentUserService.getOrCreateCurrentUser();
        return SeasonResponse.from(seasonService.getCurrent(teamId, requester));
    }

    /** Final standings of an ended season (empty while it is still running). */
    @GET
    @Path("/{teamId}/seasons/{seasonId}/results")
    public List<SeasonResultResponse> getSeasonResults(@PathParam("teamId") UUID teamId,
            @PathParam("seasonId") UUID seasonId) {
        User requester = currentUserService.getOrCreateCurrentUser();
        return seasonService.getResults(teamId, seasonId, requester);
    }

    @POST
    @Path("/{teamId}/seasons/end")
    @Consumes(MediaType.WILDCARD)
    public SeasonResponse endSeason(@PathParam("teamId") UUID teamId) {
        User requester = currentUserService.getOrCreateCurrentUser();
        return SeasonResponse.from(seasonService.endNow(teamId, requester));
    }

    @POST
    @Path("/{teamId}/seasons/extend")
    public SeasonResponse extendSeason(@PathParam("teamId") UUID teamId, @Valid ExtendSeasonRequest request) {
        User requester = currentUserService.getOrCreateCurrentUser();
        return SeasonResponse.from(seasonService.extend(teamId, requester, request.days()));
    }

    @PUT
    @Path("/{teamId}/season-length")
    public TeamResponse setSeasonLength(@PathParam("teamId") UUID teamId, @Valid SetSeasonLengthRequest request) {
        User requester = currentUserService.getOrCreateCurrentUser();
        return TeamResponse.from(seasonService.setLength(teamId, requester, request.days()));
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
        return PagedResponse.from(result, card -> ChoreCardResponse.from(card, requester.id));
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
        User requester = currentUserService.getOrCreateCurrentUser();
        teamService.requireMembership(teamId, requester.id);
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

    @DELETE
    @Path("/{teamId}/members/{userId}")
    public Response removeMember(@PathParam("teamId") UUID teamId, @PathParam("userId") UUID userId) {
        User requester = currentUserService.getOrCreateCurrentUser();
        teamService.removeMember(teamId, requester, userId);
        return Response.noContent().build();
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
