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
 * End-to-end team lifecycle: create -> join -> role assignment -> leaderboard.
 * Also covers that a user can belong to more than one team at once, with an
 * independent role per team.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TeamResourceTest {

    private static final String OWNER_SUB = "team-owner-sub";
    private static final String OWNER_EMAIL = "owner@chorahost.test";
    private static final String MEMBER_SUB = "team-member-sub";
    private static final String MEMBER_EMAIL = "member@chorahost.test";

    private static String teamId;
    private static String secondTeamId;
    private static String memberId;

    @Test
    @Order(1)
    @TestSecurity(user = OWNER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = OWNER_SUB),
            @Claim(key = "email", value = OWNER_EMAIL),
            @Claim(key = "name", value = "Team Owner")
    })
    void createTeam_makesCreatorTeamAdmin() {
        teamId = given()
                .contentType("application/json")
                .body("{\"name\":\"The Chore Crusaders\"}")
                .when().post("/api/v1/teams")
                .then().statusCode(201)
                .body("name", is("The Chore Crusaders"))
                .extract().path("id");

        given()
                .when().get("/api/v1/users/me")
                .then().statusCode(200)
                .body("teams.find { it.teamId == '" + teamId + "' }.role", is("TEAM_ADMIN"));
    }

    @Test
    @Order(2)
    @TestSecurity(user = MEMBER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = MEMBER_SUB),
            @Claim(key = "email", value = MEMBER_EMAIL),
            @Claim(key = "name", value = "Team Member")
    })
    void memberProvisionsThenJoinsTeamByEmail() {
        // First authenticated call auto-provisions the local user record.
        memberId = given()
                .when().get("/api/v1/users/me")
                .then().statusCode(200)
                .body("teams", hasSize(0))
                .extract().path("id");

        given()
                .contentType("application/json")
                .body("{\"email\":\"" + MEMBER_EMAIL + "\"}")
                .when().post("/api/v1/team/" + teamId + "/join")
                .then().statusCode(200)
                .body("id", is(teamId));

        given()
                .when().get("/api/v1/users/me")
                .then().statusCode(200)
                .body("teams.find { it.teamId == '" + teamId + "' }.role", is("USER"));
    }

    @Test
    @Order(3)
    @TestSecurity(user = MEMBER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = MEMBER_SUB),
            @Claim(key = "email", value = MEMBER_EMAIL),
            @Claim(key = "name", value = "Team Member")
    })
    void joiningTwiceIsRejected() {
        given()
                .contentType("application/json")
                .body("{\"email\":\"" + MEMBER_EMAIL + "\"}")
                .when().post("/api/v1/team/" + teamId + "/join")
                .then().statusCode(409);
    }

    @Test
    @Order(4)
    @TestSecurity(user = MEMBER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = MEMBER_SUB),
            @Claim(key = "email", value = MEMBER_EMAIL),
            @Claim(key = "name", value = "Team Member")
    })
    void nonTeamAdminCannotAssignRoles() {
        given()
                .contentType("application/json")
                .body("{\"userId\":\"" + memberId + "\",\"role\":\"ADMIN\"}")
                .when().post("/api/v1/teams/" + teamId + "/role")
                .then().statusCode(403);
    }

    @Test
    @Order(5)
    @TestSecurity(user = OWNER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = OWNER_SUB),
            @Claim(key = "email", value = OWNER_EMAIL),
            @Claim(key = "name", value = "Team Owner")
    })
    void teamAdminCannotPromoteToTeamAdminViaRoleEndpoint() {
        given()
                .contentType("application/json")
                .body("{\"userId\":\"" + memberId + "\",\"role\":\"TEAM_ADMIN\"}")
                .when().post("/api/v1/teams/" + teamId + "/role")
                .then().statusCode(400);
    }

    @Test
    @Order(6)
    @TestSecurity(user = OWNER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = OWNER_SUB),
            @Claim(key = "email", value = OWNER_EMAIL),
            @Claim(key = "name", value = "Team Owner")
    })
    void teamAdminPromotesMemberToAdmin() {
        given()
                .contentType("application/json")
                .body("{\"userId\":\"" + memberId + "\",\"role\":\"ADMIN\"}")
                .when().post("/api/v1/teams/" + teamId + "/role")
                .then().statusCode(200)
                .body("role", is("ADMIN"));
    }

    @Test
    @Order(7)
    @TestSecurity(user = OWNER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = OWNER_SUB),
            @Claim(key = "email", value = OWNER_EMAIL),
            @Claim(key = "name", value = "Team Owner")
    })
    void leaderboardListsBothTeamMembers() {
        given()
                .when().get("/api/v1/teams/" + teamId + "/leaderboard")
                .then().statusCode(200)
                .body("$", hasSize(2));
    }

    @Test
    @Order(8)
    @TestSecurity(user = MEMBER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = MEMBER_SUB),
            @Claim(key = "email", value = MEMBER_EMAIL),
            @Claim(key = "name", value = "Team Member")
    })
    void userCanJoinASecondTeamWithAnIndependentRole() {
        secondTeamId = given()
                .contentType("application/json")
                .body("{\"name\":\"Second Team\"}")
                .when().post("/api/v1/teams")
                .then().statusCode(201)
                .extract().path("id");

        // Member is TEAM_ADMIN on their own new second team, while remaining
        // just ADMIN (promoted earlier) on the first team.
        given()
                .when().get("/api/v1/users/me")
                .then().statusCode(200)
                .body("teams", hasSize(2))
                .body("teams.find { it.teamId == '" + teamId + "' }.role", is("ADMIN"))
                .body("teams.find { it.teamId == '" + secondTeamId + "' }.role", is("TEAM_ADMIN"));
    }
}
