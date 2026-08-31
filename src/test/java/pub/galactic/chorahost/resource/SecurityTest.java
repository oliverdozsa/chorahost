package pub.galactic.chorahost.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SecurityTest {

    @Test
    void unauthenticatedRequestToApiIsRejected() {
        given()
                .when().get("/api/v1/teams/" + UUID.randomUUID() + "/leaderboard")
                .then().statusCode(401);
    }

    @Test
    void unauthenticatedCardClaimIsRejected() {
        given()
                .when().post("/api/v1/cards/" + UUID.randomUUID() + "/claim")
                .then().statusCode(401);
    }

    @Test
    void healthEndpointDoesNotRequireAuth() {
        given()
                .when().get("/q/health")
                .then().statusCode(200)
                .body("status", is("UP"));
    }
}
