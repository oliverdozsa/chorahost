package pub.galactic.chorahost.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import pub.galactic.chorahost.dto.SeasonResultResponse;
import pub.galactic.chorahost.entity.ChoreCard;
import pub.galactic.chorahost.entity.Season;
import pub.galactic.chorahost.entity.SeasonResult;
import pub.galactic.chorahost.entity.Team;
import pub.galactic.chorahost.entity.TeamMembership;
import pub.galactic.chorahost.entity.User;
import pub.galactic.chorahost.enums.CardStatus;
import pub.galactic.chorahost.enums.SeasonStatus;
import pub.galactic.chorahost.exception.ApiException;
import pub.galactic.chorahost.repository.ChoreCardRepository;
import pub.galactic.chorahost.repository.SeasonRepository;
import pub.galactic.chorahost.repository.SeasonResultRepository;
import pub.galactic.chorahost.repository.TeamMembershipRepository;
import pub.galactic.chorahost.repository.TeamRepository;
import pub.galactic.chorahost.repository.UserRepository;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

/**
 * Runs back-to-back seasons per team. Level, XP and gold live on the team
 * membership, so a season is "the period since the last reset": when one
 * ends, every member's standing is archived as a {@link SeasonResult} and
 * their stats are reset for the next season. Rewards are paid out when a card
 * is verified, into whichever season is running at that moment.
 */
@ApplicationScoped
public class SeasonService {

    public static final int MIN_LENGTH_DAYS = 7;
    public static final int MAX_LENGTH_DAYS = 365;

    private static final Logger LOG = Logger.getLogger(SeasonService.class);

    @Inject
    SeasonRepository seasonRepository;

    @Inject
    SeasonResultRepository resultRepository;

    @Inject
    TeamRepository teamRepository;

    @Inject
    TeamMembershipRepository membershipRepository;

    @Inject
    ChoreCardRepository choreCardRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    TeamService teamService;

    /** Closes seasons that have run out and opens one for teams that have none. */
    @Scheduled(every = "{chorahost.season.check-interval:1m}", concurrentExecution = ConcurrentExecution.SKIP)
    void rollOverDueSeasons() {
        for (UUID teamId : findTeamsNeedingRollover()) {
            try {
                rollOver(teamId, false);
            } catch (RuntimeException e) {
                LOG.errorf(e, "Season rollover failed for team %s", teamId);
            }
        }
    }

    @Transactional
    List<UUID> findTeamsNeedingRollover() {
        List<UUID> ids = seasonRepository.findActiveEndedBy(Instant.now()).stream().map(s -> s.teamId).toList();
        List<UUID> without = teamRepository.findWithoutActiveSeason().stream().map(t -> t.id).toList();
        return java.util.stream.Stream.concat(ids.stream(), without.stream()).toList();
    }

    /**
     * Ends the team's running season if it is due (or {@code force}d), archiving
     * and resetting every member, then starts the next one. Also starts the
     * first season for a team without one. Idempotent: the team row is locked
     * and the season re-checked, so a concurrent or repeated call is a no-op.
     */
    @Transactional
    public Season rollOver(UUID teamId, boolean force) {
        Team team = teamRepository.findById(teamId, LockModeType.PESSIMISTIC_WRITE);
        if (team == null) {
            throw ApiException.notFound("Team not found: " + teamId);
        }

        Instant now = Instant.now();
        Season active = seasonRepository.findActive(teamId).orElse(null);
        if (active != null) {
            if (!force && active.endsAt.isAfter(now)) {
                return active;
            }
            endSeason(active, now);
            // Flush the ENDED update before inserting the next ACTIVE season (unique-active index).
            seasonRepository.flush();
        }

        int number = seasonRepository.findLatest(teamId).map(s -> s.number + 1).orElse(1);
        Season next = new Season();
        next.teamId = teamId;
        next.number = number;
        next.startsAt = now;
        next.endsAt = now.plus(team.seasonLengthDays, ChronoUnit.DAYS);
        seasonRepository.persist(next);
        LOG.infof("Team %s: started season %d, ends %s", teamId, number, next.endsAt);
        return next;
    }

