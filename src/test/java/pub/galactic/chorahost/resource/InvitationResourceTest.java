package pub.galactic.chorahost.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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
 * Invitation-based joining: who may invite, that codes are bound to the
 * invited email, and that revoked/replaced invitations stop working.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InvitationResourceTest extends AbstractResourceTest {

    private static final String OWNER_SUB = "invite-owner-sub";
    private static final String OWNER_EMAIL = "invite-owner@chorahost.test";
    private static final String GUEST_SUB = "invite-guest-sub";
    private static final String GUEST_EMAIL = "invite-guest@chorahost.test";
    private static final String OTHER_SUB = "invite-other-sub";
    private static final String OTHER_EMAIL = "invite-other@chorahost.test";

    private static String teamId;
    private static String firstInvitationId;
    private static String firstCode;
    private static String secondCode;

    @Test
    @Order(1)
    @TestSecurity(user = OWNER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = OWNER_SUB),
            @Claim(key = "email", value = OWNER_EMAIL),
            @Claim(key = "name", value = "Invite Owner")
    })
    void teamAdminInvitesByEmail() {
        teamId = given()
                .contentType("application/json")
                .body("{\"name\":\"Invite Team\"}")
                .when().post("/api/v1/teams")
                .then().statusCode(201)
                .extract().path("id");

        var response = given()
                .contentType("application/json")
                .body("{\"email\":\"" + GUEST_EMAIL.toUpperCase() + "\"}")
                .when().post("/api/v1/teams/" + teamId + "/invitations")
                .then().statusCode(201)
                .body("email", is(GUEST_EMAIL))
                .body("status", is("PENDING"))
                .body("code", not(nullValue()))
                .extract();
        firstInvitationId = response.path("id");
        firstCode = response.path("code");

        given()
                .when().get("/api/v1/teams/" + teamId + "/invitations")
                .then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].code", nullValue());
    }

    @Test
    @Order(2)
    @TestSecurity(user = GUEST_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = GUEST_SUB),
            @Claim(key = "email", value = GUEST_EMAIL),
            @Claim(key = "name", value = "Invite Guest")
    })
    void nonAdminCannotInviteOrListInvitations() {
        given()
                .contentType("application/json")
                .body("{\"email\":\"someone@chorahost.test\"}")
                .when().post("/api/v1/teams/" + teamId + "/invitations")
                .then().statusCode(403);

        given()
                .when().get("/api/v1/teams/" + teamId + "/invitations")
                .then().statusCode(403);
    }

    @Test
    @Order(3)
    @TestSecurity(user = OTHER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = OTHER_SUB),
            @Claim(key = "email", value = OTHER_EMAIL),
            @Claim(key = "name", value = "Invite Other")
    })
    void codeCannotBeUsedByADifferentEmail() {
        given()
                .contentType("application/json")
                .body("{\"code\":\"" + firstCode + "\"}")
                .when().post("/api/v1/invitations/accept")
                .then().statusCode(404);

        given()
                .contentType("application/json")
                .when().post("/api/v1/invitations/" + firstInvitationId + "/accept")
                .then().statusCode(404);

        given()
                .when().get("/api/v1/invitations")
                .then().statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    @Order(4)
    @TestSecurity(user = OWNER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = OWNER_SUB),
            @Claim(key = "email", value = OWNER_EMAIL),
            @Claim(key = "name", value = "Invite Owner")
    })
    void reInvitingRevokesTheEarlierCode() {
        secondCode = given()
                .contentType("application/json")
                .body("{\"email\":\"" + GUEST_EMAIL + "\"}")
                .when().post("/api/v1/teams/" + teamId + "/invitations")
                .then().statusCode(201)
                .extract().path("code");

        given()
                .when().get("/api/v1/teams/" + teamId + "/invitations")
                .then().statusCode(200)
                .body("$", hasSize(1));
    }

    @Test
    @Order(5)
    @TestSecurity(user = GUEST_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = GUEST_SUB),
            @Claim(key = "email", value = GUEST_EMAIL),
            @Claim(key = "name", value = "Invite Guest")
    })
    void revokedCodeFailsAndCurrentCodeWorks() {
        given()
                .contentType("application/json")
                .body("{\"code\":\"" + firstCode + "\"}")
                .when().post("/api/v1/invitations/accept")
                .then().statusCode(404);

        given()
                .when().get("/api/v1/invitations")
                .then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].teamId", is(teamId))
                .body("[0].code", nullValue());

        given()
                .contentType("application/json")
                .body("{\"code\":\"" + secondCode + "\"}")
                .when().post("/api/v1/invitations/accept")
                .then().statusCode(200)
                .body("id", is(teamId));

        given()
                .when().get("/api/v1/users/me")
                .then().statusCode(200)
                .body("teams.find { it.teamId == '" + teamId + "' }.role", is("USER"));
    }

    @Test
    @Order(6)
    @TestSecurity(user = OWNER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = OWNER_SUB),
            @Claim(key = "email", value = OWNER_EMAIL),
            @Claim(key = "name", value = "Invite Owner")
    })
    void invitingExistingMemberIsRejectedAndPendingInvitationCanBeRevoked() {
        given()
                .contentType("application/json")
                .body("{\"email\":\"" + GUEST_EMAIL + "\"}")
                .when().post("/api/v1/teams/" + teamId + "/invitations")
                .then().statusCode(409);

        String id = given()
                .contentType("application/json")
                .body("{\"email\":\"" + OTHER_EMAIL + "\"}")
                .when().post("/api/v1/teams/" + teamId + "/invitations")
                .then().statusCode(201)
                .extract().path("id");

        given()
                .when().delete("/api/v1/teams/" + teamId + "/invitations/" + id)
                .then().statusCode(204);

        given()
                .when().get("/api/v1/teams/" + teamId + "/invitations")
                .then().statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    @Order(7)
    @TestSecurity(user = OTHER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = OTHER_SUB),
            @Claim(key = "email", value = OTHER_EMAIL),
            @Claim(key = "name", value = "Invite Other")
    })
    void uninvitedUserCannotJoinByTeamId() {
        given()
                .contentType("application/json")
                .body("{\"email\":\"" + OTHER_EMAIL + "\"}")
                .when().post("/api/v1/team/" + teamId + "/join")
                .then().statusCode(404);
    }
}
