package pub.galactic.chorahost.resource;

import pub.galactic.chorahost.repository.ChoreCardRepository;
import pub.galactic.chorahost.repository.ChoreCardTemplateRepository;
import pub.galactic.chorahost.repository.SeasonRepository;
import pub.galactic.chorahost.repository.SeasonResultRepository;
import pub.galactic.chorahost.repository.TeamInvitationRepository;
import pub.galactic.chorahost.repository.TeamMembershipRepository;
import pub.galactic.chorahost.repository.TeamRepository;
import pub.galactic.chorahost.repository.UserRepository;
import io.quarkus.arc.Arc;
import io.quarkus.narayana.jta.QuarkusTransaction;
import org.junit.jupiter.api.BeforeAll;

/**
 * Wipes all tables once before each ordered test class runs. Test methods
 * within a class rely on state built up by earlier tests in that same class
 * (e.g. a team/card id captured in an earlier @Order step), so cleanup only
 * happens per class, not per method — done statically (via {@link Arc}
 * lookups rather than field injection) so the default per-method test
 * instance lifecycle can be kept.
 *
 * This matters under `quarkus:dev` continuous testing: the Dev Services
 * Postgres container stays up across re-runs (unlike a fresh `mvn test`,
 * which gets a throwaway container), so fixed test-fixture emails/subs would
 * otherwise collide with rows a previous run already created.
 *
 * Deliberately NOT annotated with {@code @QuarkusTest} itself — that
 * confuses Quarkus's test-instance construction for subclasses (it tries to
 * resolve the concrete subclass as a CDI bean and fails). Each concrete
 * subclass carries {@code @QuarkusTest} directly instead.
 */
abstract class AbstractResourceTest {

    @BeforeAll
    static void cleanDatabase() {
        QuarkusTransaction.requiringNew().run(() -> {
            Arc.container().instance(ChoreCardRepository.class).get().deleteAll();
            Arc.container().instance(ChoreCardTemplateRepository.class).get().deleteAll();
            Arc.container().instance(TeamInvitationRepository.class).get().deleteAll();
            Arc.container().instance(SeasonResultRepository.class).get().deleteAll();
            Arc.container().instance(SeasonRepository.class).get().deleteAll();
            Arc.container().instance(TeamMembershipRepository.class).get().deleteAll();
            Arc.container().instance(TeamRepository.class).get().deleteAll();
            Arc.container().instance(UserRepository.class).get().deleteAll();
        });
    }
}
