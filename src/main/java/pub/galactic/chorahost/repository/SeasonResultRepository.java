package pub.galactic.chorahost.repository;

import java.util.List;
import java.util.UUID;

import pub.galactic.chorahost.entity.SeasonResult;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SeasonResultRepository implements PanacheRepositoryBase<SeasonResult, UUID> {

    public List<SeasonResult> findBySeason(UUID seasonId) {
        return find("seasonId = ?1 order by rank", seasonId).list();
    }
}
