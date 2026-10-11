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
 * Team shop: admins manage items, members spend their gold on them, admins
 * track which purchases have been handed over.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ShopResourceTest extends AbstractResourceTest {

    private static final String ADMIN_SUB = "shop-admin-sub";
    private static final String ADMIN_EMAIL = "shop-admin@chorahost.test";
    private static final String PLAYER_SUB = "shop-player-sub";
    private static final String PLAYER_EMAIL = "shop-player@chorahost.test";

    private static String inviteCode;
    private static String teamId;
    private static String playerId;
    private static String limitedItemId;
    private static String unlimitedItemId;
    private static String limitedPurchaseId;

    @Test
    @Order(1)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Shop Admin")
    })
    void setUpTeam() {
        teamId = given()
                .contentType("application/json")
                .body("{\"name\":\"Shop Test Team\"}")
                .when().post("/api/v1/teams")
                .then().statusCode(201)
                .extract().path("id");

        inviteCode = given()
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
            @Claim(key = "name", value = "Shop Player")
    })
    void playerJoinsTeam() {
        playerId = given()
                .when().get("/api/v1/users/me")
                .then().statusCode(200)
                .extract().path("id");

        given()
                .contentType("application/json")
                .body("{\"code\":\"" + inviteCode + "\"}")
                .when().post("/api/v1/invitations/accept")
                .then().statusCode(200);
    }

    @Test
    @Order(3)
    @TestSecurity(user = PLAYER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = PLAYER_SUB),
            @Claim(key = "email", value = PLAYER_EMAIL),
            @Claim(key = "name", value = "Shop Player")
    })
    void playerCannotManageItems() {
        given()
                .contentType("application/json")
                .body(itemPayload("Sneaky", 1, 1))
                .when().post("/api/v1/teams/" + teamId + "/shop/items")
                .then().statusCode(403);
    }

    @Test
    @Order(4)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Shop Admin")
    })
    void adminCreatesItems() {
        given()
                .contentType("application/json")
                .body("{\"name\":\"Bad\",\"price\":1,\"imageUrl\":\"javascript:alert(1)\"}")
                .when().post("/api/v1/teams/" + teamId + "/shop/items")
                .then().statusCode(400);

        limitedItemId = given()
                .contentType("application/json")
                .body(itemPayload("Pizza night", 30, 2))
                .when().post("/api/v1/teams/" + teamId + "/shop/items")
                .then().statusCode(201)
                .body("stock", is(2))
                .extract().path("id");

        unlimitedItemId = given()
                .contentType("application/json")
                .body("{\"name\":\"Screen time\",\"price\":10}")
                .when().post("/api/v1/teams/" + teamId + "/shop/items")
                .then().statusCode(201)
                .body("stock", nullValue())
                .extract().path("id");
    }

    @Test
    @Order(5)
    @TestSecurity(user = PLAYER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = PLAYER_SUB),
            @Claim(key = "email", value = PLAYER_EMAIL),
            @Claim(key = "name", value = "Shop Player")
    })
    void playerListsItemsButCannotAffordAny() {
        given()
                .when().get("/api/v1/teams/" + teamId + "/shop/items")
                .then().statusCode(200)
                .body("size()", is(2));

        given()
                .contentType("application/json")
                .body("{}")
                .when().post("/api/v1/teams/" + teamId + "/shop/items/" + limitedItemId + "/purchase")
                .then().statusCode(409);
    }

    @Test
    @Order(6)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Shop Admin")
    })
    void adminGivesPlayerGold() {
        given()
                .contentType("application/json")
                .body("{\"gold\":100}")
                .when().put("/api/v1/teams/" + teamId + "/members/" + playerId + "/gold")
                .then().statusCode(200);
    }

    @Test
    @Order(7)
    @TestSecurity(user = PLAYER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = PLAYER_SUB),
            @Claim(key = "email", value = PLAYER_EMAIL),
            @Claim(key = "name", value = "Shop Player")
    })
    void playerBuysUntilStockRunsOut() {
        limitedPurchaseId = given()
                .contentType("application/json")
                .body("{\"quantity\":2}")
                .when().post("/api/v1/teams/" + teamId + "/shop/items/" + limitedItemId + "/purchase")
                .then().statusCode(201)
                .body("totalPrice", is(60))
                .body("fulfilled", is(false))
                .extract().path("id");

        // Sold out.
        given()
                .contentType("application/json")
                .body("{}")
                .when().post("/api/v1/teams/" + teamId + "/shop/items/" + limitedItemId + "/purchase")
                .then().statusCode(409);

        // Unlimited stock, default quantity of 1.
        given()
                .contentType("application/json")
                .body("{}")
                .when().post("/api/v1/teams/" + teamId + "/shop/items/" + unlimitedItemId + "/purchase")
                .then().statusCode(201)
                .body("quantity", is(1));

        // 100 - 60 - 10
        given()
                .when().get("/api/v1/teams/" + teamId + "/leaderboard")
                .then().statusCode(200)
                .body("find { it.userId == '" + playerId + "' }.gold", is(30));

        // 30 gold can't cover 4 x 10.
        given()
                .contentType("application/json")
                .body("{\"quantity\":4}")
                .when().post("/api/v1/teams/" + teamId + "/shop/items/" + unlimitedItemId + "/purchase")
                .then().statusCode(409);
    }

    @Test
    @Order(8)
    @TestSecurity(user = PLAYER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = PLAYER_SUB),
            @Claim(key = "email", value = PLAYER_EMAIL),
            @Claim(key = "name", value = "Shop Player")
    })
    void playerSeesOnlyOwnPurchases() {
        given()
                .when().get("/api/v1/teams/" + teamId + "/shop/purchases")
                .then().statusCode(200)
                .body("size()", is(2));

        given()
                .when().get("/api/v1/teams/" + teamId + "/shop/purchases?all=true")
                .then().statusCode(403);

        given()
                .contentType("application/json")
                .body("{\"fulfilled\":true}")
                .when().put("/api/v1/teams/" + teamId + "/shop/purchases/" + limitedPurchaseId + "/fulfilled")
                .then().statusCode(403);
    }

    @Test
    @Order(9)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Shop Admin")
    })
    void adminFulfillsPurchase() {
        given()
                .when().get("/api/v1/teams/" + teamId + "/shop/purchases?all=true")
                .then().statusCode(200)
                .body("size()", is(2));

        given()
                .contentType("application/json")
                .body("{\"fulfilled\":true}")
                .when().put("/api/v1/teams/" + teamId + "/shop/purchases/" + limitedPurchaseId + "/fulfilled")
                .then().statusCode(200)
                .body("fulfilled", is(true))
                .body("fulfilledAt", notNullValue());
    }

    @Test
    @Order(10)
    @TestSecurity(user = ADMIN_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = ADMIN_SUB),
            @Claim(key = "email", value = ADMIN_EMAIL),
            @Claim(key = "name", value = "Shop Admin")
    })
    void deletedItemDisappearsButHistoryStays() {
        given()
                .when().delete("/api/v1/teams/" + teamId + "/shop/items/" + unlimitedItemId)
                .then().statusCode(204);

        given()
                .when().get("/api/v1/teams/" + teamId + "/shop/items")
                .then().statusCode(200)
                .body("size()", is(1));

        given()
                .contentType("application/json")
                .body("{}")
                .when().post("/api/v1/teams/" + teamId + "/shop/items/" + unlimitedItemId + "/purchase")
                .then().statusCode(404);

        given()
                .when().get("/api/v1/teams/" + teamId + "/shop/purchases?all=true")
                .then().statusCode(200)
                .body("size()", is(2));
    }

    private static String itemPayload(String name, int price, int stock) {
        return "{\"name\":\"" + name + "\",\"price\":" + price + ",\"stock\":" + stock
                + ",\"imageUrl\":\"https://example.com/" + name.hashCode() + ".png\"}";
    }
}
