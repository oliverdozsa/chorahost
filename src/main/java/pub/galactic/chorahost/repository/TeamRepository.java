package pub.galactic.chorahost.repository;

import java.util.List;
import java.util.UUID;

import pub.galactic.chorahost.entity.Team;
import pub.galactic.chorahost.enums.SeasonStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TeamRepository implements PanacheRepositoryBase<Team, UUID> {

    /** Teams that have no running season yet (e.g. created outside the service layer). */
    public List<Team> findWithoutActiveSeason() {
        return find("id not in (select s.teamId from Season s where s.status = ?1)", SeasonStatus.ACTIVE).list();
    }
}
