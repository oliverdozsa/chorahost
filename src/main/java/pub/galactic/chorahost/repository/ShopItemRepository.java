package pub.galactic.chorahost.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import pub.galactic.chorahost.entity.ShopItem;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

@ApplicationScoped
public class ShopItemRepository implements PanacheRepositoryBase<ShopItem, UUID> {

    public List<ShopItem> findActiveByTeam(UUID teamId) {
        return find("teamId = ?1 and active = true order by createdAt", teamId).list();
    }

    public Optional<ShopItem> findActiveInTeam(UUID itemId, UUID teamId) {
        return find("id = ?1 and teamId = ?2 and active = true", itemId, teamId).firstResultOptional();
    }

    /** Same as {@link #findActiveInTeam} but row-locked, for stock changes. */
    public Optional<ShopItem> findActiveInTeamForUpdate(UUID itemId, UUID teamId) {
        return find("id = ?1 and teamId = ?2 and active = true", itemId, teamId)
                .withLock(LockModeType.PESSIMISTIC_WRITE).firstResultOptional();
    }
}
