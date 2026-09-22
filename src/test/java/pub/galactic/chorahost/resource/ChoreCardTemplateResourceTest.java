package pub.galactic.chorahost.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Repeating-chore templates: a team admin defines a template once, then
 * instantiates it into fresh AVAILABLE cards without re-entering the chore.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChoreCardTemplateResourceTest extends AbstractResourceTest {

    private static final String ADMIN_SUB = "template-admin-sub";
    private static final String ADMIN_EMAIL = "template-admin@chorahost.test";
    private static final String OUTSIDER_SUB = "not-a-team-member-sub";
    private static final String OUTSIDER_EMAIL = "outsider@chorahost.test";

    private static String teamId;
    private static String templateId;

    @Test
    @Order(1)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Template Admin")
    })
    void setUpTeam() {
        teamId = given()
                .contentType("application/json")
                .body("{\"name\":\"Template Test Team\"}")
                .when().post("/api/v1/teams")
                .then().statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(2)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Template Admin")
    })
    void adminCreatesTemplate() {
        templateId = given()
                .contentType("application/json")
                .body("{"
                        + "\"teamId\":\"" + teamId + "\","
                        + "\"title\":\"Take out the trash\","
                        + "\"description\":\"Weekly bins\","
                        + "\"rarity\":\"COMMON\","
                        + "\"complexity\":2,"
                        + "\"estimatedMinutes\":10,"
                        + "\"xpReward\":50,"
                        + "\"goldReward\":5"
                        + "}")
                .when().post("/api/v1/card-templates")
                .then().statusCode(201)
                .body("title", is("Take out the trash"))
                .extract().path("id");

        given()
                .when().get("/api/v1/teams/" + teamId + "/card-templates")
                .then().statusCode(200)
                .body("find { it.id == '" + templateId + "' }.title", is("Take out the trash"));
    }

    @Test
    @Order(3)
    @TestSecurity(user = OUTSIDER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = OUTSIDER_SUB),
            @Claim(key = "email", value = OUTSIDER_EMAIL),
            @Claim(key = "name", value = "Outsider")
    })
    void nonMemberCannotCreateTemplate() {
        given()
                .contentType("application/json")
                .body("{"
                        + "\"teamId\":\"" + teamId + "\","
                        + "\"title\":\"Sneaky template\","
                        + "\"rarity\":\"COMMON\","
                        + "\"complexity\":1,"
                        + "\"estimatedMinutes\":5,"
                        + "\"xpReward\":10,"
                        + "\"goldReward\":1"
                        + "}")
                .when().post("/api/v1/card-templates")
                .then().statusCode(403);
    }

    @Test
    @Order(4)
    @TestSecurity(user = OUTSIDER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = OUTSIDER_SUB),
            @Claim(key = "email", value = OUTSIDER_EMAIL),
            @Claim(key = "name", value = "Outsider")
    })
    void nonMemberCannotEditTemplate() {
        given()
                .contentType("application/json")
                .body(editedTemplatePayload("Sneaky edit", 999, 999))
                .when().put("/api/v1/card-templates/" + templateId)
                .then().statusCode(403);
    }

    @Test
    @Order(5)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Template Admin")
    })
    void adminEditsTemplate() {
        given()
                .contentType("application/json")
                .body(editedTemplatePayload("Take out the trash (updated)", 75, 8))
                .when().put("/api/v1/card-templates/" + templateId)
                .then().statusCode(200)
                .body("title", is("Take out the trash (updated)"))
                .body("xpReward", is(75))
                .body("goldReward", is(8));

        given()
                .when().get("/api/v1/teams/" + teamId + "/card-templates")
                .then().statusCode(200)
                .body("find { it.id == '" + templateId + "' }.title", is("Take out the trash (updated)"));
    }

    @Test
    @Order(6)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Template Admin")
    })
    void editingAMissingTemplateReturns404() {
        String missingTemplateId = "00000000-0000-0000-0000-000000000000";
        given()
                .contentType("application/json")
                .body(editedTemplatePayload("Nope", 1, 1))
                .when().put("/api/v1/card-templates/" + missingTemplateId)
                .then().statusCode(404);
    }

    private static String editedTemplatePayload(String title, int xpReward, int goldReward) {
        return "{"
                + "\"title\":\"" + title + "\","
                + "\"description\":\"Weekly bins\","
                + "\"rarity\":\"COMMON\","
                + "\"complexity\":2,"
                + "\"estimatedMinutes\":10,"
                + "\"xpReward\":" + xpReward + ","
                + "\"goldReward\":" + goldReward
                + "}";
    }

    private static void assertNotBlank(String value) {
        if (value == null || value.isBlank()) {
            throw new AssertionError("Expected a non-blank id");
        }
    }
}
