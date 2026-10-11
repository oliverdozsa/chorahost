package pub.galactic.chorahost.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import pub.galactic.chorahost.entity.Season;
import pub.galactic.chorahost.enums.SeasonStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SeasonRepository implements PanacheRepositoryBase<Season, UUID> {

    public Optional<Season> findActive(UUID teamId) {
        return find("teamId = ?1 and status = ?2", teamId, SeasonStatus.ACTIVE).firstResultOptional();
    }

    public Optional<Season> findLatest(UUID teamId) {
        return find("teamId = ?1 order by number desc", teamId).firstResultOptional();
    }

    public List<Season> findByTeam(UUID teamId) {
        return find("teamId = ?1 order by number desc", teamId).list();
    }

    public List<Season> findActiveEndedBy(Instant now) {
        return find("status = ?1 and endsAt <= ?2", SeasonStatus.ACTIVE, now).list();
    }
}
