package pub.galactic.chorahost.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
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
 * GET /users/{id} is only allowed between users who share at least one team,
 * and the response is scoped to just the shared subset of memberships - a
 * user on multiple teams together with the requester should not leak their
 * membership in teams the requester isn't part of.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserResourceTest extends AbstractResourceTest {

    private static final String OWNER_SUB = "user-lookup-owner-sub";
    private static final String OWNER_EMAIL = "lookup-owner@chorahost.test";
    private static final String OUTSIDER_SUB = "user-lookup-outsider-sub";
    private static final String OUTSIDER_EMAIL = "lookup-outsider@chorahost.test";
    private static final String SHARED_SUB = "user-lookup-shared-sub";
    private static final String SHARED_EMAIL = "lookup-shared@chorahost.test";

    private static String sharedInviteCode;
    private static String sharedTeamId;
    private static String otherTeamId;
    private static String ownerId;
    private static String sharedUserId;

    @Test
    @Order(1)
    @TestSecurity(user = OWNER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = OWNER_SUB),
            @Claim(key = "email", value = OWNER_EMAIL),
            @Claim(key = "name", value = "Lookup Owner")
    })
    void setUpTeams() {
        ownerId = given()
                .when().get("/api/v1/users/me")
                .then().statusCode(200)
                .extract().path("id");

        sharedTeamId = given()
                .contentType("application/json")
                .body("{\"name\":\"Shared Team\"}")
                .when().post("/api/v1/teams")
                .then().statusCode(201)
                .extract().path("id");

        sharedInviteCode = given()
                .contentType("application/json")
                .body("{\"email\":\"" + SHARED_EMAIL + "\"}")
                .when().post("/api/v1/teams/" + sharedTeamId + "/invitations")
                .then().statusCode(201)
                .extract().path("code");

        otherTeamId = given()
                .contentType("application/json")
                .body("{\"name\":\"Owner Only Team\"}")
                .when().post("/api/v1/teams")
                .then().statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(2)
    @TestSecurity(user = SHARED_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = SHARED_SUB),
            @Claim(key = "email", value = SHARED_EMAIL),
            @Claim(key = "name", value = "Lookup Shared")
    })
    void sharedUserJoinsOnlyTheSharedTeam() {
        sharedUserId = given()
                .when().get("/api/v1/users/me")
                .then().statusCode(200)
                .extract().path("id");

        given()
                .contentType("application/json")
                .body("{\"code\":\"" + sharedInviteCode + "\"}")
                .when().post("/api/v1/invitations/accept")
                .then().statusCode(200);
    }

    @Test
    @Order(3)
    @TestSecurity(user = OUTSIDER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = OUTSIDER_SUB),
            @Claim(key = "email", value = OUTSIDER_EMAIL),
            @Claim(key = "name", value = "Lookup Outsider")
    })
    void userWithNoSharedTeamCannotLookUpOwner() {
        // Provisions the outsider's own user record; they never join a team.
        given().when().get("/api/v1/users/me").then().statusCode(200);

        given()
                .when().get("/api/v1/users/" + ownerId)
                .then().statusCode(403);
    }

    @Test
    @Order(4)
    @TestSecurity(user = OWNER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = OWNER_SUB),
            @Claim(key = "email", value = OWNER_EMAIL),
            @Claim(key = "name", value = "Lookup Owner")
    })
    void ownerLookingUpSharedUserSeesOnlyTheSharedTeam() {
        given()
                .when().get("/api/v1/users/" + sharedUserId)
                .then().statusCode(200)
                .body("teams", hasSize(1))
                .body("teams[0].teamId", is(sharedTeamId));
    }
}
