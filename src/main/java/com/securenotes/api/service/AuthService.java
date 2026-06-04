package com.securenotes.api.service;

import com.securenotes.api.dto.AuthResponse;
import com.securenotes.api.dto.LoginRequest;
import com.securenotes.api.dto.RegisterRequest;
import com.securenotes.api.entity.AppUser;
import com.securenotes.api.exception.ApiException;
import com.securenotes.api.repository.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Handles local user registration, password verification, and JWT issuance.
 */
@ApplicationScoped
public class AuthService {

    private final UserRepository userRepository;
    private final String issuer;
    private final long tokenDurationSeconds;

    /**
     * Creates the authentication service.
     *
     * @param userRepository user persistence dependency
     * @param issuer JWT issuer value accepted by the API
     * @param tokenDurationSeconds token lifetime in seconds
     */
    public AuthService(
            UserRepository userRepository,
            @ConfigProperty(name = "securenotes.jwt.issuer") String issuer,
            @ConfigProperty(name = "securenotes.jwt.duration-seconds") long tokenDurationSeconds) {
        this.userRepository = userRepository;
        this.issuer = issuer;
        this.tokenDurationSeconds = tokenDurationSeconds;
    }

    /**
     * Registers a new user and returns an access token for immediate use.
     *
     * @param request validated registration request
     * @return bearer token response
     * @throws ApiException when the username is already registered
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        if (userRepository.findByUsername(username).isPresent()) {
            // OWASP Issue Fix: Do not acknowledge if a user by that name exists, simply return 404.
            throw new ApiException(Response.Status.NOT_FOUND, "not found");
        }

        AppUser user = new AppUser();
        user.username = username;
        user.passwordHash = BcryptUtil.bcryptHash(request.password());
        user.createdAt = Instant.now();
        userRepository.persistAndFlush(user);

        return tokenFor(user);
    }

    /**
     * Authenticates a user and returns a new access token.
     *
     * @param request validated login request
     * @return bearer token response
     * @throws ApiException when credentials are invalid
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepository.findByUsername(request.username().trim())
                .orElseThrow(() -> new ApiException(Response.Status.UNAUTHORIZED, "invalid username or password"));
        if (!BcryptUtil.matches(request.password(), user.passwordHash)) {
            throw new ApiException(Response.Status.UNAUTHORIZED, "invalid username or password");
        }
        return tokenFor(user);
    }

    private AuthResponse tokenFor(AppUser user) {
        Instant expiresAt = Instant.now().plusSeconds(tokenDurationSeconds);
        String token = Jwt.issuer(issuer)
                .subject(user.id.toString())
                .upn(user.username)
                .groups(Set.of("user"))
                .claim("username", user.username)
                .expiresAt(expiresAt)
                .sign();
        return new AuthResponse(token, "Bearer", expiresAt);
    }
}
