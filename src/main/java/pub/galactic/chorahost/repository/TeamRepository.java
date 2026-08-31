package pub.galactic.chorahost.repository;

import java.util.UUID;

import pub.galactic.chorahost.entity.Team;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TeamRepository implements PanacheRepositoryBase<Team, UUID> {
}
