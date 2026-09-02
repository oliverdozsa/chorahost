package pub.galactic.chorahost.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Full chore card lifecycle: create (admin-only) -> claim -> complete (by the
 * assignee) -> verify (admin-only, awards XP/gold and resolves level-up).
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChoreCardResourceTest extends AbstractResourceTest {

    private static final String ADMIN_SUB = "card-admin-sub";
    private static final String ADMIN_EMAIL = "card-admin@chorahost.test";
    private static final String PLAYER_SUB = "card-player-sub";
    private static final String PLAYER_EMAIL = "card-player@chorahost.test";

    private static String teamId;
    private static String playerId;
    private static String cardId;
    private static String bigCardId;

    @Test
    @Order(1)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Card Admin")
    })
    void setUpTeam() {
        teamId = given()
                .contentType("application/json")
                .body("{\"name\":\"Card Test Team\"}")
                .when().post("/api/v1/teams")
                .then().statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(2)
    @TestSecurity(user = PLAYER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = PLAYER_SUB),
            @Claim(key = "email", value = PLAYER_EMAIL),
            @Claim(key = "name", value = "Card Player")
    })
    void playerJoinsTeam() {
        playerId = given()
                .when().get("/api/v1/users/me")
                .then().statusCode(200)
                .extract().path("id");
    }

    @Test
    @Order(3)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Card Admin")
    })
    void adminAddsPlayerToTeam() {
        given()
                .contentType("application/json")
                .body("{\"email\":\"" + PLAYER_EMAIL + "\"}")
                .when().post("/api/v1/team/" + teamId + "/join")
                .then().statusCode(200);
    }

    @Test
    @Order(4)
    @TestSecurity(user = PLAYER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = PLAYER_SUB),
            @Claim(key = "email", value = PLAYER_EMAIL),
            @Claim(key = "name", value = "Card Player")
    })
    void nonAdminCannotCreateCard() {
        given()
                .contentType("application/json")
                .body(cardPayload(100, 10))
                .when().post("/api/v1/cards")
                .then().statusCode(403);
    }

    @Test
    @Order(5)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Card Admin")
    })
    void adminCreatesCard() {
        cardId = given()
                .contentType("application/json")
                .body(cardPayload(100, 10))
                .when().post("/api/v1/cards")
                .then().statusCode(201)
                .body("status", is("AVAILABLE"))
                .extract().path("id");

        given()
                .when().get("/api/v1/teams/" + teamId + "/cards")
                .then().statusCode(200)
                .body("find { it.id == '" + cardId + "' }.status", is("AVAILABLE"));
    }

    @Test
    @Order(6)
    @TestSecurity(user = PLAYER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = PLAYER_SUB),
            @Claim(key = "email", value = PLAYER_EMAIL),
            @Claim(key = "name", value = "Card Player")
    })
    void playerClaimsCard() {
        given()
                .when().post("/api/v1/cards/" + cardId + "/claim")
                .then().statusCode(200)
                .body("status", is("CLAIMED"))
                .body("assignedUserId", is(playerId));

        given()
                .when().get("/api/v1/users/" + playerId + "/hand")
                .then().statusCode(200)
                .body("find { it.id == '" + cardId + "' }.status", is("CLAIMED"));
    }

    @Test
    @Order(7)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Card Admin")
    })
    void claimingAnAlreadyClaimedCardConflicts() {
        given()
                .when().post("/api/v1/cards/" + cardId + "/claim")
                .then().statusCode(409);
    }

    @Test
    @Order(8)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Card Admin")
    })
    void onlyTheAssigneeCanCompleteTheCard() {
        given()
                .when().post("/api/v1/cards/" + cardId + "/complete")
                .then().statusCode(403);
    }

    @Test
    @Order(9)
    @TestSecurity(user = PLAYER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = PLAYER_SUB),
            @Claim(key = "email", value = PLAYER_EMAIL),
            @Claim(key = "name", value = "Card Player")
    })
    void playerCompletesCard() {
        given()
                .when().post("/api/v1/cards/" + cardId + "/complete")
                .then().statusCode(200)
                .body("status", is("COMPLETED"));

        // XP/gold are only awarded on verification, not on completion.
        given()
                .when().get("/api/v1/users/" + playerId)
                .then().statusCode(200)
                .body("teams.find { it.teamId == '" + teamId + "' }.currentXp", is(0))
                .body("teams.find { it.teamId == '" + teamId + "' }.gold", is(0));
    }

    @Test
    @Order(10)
    @TestSecurity(user = PLAYER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = PLAYER_SUB),
            @Claim(key = "email", value = PLAYER_EMAIL),
            @Claim(key = "name", value = "Card Player")
    })
    void nonAdminCannotVerify() {
        given()
                .when().post("/api/v1/cards/" + cardId + "/verify")
                .then().statusCode(403);
    }

    @Test
    @Order(11)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Card Admin")
    })
    void adminVerifiesCardAndAwardsRewards() {
        given()
                .when().post("/api/v1/cards/" + cardId + "/verify")
                .then().statusCode(200)
                .body("cardCompleted.status", is("VERIFIED"))
                .body("xpGained", is(100))
                .body("goldGained", is(10))
                .body("leveledUp", is(false))
                .body("newLevel", is(1));

        given()
                .when().get("/api/v1/users/" + playerId)
                .then().statusCode(200)
                .body("teams.find { it.teamId == '" + teamId + "' }.currentXp", is(100))
                .body("teams.find { it.teamId == '" + teamId + "' }.gold", is(10))
                .body("teams.find { it.teamId == '" + teamId + "' }.level", is(1));
    }

    @Test
    @Order(12)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Card Admin")
    })
    void adminCreatesBigCardForLevelUpTest() {
        // Player currently sits at level 1 / 100 xp. A 950 xp reward crosses
        // the level-1 threshold (1000) with 50 xp overflowing into level 2.
        bigCardId = given()
                .contentType("application/json")
                .body(cardPayload(950, 5))
                .when().post("/api/v1/cards")
                .then().statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(13)
    @TestSecurity(user = PLAYER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = PLAYER_SUB),
            @Claim(key = "email", value = PLAYER_EMAIL),
            @Claim(key = "name", value = "Card Player")
    })
    void playerClaimsAndCompletesBigCard() {
        given().when().post("/api/v1/cards/" + bigCardId + "/claim").then().statusCode(200);
        given().when().post("/api/v1/cards/" + bigCardId + "/complete").then().statusCode(200);
    }

    @Test
    @Order(14)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Card Admin")
    })
    void verifyingBigCardCrossesLevelThreshold() {
        given()
                .when().post("/api/v1/cards/" + bigCardId + "/verify")
                .then().statusCode(200)
                .body("leveledUp", is(true))
                .body("newLevel", is(2));

        given()
                .when().get("/api/v1/users/" + playerId)
                .then().statusCode(200)
                .body("teams.find { it.teamId == '" + teamId + "' }.level", is(2))
                .body("teams.find { it.teamId == '" + teamId + "' }.currentXp", is(50))
                .body("teams.find { it.teamId == '" + teamId + "' }.gold", is(15));
    }

    @Test
    @Order(15)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Card Admin")
    })
    void adminSelfVerifiesOwnCompletedCard() {
        String selfCardId = given()
                .contentType("application/json")
                .body(cardPayload(10, 1))
                .when().post("/api/v1/cards")
                .then().statusCode(201)
                .extract().path("id");

        given().when().post("/api/v1/cards/" + selfCardId + "/claim").then().statusCode(200);
        given().when().post("/api/v1/cards/" + selfCardId + "/complete").then().statusCode(200);
        given()
                .when().post("/api/v1/cards/" + selfCardId + "/verify")
                .then().statusCode(200)
                .body("cardCompleted.status", is("VERIFIED"));
    }

    @Test
    @Order(16)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Card Admin")
    })
    void teamCardsCanBeFilteredByMultipleStatuses() {
        // No status param defaults to AVAILABLE only.
        given()
                .when().get("/api/v1/teams/" + teamId + "/cards")
                .then().statusCode(200)
                .body("findAll { it.status != 'AVAILABLE' }", org.hamcrest.Matchers.empty());

        given()
                .when().get("/api/v1/teams/" + teamId + "/cards?status=COMPLETED,VERIFIED")
                .then().statusCode(200)
                .body("find { it.id == '" + cardId + "' }.status", is("VERIFIED"))
                .body("find { it.id == '" + bigCardId + "' }.status", is("VERIFIED"))
                .body("findAll { it.status == 'AVAILABLE' }", org.hamcrest.Matchers.empty());
    }

    @Test
    @Order(17)
    @TestSecurity(user = PLAYER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = PLAYER_SUB),
            @Claim(key = "email", value = PLAYER_EMAIL),
            @Claim(key = "name", value = "Card Player")
    })
    void nonMemberCannotListTeamCards() {
        String outsideTeamId = "00000000-0000-0000-0000-000000000000";
        given()
                .when().get("/api/v1/teams/" + outsideTeamId + "/cards")
                .then().statusCode(403);
    }

    private static String cardPayload(int xpReward, int goldReward) {
        return "{"
                + "\"teamId\":\"" + teamId + "\","
                + "\"title\":\"Wash the dishes\","
                + "\"description\":\"Every last plate\","
                + "\"rarity\":\"COMMON\","
                + "\"complexity\":3,"
                + "\"estimatedMinutes\":15,"
                + "\"xpReward\":" + xpReward + ","
                + "\"goldReward\":" + goldReward
                + "}";
    }
}
