package pub.galactic.chorahost.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import pub.galactic.chorahost.entity.ChoreCard;
import pub.galactic.chorahost.entity.Season;
import pub.galactic.chorahost.entity.TeamMembership;
import pub.galactic.chorahost.enums.CardStatus;
import pub.galactic.chorahost.enums.Rarity;
import pub.galactic.chorahost.enums.SeasonStatus;
import pub.galactic.chorahost.repository.ChoreCardRepository;
import pub.galactic.chorahost.repository.SeasonRepository;
import pub.galactic.chorahost.repository.TeamMembershipRepository;
import pub.galactic.chorahost.service.SeasonService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SeasonTest extends AbstractResourceTest {

    private static final String OWNER_SUB = "season-owner-sub";
    private static final String OWNER_EMAIL = "season-owner@chorahost.test";
    private static final String MEMBER_SUB = "season-member-sub";
    private static final String MEMBER_EMAIL = "season-member@chorahost.test";

    private static String teamId;
    private static String ownerId;
    private static String memberId;
    private static String firstSeasonId;

    @Inject
    SeasonService seasonService;

    @Inject
    SeasonRepository seasons;

    @Inject
    TeamMembershipRepository memberships;

    @Inject
    ChoreCardRepository cards;

    @Test
    @Order(1)
    @TestSecurity(user = OWNER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = OWNER_SUB),
            @Claim(key = "email", value = OWNER_EMAIL),
            @Claim(key = "name", value = "Season Owner")
    })
    void newTeamStartsWithActiveSeason() {
        teamId = given().contentType("application/json").body("{\"name\":\"Season Team\"}")
                .when().post("/api/v1/teams")
                .then().statusCode(201)
                .body("seasonLengthDays", is(30))
                .extract().path("id");

        ownerId = given().when().get("/api/v1/users/me").then().statusCode(200).extract().path("id");

        String code = given().contentType("application/json").body("{\"email\":\"" + MEMBER_EMAIL + "\"}")
                .when().post("/api/v1/teams/" + teamId + "/invitations")
                .then().statusCode(201).extract().path("code");
        // Accept as the member below via a direct service-free call.
        memberCode = code;

        firstSeasonId = given().when().get("/api/v1/teams/" + teamId + "/seasons/current")
                .then().statusCode(200)
                .body("number", is(1))
                .body("status", is("ACTIVE"))
                .extract().path("id");
    }

    private static String memberCode;

    @Test
    @Order(2)
    @TestSecurity(user = MEMBER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = MEMBER_SUB),
            @Claim(key = "email", value = MEMBER_EMAIL),
            @Claim(key = "name", value = "Season Member")
    })
    void memberJoins() {
        given().contentType("application/json").body("{\"code\":\"" + memberCode + "\"}")
                .when().post("/api/v1/invitations/accept").then().statusCode(200);
        memberId = given().when().get("/api/v1/users/me").then().statusCode(200).extract().path("id");
    }

    @Test
    @Order(3)
    @TestSecurity(user = MEMBER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = MEMBER_SUB),
            @Claim(key = "email", value = MEMBER_EMAIL),
            @Claim(key = "name", value = "Season Member")
    })
    void plainMemberCannotManageSeasons() {
        given().contentType("application/json").body("{\"days\":14}")
                .when().put("/api/v1/teams/" + teamId + "/season-length").then().statusCode(403);
        given().when().post("/api/v1/teams/" + teamId + "/seasons/end").then().statusCode(403);
        given().contentType("application/json").body("{\"days\":3}")
                .when().post("/api/v1/teams/" + teamId + "/seasons/extend").then().statusCode(403);
    }

    @Test
    @Order(4)
    @TestSecurity(user = OWNER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = OWNER_SUB),
            @Claim(key = "email", value = OWNER_EMAIL),
            @Claim(key = "name", value = "Season Owner")
    })
    void seasonLengthIsValidatedAndExtendMovesEndDate() {
        given().contentType("application/json").body("{\"days\":3}")
                .when().put("/api/v1/teams/" + teamId + "/season-length").then().statusCode(400);
        given().contentType("application/json").body("{\"days\":14}")
                .when().put("/api/v1/teams/" + teamId + "/season-length")
                .then().statusCode(200).body("seasonLengthDays", is(14));

        Instant before = activeEndsAt();
        given().contentType("application/json").body("{\"days\":2}")
                .when().post("/api/v1/teams/" + teamId + "/seasons/extend").then().statusCode(200);
        Instant after = activeEndsAt();
        assertEquals(before.plus(2, ChronoUnit.DAYS), after);
    }

    // Fresh transaction: the test thread's session would otherwise serve a cached entity.
    private Instant activeEndsAt() {
        return QuarkusTransaction.requiringNew()
                .call(() -> seasons.findActive(UUID.fromString(teamId)).orElseThrow().endsAt);
    }

    @Test
    @Order(5)
    void rolloverArchivesRanksAndResetsStats() {
        UUID team = UUID.fromString(teamId);
        QuarkusTransaction.requiringNew().run(() -> {
            TeamMembership owner = memberships.findByUserAndTeam(UUID.fromString(ownerId), team).orElseThrow();
            owner.level = 2;
            owner.currentXp = 300;
            owner.gold = 40;
            TeamMembership member = memberships.findByUserAndTeam(UUID.fromString(memberId), team).orElseThrow();
            member.level = 3;
            member.currentXp = 10;
            member.gold = 5;

            ChoreCard card = new ChoreCard();
            card.teamId = team;
            card.title = "Done this season";
            card.rarity = Rarity.COMMON;
            card.complexity = 1;
            card.estimatedMinutes = 5;
            card.xpReward = 5;
            card.goldReward = 1;
            card.status = CardStatus.VERIFIED;
            card.assignedUserId = member.userId;
            card.verifiedAt = Instant.now();
            cards.persist(card);
        });

        // Not due yet: nothing happens.
        seasonService.rollOver(team, false);
        assertEquals(1, seasons.findActive(team).orElseThrow().number);

        QuarkusTransaction.requiringNew().run(() -> {
            Season s = seasons.findActive(team).orElseThrow();
            s.endsAt = Instant.now().minusSeconds(1);
        });
        seasonService.rollOver(team, false);
        // A repeated call must not roll over again.
        seasonService.rollOver(team, false);

        Season next = seasons.findActive(team).orElseThrow();
        assertEquals(2, next.number);
        assertTrue(next.endsAt.isAfter(Instant.now()));
        assertEquals(2, seasons.findByTeam(team).size());

        QuarkusTransaction.requiringNew().run(() -> {
            for (String uid : new String[] { ownerId, memberId }) {
                TeamMembership m = memberships.findByUserAndTeam(UUID.fromString(uid), team).orElseThrow();
                assertEquals(1, m.level);
                assertEquals(0, m.currentXp);
                assertEquals(0, m.gold);
            }
            assertEquals(SeasonStatus.ENDED, seasons.findById(UUID.fromString(firstSeasonId)).status);
        });
    }

    @Test
    @Order(6)
    @TestSecurity(user = MEMBER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = MEMBER_SUB),
            @Claim(key = "email", value = MEMBER_EMAIL),
            @Claim(key = "name", value = "Season Member")
    })
    void resultsOfEndedSeasonAreRanked() {
        given().when().get("/api/v1/teams/" + teamId + "/seasons/" + firstSeasonId + "/results")
                .then().statusCode(200)
                .body("$", hasSize(2))
                .body("[0].userId", is(memberId))
                .body("[0].rank", is(1))
                .body("[0].level", is(3))
                .body("[0].cardsVerified", is(1))
                .body("[1].userId", is(ownerId))
                .body("[1].rank", is(2))
                .body("[1].gold", is(40))
                .body("[1].cardsVerified", is(0));

        given().when().get("/api/v1/teams/" + teamId + "/seasons").then().statusCode(200)
                .body("$", hasSize(2))
                .body("[0].number", is(2));

        given().when().get("/api/v1/teams/" + teamId + "/leaderboard").then().statusCode(200)
                .body("[0].level", is(1))
                .body("[0].gold", is(0));
    }

    @Test
    @Order(7)
    @TestSecurity(user = OWNER_SUB)
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = OWNER_SUB),
            @Claim(key = "email", value = OWNER_EMAIL),
            @Claim(key = "name", value = "Season Owner")
    })
    void adminCanEndSeasonEarly() {
        given().when().post("/api/v1/teams/" + teamId + "/seasons/end")
                .then().statusCode(200).body("number", is(3)).body("status", is("ACTIVE"));
    }

    @Test
    @Order(8)
    void teamWithoutSeasonGetsOneFromTheRolloverJob() {
        UUID orphan = QuarkusTransaction.requiringNew().call(() -> {
            pub.galactic.chorahost.entity.Team t = new pub.galactic.chorahost.entity.Team();
            t.name = "Orphan Team";
            t.persist();
            return t.id;
        });
        seasonService.rollOver(orphan, false);
        assertEquals(1, seasons.findActive(orphan).orElseThrow().number);
    }
}
