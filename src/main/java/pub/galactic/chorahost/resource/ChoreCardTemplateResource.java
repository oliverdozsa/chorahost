package pub.galactic.chorahost.resource;

import java.net.URI;
import java.util.UUID;

import pub.galactic.chorahost.dto.ChoreCardResponse;
import pub.galactic.chorahost.dto.ChoreCardTemplateResponse;
import pub.galactic.chorahost.dto.CreateChoreCardTemplateRequest;
import pub.galactic.chorahost.dto.InstantiateCardRequest;
import pub.galactic.chorahost.entity.ChoreCard;
import pub.galactic.chorahost.entity.ChoreCardTemplate;
import pub.galactic.chorahost.entity.User;
import pub.galactic.chorahost.security.CurrentUserService;
import pub.galactic.chorahost.service.ChoreCardTemplateService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/card-templates")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChoreCardTemplateResource {

    @Inject
    ChoreCardTemplateService templateService;

    @Inject
    CurrentUserService currentUserService;

    @POST
    public Response createTemplate(@Valid CreateChoreCardTemplateRequest request) {
        User requester = currentUserService.getOrCreateCurrentUser();
        ChoreCardTemplate template = templateService.createTemplate(request, requester);
        return Response.created(URI.create("/api/v1/card-templates/" + template.id))
                .entity(ChoreCardTemplateResponse.from(template))
                .build();
    }
}
