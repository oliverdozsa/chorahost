package pub.galactic.chorahost.resource;

import java.util.UUID;

import pub.galactic.chorahost.dto.JoinTeamRequest;
import pub.galactic.chorahost.dto.TeamResponse;
import pub.galactic.chorahost.entity.Team;
import pub.galactic.chorahost.service.TeamService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Separate resource for the singular {@code /api/v1/team/{teamId}/join} path
 * called out explicitly in the API spec, alongside the plural
 * {@code /api/v1/teams} resource used for everything else.
 */
@Path("/api/v1/team")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TeamJoinResource {

    @Inject
    TeamService teamService;

    @POST
    @Path("/{teamId}/join")
    public TeamResponse join(@PathParam("teamId") UUID teamId, @Valid JoinTeamRequest request) {
        Team team = teamService.joinTeamByEmail(teamId, request.email());
        return TeamResponse.from(team);
    }
}
