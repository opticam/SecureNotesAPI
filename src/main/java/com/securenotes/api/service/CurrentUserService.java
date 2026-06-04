package com.securenotes.api.service;

import com.securenotes.api.entity.AppUser;
import com.securenotes.api.exception.ApiException;
import com.securenotes.api.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Resolves the persisted application user represented by the active JWT.
 */
@ApplicationScoped
public class CurrentUserService {

    private final JsonWebToken jwt;
    private final UserRepository userRepository;

    /**
     * Creates the current-user resolver.
     *
     * @param jwt token associated with the current request
     * @param userRepository user persistence dependency
     */
    public CurrentUserService(JsonWebToken jwt, UserRepository userRepository) {
        this.jwt = jwt;
        this.userRepository = userRepository;
    }

    /**
     * Loads the authenticated user from the JWT {@code sub} claim.
     *
     * @return persisted user represented by the token
     * @throws ApiException when the token subject is invalid or stale
     */
    public AppUser requireCurrentUser() {
        try {
            Long userId = Long.valueOf(jwt.getSubject());
            return userRepository.findByIdOptional(userId)
                    .orElseThrow(() -> new ApiException(Response.Status.UNAUTHORIZED, "authenticated user no longer exists"));
        } catch (NumberFormatException exception) {
            throw new ApiException(Response.Status.UNAUTHORIZED, "invalid token subject");
        }
    }
}
