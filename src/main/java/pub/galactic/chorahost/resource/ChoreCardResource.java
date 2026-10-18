package pub.galactic.chorahost.resource;

import java.net.URI;
import java.util.UUID;

import pub.galactic.chorahost.dto.CardVerificationResponse;
import pub.galactic.chorahost.dto.ChoreCardResponse;
import pub.galactic.chorahost.dto.CreateChoreCardRequest;
import pub.galactic.chorahost.dto.UpdateChoreCardRequest;
import pub.galactic.chorahost.entity.ChoreCard;
import pub.galactic.chorahost.entity.User;
import pub.galactic.chorahost.security.CurrentUserService;
import pub.galactic.chorahost.service.ChoreCardService;
import pub.galactic.chorahost.service.ChoreCardService.VerificationResult;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/cards")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class ChoreCardResource {

    @Inject
    ChoreCardService choreCardService;

    @Inject
    CurrentUserService currentUserService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createCard(@Valid CreateChoreCardRequest request) {
        User requester = currentUserService.getOrCreateCurrentUser();
        ChoreCard card = choreCardService.createCard(request, requester);
        return Response.created(URI.create("/api/v1/cards/" + card.id))
                .entity(ChoreCardResponse.from(card, requester.id))
                .build();
    }

    @PUT
    @Path("/{cardId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public ChoreCardResponse updateCard(@PathParam("cardId") UUID cardId, @Valid UpdateChoreCardRequest request) {
        User requester = currentUserService.getOrCreateCurrentUser();
        return ChoreCardResponse.from(choreCardService.updateCard(cardId, request, requester), requester.id);
    }

    @DELETE
    @Path("/{cardId}")
    public Response deleteCard(@PathParam("cardId") UUID cardId) {
        User requester = currentUserService.getOrCreateCurrentUser();
        choreCardService.deleteCard(cardId, requester);
        return Response.noContent().build();
    }

    @POST
    @Path("/{cardId}/claim")
    public ChoreCardResponse claimCard(@PathParam("cardId") UUID cardId) {
        User requester = currentUserService.getOrCreateCurrentUser();
        return ChoreCardResponse.from(choreCardService.claimCard(cardId, requester), requester.id);
    }

    @POST
    @Path("/{cardId}/unclaim")
    public ChoreCardResponse unclaimCard(@PathParam("cardId") UUID cardId) {
        User requester = currentUserService.getOrCreateCurrentUser();
        return ChoreCardResponse.from(choreCardService.unclaimCard(cardId, requester), requester.id);
    }

    @POST
    @Path("/{cardId}/complete")
    public ChoreCardResponse completeCard(@PathParam("cardId") UUID cardId) {
        User requester = currentUserService.getOrCreateCurrentUser();
        return ChoreCardResponse.from(choreCardService.completeCard(cardId, requester), requester.id);
    }

    @POST
    @Path("/{cardId}/acknowledge-reward")
    public ChoreCardResponse acknowledgeReward(@PathParam("cardId") UUID cardId) {
        User requester = currentUserService.getOrCreateCurrentUser();
        return ChoreCardResponse.from(choreCardService.acknowledgeReward(cardId, requester), requester.id);
    }

    @POST
    @Path("/{cardId}/verify")
    public CardVerificationResponse verifyCard(@PathParam("cardId") UUID cardId) {
        User requester = currentUserService.getOrCreateCurrentUser();
        VerificationResult result = choreCardService.verifyCard(cardId, requester);
        return new CardVerificationResponse(
                ChoreCardResponse.from(result.card(), requester.id),
                result.xpGained(),
                result.goldGained(),
                result.leveledUp(),
                result.newLevel());
    }
}
