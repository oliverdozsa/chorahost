package pub.galactic.chorahost.resource;

import java.util.UUID;

import pub.galactic.chorahost.dto.ChoreCardResponse;
import pub.galactic.chorahost.dto.UserResponse;
import pub.galactic.chorahost.entity.User;
import pub.galactic.chorahost.exception.ApiException;
import pub.galactic.chorahost.repository.UserRepository;
import pub.galactic.chorahost.security.CurrentUserService;
import pub.galactic.chorahost.service.ChoreCardService;
import pub.galactic.chorahost.service.TeamService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1/users")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserRepository userRepository;

    @Inject
    ChoreCardService choreCardService;

    @Inject
    CurrentUserService currentUserService;

    @Inject
    TeamService teamService;

    /**
     * Resolves (and auto-provisions on first sight) the chorahost user record
     * behind the caller's Google ID token. The frontend calls this right
     * after Google sign-in to learn its own user id.
     */
    @GET
    @Path("/me")
    public UserResponse getCurrentUser() {
        User user = currentUserService.getOrCreateCurrentUser();
        return UserResponse.from(user, teamService.getMembershipsForUser(user.id));
    }

    @GET
    @Path("/{id}")
    public UserResponse getUser(@PathParam("id") UUID id) {
        User user = userRepository.findByIdOptional(id)
                .orElseThrow(() -> ApiException.notFound("User not found: " + id));
        return UserResponse.from(user, teamService.getMembershipsForUser(id));
    }

    @GET
    @Path("/{userId}/hand")
    public java.util.List<ChoreCardResponse> getHand(@PathParam("userId") UUID userId) {
        return choreCardService.getHand(userId).stream().map(ChoreCardResponse::from).toList();
    }
}
