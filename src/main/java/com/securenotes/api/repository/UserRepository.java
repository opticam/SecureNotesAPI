package com.securenotes.api.repository;

import com.securenotes.api.entity.AppUser;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/**
 * Persistence operations for application users.
 */
@ApplicationScoped
public class UserRepository implements PanacheRepository<AppUser> {

    /**
     * Finds a user by its unique username.
     *
     * @param username username to search for
     * @return matching user when present
     */
    public Optional<AppUser> findByUsername(String username) {
        return find("username", username).firstResultOptional();
    }
}
