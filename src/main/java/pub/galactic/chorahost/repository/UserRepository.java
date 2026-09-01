package pub.galactic.chorahost.repository;

import java.util.Optional;
import java.util.UUID;

import pub.galactic.chorahost.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<User, UUID> {
    public Optional<User> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }
}
