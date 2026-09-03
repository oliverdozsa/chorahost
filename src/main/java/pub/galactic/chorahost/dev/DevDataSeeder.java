package pub.galactic.chorahost.dev;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import pub.galactic.chorahost.entity.ChoreCard;
import pub.galactic.chorahost.entity.Team;
import pub.galactic.chorahost.entity.TeamMembership;
import pub.galactic.chorahost.entity.User;
import pub.galactic.chorahost.enums.CardStatus;
import pub.galactic.chorahost.enums.Rarity;
import pub.galactic.chorahost.enums.TeamRole;
import pub.galactic.chorahost.repository.ChoreCardRepository;
import pub.galactic.chorahost.repository.TeamMembershipRepository;
import pub.galactic.chorahost.repository.TeamRepository;
import pub.galactic.chorahost.repository.UserRepository;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

/**
 * Seeds a couple of teams with members and chore cards on startup in dev
 * mode, so the app has something to look at without manually creating data
 * through the API first. Only runs when the database has no teams yet, so it
 * won't duplicate data across dev-mode live reloads.
 */
@ApplicationScoped
@IfBuildProfile("dev")
public class DevDataSeeder {

    private static final Logger LOG = Logger.getLogger(DevDataSeeder.class);

    @Inject
    TeamRepository teamRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    TeamMembershipRepository teamMembershipRepository;

    @Inject
    ChoreCardRepository choreCardRepository;

    @Transactional
    void onStart(@Observes StartupEvent event) {
        if (teamRepository.count() > 0) {
            return;
        }

        LOG.info("Seeding dev data: teams, users, and chore cards");

        User alice = user("alice@example.com", "Alice");
        User bob = user("bob@example.com", "Bob");
        User carol = user("carol@example.com", "Carol");
        User dave = user("dave@example.com", "Dave");

        Team household = team("The Chore Chrusaders Household");
        membership(household, alice, TeamRole.TEAM_ADMIN);
        membership(household, bob, TeamRole.USER);

        card(household, "Wash the dishes", "Clean and put away all dishes after dinner.",
                Rarity.COMMON, 2, 15, 10, 5, null, CardStatus.AVAILABLE, null);
        card(household, "Vacuum the living room", "Vacuum carpets and rugs in the living room.",
                Rarity.COMMON, 3, 20, 15, 5, null, CardStatus.CLAIMED, bob);
        card(household, "Deep clean the bathroom", "Scrub tub, sink, toilet, and mop the floor.",
                Rarity.RARE, 6, 45, 40, 20, "Sparkle Sponge", CardStatus.COMPLETED, bob);
        card(household, "Mow the lawn", "Mow front and back yard, trim edges.",
                Rarity.EPIC, 7, 60, 60, 30, "Golden Shears", CardStatus.VERIFIED, alice);

        Team roomies = team("Downtown Roomies");
        membership(roomies, carol, TeamRole.TEAM_ADMIN);
        membership(roomies, dave, TeamRole.USER);

        card(roomies, "Take out the trash", "Bins go to the curb, recycling too.",
                Rarity.COMMON, 1, 10, 5, 5, null, CardStatus.AVAILABLE, null);
        card(roomies, "Restock kitchen supplies", "Buy paper towels, dish soap, and sponges.",
                Rarity.RARE, 4, 30, 25, 15, null, CardStatus.CLAIMED, dave);
        card(roomies, "Organize the pantry", "Sort and label all pantry shelves.",
                Rarity.LEGENDARY, 9, 90, 100, 50, "Master Organizer Badge", CardStatus.VERIFIED, carol);
    }

    private User user(String email, String name) {
        User u = new User();
        u.email = email;
        u.name = name;
        userRepository.persist(u);
        return u;
    }

    private Team team(String name) {
        Team t = new Team();
        t.name = name;
        teamRepository.persist(t);
        return t;
    }

    private void membership(Team team, User user, TeamRole role) {
        TeamMembership m = new TeamMembership();
        m.teamId = team.id;
        m.userId = user.id;
        m.role = role;
        teamMembershipRepository.persist(m);
    }

    private void card(Team team, String title, String description, Rarity rarity, int complexity,
            int estimatedMinutes, int xpReward, int goldReward, String itemDrop, CardStatus status, User assignee) {
        ChoreCard c = new ChoreCard();
        c.teamId = team.id;
        c.title = title;
        c.description = description;
        c.rarity = rarity;
        c.complexity = complexity;
        c.estimatedMinutes = estimatedMinutes;
        c.xpReward = xpReward;
        c.goldReward = goldReward;
        c.itemDrop = itemDrop;
        c.status = status;
        c.assignedUserId = assignee != null ? assignee.id : null;

        Instant now = Instant.now();
        if (status == CardStatus.CLAIMED || status == CardStatus.COMPLETED || status == CardStatus.VERIFIED) {
            c.claimedAt = now.minus(2, ChronoUnit.DAYS);
        }
        if (status == CardStatus.COMPLETED || status == CardStatus.VERIFIED) {
            c.completedAt = now.minus(1, ChronoUnit.DAYS);
        }
        if (status == CardStatus.VERIFIED) {
            c.verifiedAt = now.minus(12, ChronoUnit.HOURS);
        }

        choreCardRepository.persist(c);
    }
}
