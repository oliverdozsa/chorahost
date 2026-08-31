package pub.galactic.chorahost.repository;

import java.util.List;
import java.util.UUID;

import pub.galactic.chorahost.entity.ChoreCardTemplate;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ChoreCardTemplateRepository implements PanacheRepositoryBase<ChoreCardTemplate, UUID> {

    public List<ChoreCardTemplate> findByTeam(UUID teamId) {
        return find("teamId", teamId).list();
    }
}
