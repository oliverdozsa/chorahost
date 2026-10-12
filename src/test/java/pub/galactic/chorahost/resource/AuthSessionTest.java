package pub.galactic.chorahost.resource;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;

import java.time.Duration;
import java.time.Instant;

import pub.galactic.chorahost.entity.AuthSession;
import pub.galactic.chorahost.repository.AuthSessionRepository;
import io.quarkus.arc.Arc;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * A session token minted from a Google identity authenticates later requests
 * on its own. Methods without {@code @TestSecurity} hit the real
 * authentication mechanisms.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthSessionTest extends AbstractResourceTest {

    private static final String SUB = "session-user-sub";
    private static final String EMAIL = "session-user@chorahost.test";

    private static String sessionToken;
    private static String userId;

    @Test
    @Order(1)
    @TestSecurity(user = SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = SUB),
            @Claim(key = "email", value = EMAIL),
            @Claim(key = "name", value = "Session User"),
            @Claim(key = "picture", value = "https://example.com/pic.png")
    })
    void googleIdentityCanCreateSession() {
        sessionToken = given()
                .when().post("/api/v1/auth/session")
                .then().statusCode(200)
                .body("token", startsWith("chs_"))
                .body("email", org.hamcrest.Matchers.is(EMAIL))
                .body("name", org.hamcrest.Matchers.is("Session User"))
                .body("picture", org.hamcrest.Matchers.is("https://example.com/pic.png"))
                .extract().path("token");
    }

    @Test
    @Order(2)
    void sessionTokenAuthenticatesRequests() {
        userId = given()
                .header("Authorization", "Bearer " + sessionToken)
                .when().get("/api/v1/users/me")
                .then().statusCode(200)
                .body("email", org.hamcrest.Matchers.is(EMAIL))
                .extract().path("id");
        assertThat(userId).isNotNull();
    }

    @Test
    @Order(3)
    void sessionTokenCannotCreateAnotherSession() {
        given()
                .header("Authorization", "Bearer " + sessionToken)
                .when().post("/api/v1/auth/session")
                .then().statusCode(403);
    }

    @Test
    @Order(4)
    void unknownSessionTokenIsRejected() {
        given()
                .header("Authorization", "Bearer chs_not-a-real-token")
                .when().get("/api/v1/users/me")
                .then().statusCode(401);
    }

    @Test
    @Order(5)
    void expiredSessionIsRejected() {
        setExpiry(Instant.now().minusSeconds(60), Instant.now().minus(Duration.ofDays(31)));
        given()
                .header("Authorization", "Bearer " + sessionToken)
                .when().get("/api/v1/users/me")
                .then().statusCode(401);
    }

    @Test
    @Order(6)
    void usingASessionPastTheRenewIntervalSlidesItsExpiry() {
        Instant staleUse = Instant.now().minus(Duration.ofDays(2));
        setExpiry(Instant.now().plus(Duration.ofDays(5)), staleUse);

        given()
                .header("Authorization", "Bearer " + sessionToken)
                .when().get("/api/v1/users/me")
                .then().statusCode(200);

        AuthSession session = onlySession();
        assertThat(session.lastUsedAt).isAfter(staleUse);
        assertThat(session.expiresAt).isAfter(Instant.now().plus(Duration.ofDays(29)));
    }

    @Test
    @Order(7)
    void recentlyUsedSessionIsNotRewrittenOnEveryRequest() {
        AuthSession before = onlySession();

        given()
                .header("Authorization", "Bearer " + sessionToken)
                .when().get("/api/v1/users/me")
                .then().statusCode(200);

        assertThat(onlySession().lastUsedAt).isEqualTo(before.lastUsedAt);
    }

    @Test
    @Order(8)
    void logoutRevokesTheSession() {
        given()
                .header("Authorization", "Bearer " + sessionToken)
                .when().delete("/api/v1/auth/session")
                .then().statusCode(204);

        given()
                .header("Authorization", "Bearer " + sessionToken)
                .when().get("/api/v1/users/me")
                .then().statusCode(401);
    }

    @Test
    @Order(9)
    void nonSessionBearerTokenIsLeftToOidc() {
        // Not a session token, so it must reach the OIDC mechanism, which rejects the garbage JWT.
        given()
                .header("Authorization", "Bearer eyJhbGciOiJSUzI1NiJ9.e30.invalid")
                .when().get("/api/v1/users/me")
                .then().statusCode(401);
    }

    private static void setExpiry(Instant expiresAt, Instant lastUsedAt) {
        QuarkusTransaction.requiringNew().run(() -> {
            AuthSession session = Arc.container().instance(AuthSessionRepository.class).get().findAll().firstResult();
            session.expiresAt = expiresAt;
            session.lastUsedAt = lastUsedAt;
        });
    }

    private static AuthSession onlySession() {
        return QuarkusTransaction.requiringNew().call(
                () -> Arc.container().instance(AuthSessionRepository.class).get().findAll().firstResult());
    }
}
