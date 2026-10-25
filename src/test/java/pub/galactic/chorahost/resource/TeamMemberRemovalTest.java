package pub.galactic.chorahost.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Removing a member from a team: TEAM_ADMIN-only, and the removed member's
 * unverified (claimed or completed) cards are returned to the pool.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TeamMemberRemovalTest extends AbstractResourceTest {

    private static final String OWNER_SUB = "removal-owner-sub";
    private static final String OWNER_EMAIL = "removal-owner@chorahost.test";
    private static final String MEMBER_SUB = "removal-member-sub";
    private static final String MEMBER_EMAIL = "removal-member@chorahost.test";
    private static final String OTHER_SUB = "removal-other-sub";
    private static final String OTHER_EMAIL = "removal-other@chorahost.test";

    private static String teamId;
    private static String ownerId;
    private static String memberId;
    private static String otherId;
    private static String claimedCardId;
    private static String completedCardId;
    private static String otherCardId;

    @Test
    @Order(1)
    @TestSecurity(user = OWNER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = OWNER_SUB),
            @Claim(key = "email", value = OWNER_EMAIL),
            @Claim(key = "name", value = "Removal Owner")
    })
    void setUpTeamAndInvite() {
        teamId = given()
                .contentType("application/json")
                .body("{\"name\":\"Removal Team\"}")
                .when().post("/api/v1/teams")
                .then().statusCode(201)
                .extract().path("id");

        ownerId = given().when().get("/api/v1/users/me").then().statusCode(200).extract().path("id");

        for (String email : new String[] { MEMBER_EMAIL, OTHER_EMAIL }) {
            given()
                    .contentType("application/json")
                    .body("{\"email\":\"" + email + "\"}")
                    .when().post("/api/v1/teams/" + teamId + "/invitations")
                    .then().statusCode(201);
        }
        claimedCardId = createCard();
        completedCardId = createCard();
        otherCardId = createCard();
    }

    @Test
    @Order(2)
    @TestSecurity(user = MEMBER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = MEMBER_SUB),
            @Claim(key = "email", value = MEMBER_EMAIL),
            @Claim(key = "name", value = "Removal Member")
    })
    void memberJoinsAndHoldsCards() {
        memberId = given().when().get("/api/v1/users/me").then().statusCode(200).extract().path("id");
        acceptPendingInvitation();

        given().when().post("/api/v1/cards/" + claimedCardId + "/claim").then().statusCode(200);
        given().when().post("/api/v1/cards/" + completedCardId + "/claim").then().statusCode(200);
        given().when().post("/api/v1/cards/" + completedCardId + "/complete")
                .then().statusCode(200)
                .body("status", is("COMPLETED"));
    }

    @Test
    @Order(3)
    @TestSecurity(user = OTHER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = OTHER_SUB),
            @Claim(key = "email", value = OTHER_EMAIL),
            @Claim(key = "name", value = "Removal Other")
    })
    void otherMemberJoinsAndClaimsCard() {
        otherId = given().when().get("/api/v1/users/me").then().statusCode(200).extract().path("id");
        acceptPendingInvitation();

        given().when().post("/api/v1/cards/" + otherCardId + "/claim").then().statusCode(200);
    }

    @Test
    @Order(4)
    @TestSecurity(user = OTHER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = OTHER_SUB),
            @Claim(key = "email", value = OTHER_EMAIL),
            @Claim(key = "name", value = "Removal Other")
    })
    void plainUserCannotRemoveMembers() {
        given()
                .when().delete("/api/v1/teams/" + teamId + "/members/" + memberId)
                .then().statusCode(403);
    }

    @Test
    @Order(5)
    @TestSecurity(user = OWNER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = OWNER_SUB),
            @Claim(key = "email", value = OWNER_EMAIL),
            @Claim(key = "name", value = "Removal Owner")
    })
    void teamAdminCannotRemoveThemselves() {
        given()
                .when().delete("/api/v1/teams/" + teamId + "/members/" + ownerId)
                .then().statusCode(400);
    }

    @Test
    @Order(6)
    @TestSecurity(user = OWNER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = OWNER_SUB),
            @Claim(key = "email", value = OWNER_EMAIL),
            @Claim(key = "name", value = "Removal Owner")
    })
    void teamAdminRemovesMemberAndTheirCardsAreUnclaimed() {
        given()
                .when().delete("/api/v1/teams/" + teamId + "/members/" + memberId)
                .then().statusCode(204);

        given()
                .when().get("/api/v1/teams/" + teamId + "/leaderboard")
                .then().statusCode(200)
                .body("", hasSize(2))
                .body("find { it.userId == '" + memberId + "' }", is(nullValue()));

        for (String cardId : new String[] { claimedCardId, completedCardId }) {
            given()
                    .when().get("/api/v1/teams/" + teamId + "/cards?status=AVAILABLE")
                    .then().statusCode(200)
                    .body("items.find { it.id == '" + cardId + "' }.status", is("AVAILABLE"))
                    .body("items.find { it.id == '" + cardId + "' }.assignedUserId", is(nullValue()));
        }
    }

    @Test
    @Order(7)
    @TestSecurity(user = OWNER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = OWNER_SUB),
            @Claim(key = "email", value = OWNER_EMAIL),
            @Claim(key = "name", value = "Removal Owner")
    })
    void otherMembersCardsAreUntouched() {
        given()
                .when().get("/api/v1/teams/" + teamId + "/cards?status=CLAIMED")
                .then().statusCode(200)
                .body("items.find { it.id == '" + otherCardId + "' }.assignedUserId", is(otherId));
    }

    @Test
    @Order(8)
    @TestSecurity(user = OWNER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = OWNER_SUB),
            @Claim(key = "email", value = OWNER_EMAIL),
            @Claim(key = "name", value = "Removal Owner")
    })
    void removingNonMemberReturnsNotFound() {
        given()
                .when().delete("/api/v1/teams/" + teamId + "/members/" + memberId)
                .then().statusCode(404);
    }

    @Test
    @Order(9)
    @TestSecurity(user = MEMBER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = MEMBER_SUB),
            @Claim(key = "email", value = MEMBER_EMAIL),
            @Claim(key = "name", value = "Removal Member")
    })
    void removedMemberLosesTeamAccess() {
        given()
                .when().get("/api/v1/teams/" + teamId + "/leaderboard")
                .then().statusCode(403);
        given()
                .when().post("/api/v1/cards/" + claimedCardId + "/claim")
                .then().statusCode(403);
    }

    private static void acceptPendingInvitation() {
        String invitationId = given()
                .when().get("/api/v1/invitations")
                .then().statusCode(200)
                .extract().path("[0].id");
        given()
                .contentType("application/json")
                .when().post("/api/v1/invitations/" + invitationId + "/accept")
                .then().statusCode(200);
    }

    private static String createCard() {
        return given()
                .contentType("application/json")
                .body("{\"teamId\":\"" + teamId + "\",\"title\":\"Take out the trash\","
                        + "\"rarity\":\"COMMON\",\"complexity\":2,\"estimatedMinutes\":5,"
                        + "\"xpReward\":10,\"goldReward\":1}")
                .when().post("/api/v1/cards")
                .then().statusCode(201)
                .extract().path("id");
    }
}