    private void endSeason(Season season, Instant now) {
        List<TeamMembership> standings = membershipRepository
                .findByTeamOrderByLevelDescCurrentXpDesc(season.teamId);

        Map<UUID, Long> verifiedByUser = choreCardRepository
                .list("teamId = ?1 and status = ?2 and verifiedAt >= ?3",
                        season.teamId, CardStatus.VERIFIED, season.startsAt)
                .stream()
                .filter(c -> c.assignedUserId != null)
                .collect(Collectors.groupingBy((ChoreCard c) -> c.assignedUserId, Collectors.counting()));

        int rank = 1;
        for (TeamMembership m : standings) {
            SeasonResult result = new SeasonResult();
            result.seasonId = season.id;
            result.userId = m.userId;
            result.rank = rank++;
            result.level = m.level;
            result.currentXp = m.currentXp;
            result.gold = m.gold;
            result.cardsVerified = verifiedByUser.getOrDefault(m.userId, 0L).intValue();
            resultRepository.persist(result);

            m.level = 1;
            m.currentXp = 0;
            m.gold = 0;
        }

        season.status = SeasonStatus.ENDED;
        season.endsAt = now;
    }

    /** Starts the first season for a freshly created team. */
    @Transactional
    public Season startFirstSeason(Team team) {
        Season season = new Season();
        season.teamId = team.id;
        season.number = 1;
        season.startsAt = Instant.now();
        season.endsAt = season.startsAt.plus(team.seasonLengthDays, ChronoUnit.DAYS);
        seasonRepository.persist(season);
        return season;
    }

    public Season getCurrent(UUID teamId, User requester) {
        teamService.requireMembership(teamId, requester.id);
        return seasonRepository.findActive(teamId)
                .orElseThrow(() -> ApiException.notFound("Team has no running season: " + teamId));
    }

    public List<Season> list(UUID teamId, User requester) {
        teamService.requireMembership(teamId, requester.id);
        return seasonRepository.findByTeam(teamId);
    }

    public List<SeasonResultResponse> getResults(UUID teamId, UUID seasonId, User requester) {
        teamService.requireMembership(teamId, requester.id);
        Season season = requireSeason(teamId, seasonId);
        return resultsFor(season);
    }

    /** Standings archived for an ended season, ranked. Empty for a running season. */
    public List<SeasonResultResponse> resultsFor(Season season) {
        List<SeasonResult> results = resultRepository.findBySeason(season.id);
        if (results.isEmpty()) {
            return List.of();
        }
        Map<UUID, User> users = userRepository.list("id in ?1", results.stream().map(r -> r.userId).toList())
                .stream().collect(Collectors.toMap(u -> u.id, u -> u));
        return results.stream()
                .map(r -> new SeasonResultResponse(r.userId, users.get(r.userId).name, r.rank, r.level,
                        r.currentXp, r.gold, r.cardsVerified))
                .toList();
    }

    public Season requireSeason(UUID teamId, UUID seasonId) {
        return seasonRepository.findByIdOptional(seasonId)
                .filter(s -> s.teamId.equals(teamId))
                .orElseThrow(() -> ApiException.notFound("Season not found: " + seasonId));
    }

    /** Sets the length for future seasons; the running season keeps its end date. */
    @Transactional
    public Team setLength(UUID teamId, User requester, int days) {
        teamService.requireAdminOrTeamAdmin(teamId, requester);
        Team team = teamRepository.findById(teamId);
        team.seasonLengthDays = days;
        return team;
    }

    @Transactional
    public Season endNow(UUID teamId, User requester) {
        teamService.requireAdminOrTeamAdmin(teamId, requester);
        return rollOver(teamId, true);
    }

    @Transactional
    public Season extend(UUID teamId, User requester, int days) {
        teamService.requireAdminOrTeamAdmin(teamId, requester);
        Season active = seasonRepository.findActive(teamId)
                .orElseThrow(() -> ApiException.notFound("Team has no running season: " + teamId));
        active.endsAt = active.endsAt.plus(days, ChronoUnit.DAYS);
        return active;
    }
}
