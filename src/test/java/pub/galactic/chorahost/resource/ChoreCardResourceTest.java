package pub.galactic.chorahost.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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

    private static String playerInviteCode;
    private static String teamId;
    private static String playerId;
    private static String cardId;
    private static String bigCardId;
    private static String completedCardId;
    private static String dueDateCardId;
    private static String unclaimCardId;
    private static String userImageCardId;

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

        playerInviteCode = given()
                .contentType("application/json")
                .body("{\"email\":\"" + PLAYER_EMAIL + "\"}")
                .when().post("/api/v1/teams/" + teamId + "/invitations")
                .then().statusCode(201)
                .extract().path("code");
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

        given()
                .contentType("application/json")
                .body("{\"code\":\"" + playerInviteCode + "\"}")
                .when().post("/api/v1/invitations/accept")
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
                .body("items.find { it.id == '" + cardId + "' }.status", is("AVAILABLE"));
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
                .body("items.findAll { it.status != 'AVAILABLE' }", org.hamcrest.Matchers.empty());

        given()
                .when().get("/api/v1/teams/" + teamId + "/cards?status=COMPLETED,VERIFIED")
                .then().statusCode(200)
                .body("items.find { it.id == '" + cardId + "' }.status", is("VERIFIED"))
                .body("items.find { it.id == '" + bigCardId + "' }.status", is("VERIFIED"))
                .body("items.findAll { it.status == 'AVAILABLE' }", org.hamcrest.Matchers.empty());
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

    @Test
    @Order(18)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Card Admin")
    })
    void onlyAssigneeCanAcknowledgeReward() {
        given()
                .when().post("/api/v1/cards/" + cardId + "/acknowledge-reward")
                .then().statusCode(403);
    }

    @Test
    @Order(19)
    @TestSecurity(user = PLAYER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = PLAYER_SUB),
            @Claim(key = "email", value = PLAYER_EMAIL),
            @Claim(key = "name", value = "Card Player")
    })
    void assigneeAcknowledgesRewardIdempotently() {
        // cardId is VERIFIED at this point (order 11).
        String firstAcknowledgedAt = given()
                .when().post("/api/v1/cards/" + cardId + "/acknowledge-reward")
                .then().statusCode(200)
                .body("id", is(cardId))
                .body("rewardAcknowledgedAt", notNullValue())
                .extract().path("rewardAcknowledgedAt");

        // Calling it again must not overwrite the timestamp (compare as
        // Instants since Postgres round-trips with microsecond, not
        // nanosecond, precision).
        String secondAcknowledgedAt = given()
                .when().post("/api/v1/cards/" + cardId + "/acknowledge-reward")
                .then().statusCode(200)
                .extract().path("rewardAcknowledgedAt");
        // Postgres round-trips at microsecond precision (vs. Java's
        // nanosecond Instant.now()), so compare within a small tolerance
        // rather than exact equality; a genuinely re-set timestamp would be
        // visibly later than that.
        java.time.Duration drift = java.time.Duration.between(
                java.time.Instant.parse(firstAcknowledgedAt), java.time.Instant.parse(secondAcknowledgedAt)).abs();
        org.assertj.core.api.Assertions.assertThat(drift).isLessThan(java.time.Duration.ofMillis(1));
    }

    @Test
    @Order(20)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Card Admin")
    })
    void adminCreatesCompletedButUnverifiedCard() {
        completedCardId = given()
                .contentType("application/json")
                .body(cardPayload(10, 1))
                .when().post("/api/v1/cards")
                .then().statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(21)
    @TestSecurity(user = PLAYER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = PLAYER_SUB),
            @Claim(key = "email", value = PLAYER_EMAIL),
            @Claim(key = "name", value = "Card Player")
    })
    void playerClaimsAndCompletesCardWithoutVerification() {
        given().when().post("/api/v1/cards/" + completedCardId + "/claim").then().statusCode(200);
        given().when().post("/api/v1/cards/" + completedCardId + "/complete").then().statusCode(200);
    }

    @Test
    @Order(22)
    @TestSecurity(user = PLAYER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = PLAYER_SUB),
            @Claim(key = "email", value = PLAYER_EMAIL),
            @Claim(key = "name", value = "Card Player")
    })
    void nonVerifiedCardCannotAcknowledgeReward() {
        given()
                .when().post("/api/v1/cards/" + completedCardId + "/acknowledge-reward")
                .then().statusCode(409);
    }

    @Test
    @Order(23)
    @TestSecurity(user = PLAYER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = PLAYER_SUB),
            @Claim(key = "email", value = PLAYER_EMAIL),
            @Claim(key = "name", value = "Card Player")
    })
    void nonAdminCannotEditCard() {
        given()
                .contentType("application/json")
                .body(cardPayload(999, 999))
                .when().put("/api/v1/cards/" + completedCardId)
                .then().statusCode(403);
    }

    @Test
    @Order(24)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Card Admin")
    })
    void adminEditsAvailableCard() {
        String editedTitle = given()
                .contentType("application/json")
                .body(editedCardPayload("Wash the dishes (updated)", 20, 2))
                .when().put("/api/v1/cards/" + bigCardId)
                .then().statusCode(200)
                .body("title", is("Wash the dishes (updated)"))
                .body("xpReward", is(20))
                .body("goldReward", is(2))
                .extract().path("title");

        org.assertj.core.api.Assertions.assertThat(editedTitle).isEqualTo("Wash the dishes (updated)");
    }

    @Test
    @Order(25)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Card Admin")
    })
    void adminEditsVerifiedCard() {
        // cardId is VERIFIED at this point (order 11); editing must still be
        // allowed regardless of status.
        given()
                .contentType("application/json")
                .body(editedCardPayload("Wash the dishes (relabeled)", 30, 3))
                .when().put("/api/v1/cards/" + cardId)
                .then().statusCode(200)
                .body("title", is("Wash the dishes (relabeled)"))
                .body("status", is("VERIFIED"))
                .body("xpReward", is(30))
                .body("goldReward", is(3));
    }

    @Test
    @Order(26)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Card Admin")
    })
    void editingAMissingCardReturns404() {
        String missingCardId = "00000000-0000-0000-0000-000000000000";
        given()
                .contentType("application/json")
                .body(editedCardPayload("Nope", 1, 1))
                .when().put("/api/v1/cards/" + missingCardId)
                .then().statusCode(404);
    }

    @Test
    @Order(27)
    @TestSecurity(user = PLAYER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = PLAYER_SUB),
            @Claim(key = "email", value = PLAYER_EMAIL),
            @Claim(key = "name", value = "Card Player")
    })
    void nonAdminCannotDeleteCard() {
        given()
                .when().delete("/api/v1/cards/" + completedCardId)
                .then().statusCode(403);
    }

    @Test
    @Order(28)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Card Admin")
    })
    void adminDeletesVerifiedCard() {
        // cardId is VERIFIED at this point (order 11); deletion must be
        // allowed regardless of status.
        given()
                .when().delete("/api/v1/cards/" + cardId)
                .then().statusCode(204);

        given()
                .when().get("/api/v1/teams/" + teamId + "/cards?status=VERIFIED,COMPLETED,CLAIMED,AVAILABLE")
                .then().statusCode(200)
                .body("items.find { it.id == '" + cardId + "' }", org.hamcrest.Matchers.nullValue());
    }

    @Test
    @Order(29)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Card Admin")
    })
    void deletingAMissingCardReturns404() {
        String missingCardId = "00000000-0000-0000-0000-000000000000";
        given()
                .when().delete("/api/v1/cards/" + missingCardId)
                .then().statusCode(404);
    }

    @Test
    @Order(30)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Card Admin")
    })
    void adminCreatesCardWithDueDateInterval() {
        dueDateCardId = given()
                .contentType("application/json")
                .body(cardPayloadWithDueDateInterval(3))
                .when().post("/api/v1/cards")
                .then().statusCode(201)
                .body("dueDateIntervalHours", is(3))
                .body("dueAt", nullValue())
                .extract().path("id");
    }

    @Test
    @Order(31)
    @TestSecurity(user = PLAYER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = PLAYER_SUB),
            @Claim(key = "email", value = PLAYER_EMAIL),
            @Claim(key = "name", value = "Card Player")
    })
    void claimingCardWithDueDateIntervalSetsDueAt() {
        String claimedAt = given()
                .when().post("/api/v1/cards/" + dueDateCardId + "/claim")
                .then().statusCode(200)
                .body("status", is("CLAIMED"))
                .body("dueAt", notNullValue())
                .extract().path("claimedAt");
        String dueAt = given()
                .when().get("/api/v1/users/" + playerId + "/hand")
                .then().statusCode(200)
                .extract().path("find { it.id == '" + dueDateCardId + "' }.dueAt");

        // Postgres round-trips at microsecond precision (vs. Java's nanosecond
        // Instant.now()), so compare within a small tolerance rather than
        // exact equality.
        java.time.Duration gap = java.time.Duration.between(
                java.time.Instant.parse(claimedAt), java.time.Instant.parse(dueAt));
        org.assertj.core.api.Assertions.assertThat(gap.minus(java.time.Duration.ofHours(3)).abs())
                .isLessThan(java.time.Duration.ofMillis(1));
    }

    @Test
    @Order(32)
    @TestSecurity(user = PLAYER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = PLAYER_SUB),
            @Claim(key = "email", value = PLAYER_EMAIL),
            @Claim(key = "name", value = "Card Player")
    })
    void completingCardClearsDueAt() {
        given()
                .when().post("/api/v1/cards/" + dueDateCardId + "/complete")
                .then().statusCode(200)
                .body("status", is("COMPLETED"))
                .body("dueAt", nullValue());
    }

    @Test
    @Order(33)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Card Admin")
    })
    void adminCreatesCardForUnclaimTest() {
        unclaimCardId = given()
                .contentType("application/json")
                .body(cardPayload(50, 5))
                .when().post("/api/v1/cards")
                .then().statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(34)
    @TestSecurity(user = PLAYER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = PLAYER_SUB),
            @Claim(key = "email", value = PLAYER_EMAIL),
            @Claim(key = "name", value = "Card Player")
    })
    void unclaimingAnAvailableCardConflicts() {
        given()
                .when().post("/api/v1/cards/" + unclaimCardId + "/unclaim")
                .then().statusCode(409);
    }

    @Test
    @Order(35)
    @TestSecurity(user = PLAYER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = PLAYER_SUB),
            @Claim(key = "email", value = PLAYER_EMAIL),
            @Claim(key = "name", value = "Card Player")
    })
    void playerClaimsCardForUnclaimTest() {
        given()
                .when().post("/api/v1/cards/" + unclaimCardId + "/claim")
                .then().statusCode(200)
                .body("status", is("CLAIMED"))
                .body("assignedUserId", is(playerId));
    }

    @Test
    @Order(36)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Card Admin")
    })
    void onlyTheAssigneeCanUnclaimTheCard() {
        given()
                .when().post("/api/v1/cards/" + unclaimCardId + "/unclaim")
                .then().statusCode(403);
    }

    @Test
    @Order(37)
    @TestSecurity(user = PLAYER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = PLAYER_SUB),
            @Claim(key = "email", value = PLAYER_EMAIL),
            @Claim(key = "name", value = "Card Player")
    })
    void assigneeUnclaimsCard() {
        given()
                .when().post("/api/v1/cards/" + unclaimCardId + "/unclaim")
                .then().statusCode(200)
                .body("status", is("AVAILABLE"))
                .body("assignedUserId", nullValue())
                .body("claimedAt", nullValue())
                .body("dueAt", nullValue());

        given()
                .when().get("/api/v1/users/" + playerId + "/hand")
                .then().statusCode(200)
                .body("find { it.id == '" + unclaimCardId + "' }", nullValue());
    }

    @Test
    @Order(38)
    @TestSecurity(user = PLAYER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = PLAYER_SUB),
            @Claim(key = "email", value = PLAYER_EMAIL),
            @Claim(key = "name", value = "Card Player")
    })
    void unclaimingAnAlreadyAvailableCardConflicts() {
        given()
                .when().post("/api/v1/cards/" + unclaimCardId + "/unclaim")
                .then().statusCode(409);
    }

    @Test
    @Order(39)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Card Admin")
    })
    void adminCreatesCardWithUserImage() {
        String fallback = "https://example.com/fallback.png";
        String custom = "https://example.com/player.png";
        String body = cardPayload(5, 1).replaceFirst("}$",
                ",\"imageUrl\":\"" + fallback + "\",\"userImageUrls\":{\"" + playerId + "\":\"" + custom + "\"}}");
        userImageCardId = given()
                .contentType("application/json")
                .body(body)
                .when().post("/api/v1/cards")
                .then().statusCode(201)
                .body("imageUrl", is(fallback))
                .body("resolvedImageUrl", is(fallback))
                .body("userImageUrls." + playerId, is(custom))
                .extract().path("id");
    }

    @Test
    @Order(40)
    @TestSecurity(user = PLAYER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = PLAYER_SUB),
            @Claim(key = "email", value = PLAYER_EMAIL),
            @Claim(key = "name", value = "Card Player")
    })
    void claimingSwitchesToAssigneeImage() {
        given()
                .when().post("/api/v1/cards/" + userImageCardId + "/claim")
                .then().statusCode(200)
                .body("resolvedImageUrl", is("https://example.com/player.png"))
                .body("imageUrl", is("https://example.com/fallback.png"));

        given()
                .when().post("/api/v1/cards/" + userImageCardId + "/unclaim")
                .then().statusCode(200)
                // Unassigned again, but the logged in player still has their own image.
                .body("resolvedImageUrl", is("https://example.com/player.png"));
    }

    @Test
    @Order(41)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Card Admin")
    })
    void nonMemberImageRejected() {
        // Unassigned and the admin has no override, so the fallback applies.
        given()
                .when().get("/api/v1/teams/" + teamId + "/cards")
                .then().statusCode(200)
                .body("items.find { it.id == '" + userImageCardId + "' }.resolvedImageUrl",
                        is("https://example.com/fallback.png"));

        String body = cardPayload(5, 1).replaceFirst("}$",
                ",\"userImageUrls\":{\"" + java.util.UUID.randomUUID() + "\":\"https://example.com/x.png\"}}");
        given()
                .contentType("application/json")
                .body(body)
                .when().post("/api/v1/cards")
                .then().statusCode(400);
    }

    @Test
    @Order(42)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Card Admin")
    })
    void updateReplacesAndClearsUserImages() {
        String url = "https://example.com/replaced.png";
        String update = editedCardPayload("Wash the dishes", 5, 1).replaceFirst("}$",
                ",\"imageUrl\":\"https://example.com/fallback.png\",\"userImageUrls\":{\"" + playerId + "\":\"" + url + "\"}}");
        given()
                .contentType("application/json")
                .body(update)
                .when().put("/api/v1/cards/" + userImageCardId)
                .then().statusCode(200)
                .body("userImageUrls." + playerId, is(url));

        given()
                .contentType("application/json")
                .body(editedCardPayload("Wash the dishes", 5, 1))
                .when().put("/api/v1/cards/" + userImageCardId)
                .then().statusCode(200)
                .body("userImageUrls.size()", is(0))
                .body("resolvedImageUrl", org.hamcrest.Matchers.nullValue());
    }

    @Test
    @Order(43)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Card Admin")
    })
    void nonHttpUserImageUrlIsRejected() {
        String body = cardPayload(5, 1).replaceFirst("}$",
                ",\"userImageUrls\":{\"" + playerId + "\":\"javascript:alert(1)\"}}");
        given()
                .contentType("application/json")
                .body(body)
                .when().post("/api/v1/cards")
                .then().statusCode(400);
    }

    private static String editedCardPayload(String title, int xpReward, int goldReward) {
        return "{"
                + "\"title\":\"" + title + "\","
                + "\"description\":\"Every last plate\","
                + "\"rarity\":\"COMMON\","
                + "\"complexity\":3,"
                + "\"estimatedMinutes\":15,"
                + "\"xpReward\":" + xpReward + ","
                + "\"goldReward\":" + goldReward
                + "}";
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

    private static String cardPayloadWithDueDateInterval(int dueDateIntervalHours) {
        return "{"
                + "\"teamId\":\"" + teamId + "\","
                + "\"title\":\"Water the garden\","
                + "\"description\":\"Before it wilts\","
                + "\"rarity\":\"COMMON\","
                + "\"complexity\":2,"
                + "\"estimatedMinutes\":10,"
                + "\"xpReward\":5,"
                + "\"goldReward\":1,"
                + "\"dueDateIntervalHours\":" + dueDateIntervalHours
                + "}";
    }
}
