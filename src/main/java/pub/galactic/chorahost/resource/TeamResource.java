package pub.galactic.chorahost.resource;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import pub.galactic.chorahost.dto.AssignRoleRequest;
import pub.galactic.chorahost.dto.ChoreCardResponse;
import pub.galactic.chorahost.dto.ChoreCardTemplateResponse;
import pub.galactic.chorahost.dto.CreateTeamRequest;
import pub.galactic.chorahost.dto.TeamMemberResponse;
import pub.galactic.chorahost.dto.TeamResponse;
import pub.galactic.chorahost.entity.Team;
import pub.galactic.chorahost.entity.User;
import pub.galactic.chorahost.security.CurrentUserService;
import pub.galactic.chorahost.service.ChoreCardService;
import pub.galactic.chorahost.service.ChoreCardTemplateService;
import pub.galactic.chorahost.service.TeamService;
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

    @GET
    @Path("/{teamId}/leaderboard")
    public List<TeamMemberResponse> getLeaderboard(@PathParam("teamId") UUID teamId) {
        return teamService.getLeaderboard(teamId);
    }

    @GET
    @Path("/{teamId}/cards")
    public List<ChoreCardResponse> getTeamCards(@PathParam("teamId") UUID teamId) {
        return choreCardService.getAvailableCardsForTeam(teamId).stream().map(ChoreCardResponse::from).toList();
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
}
