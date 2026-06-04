package com.securenotes.api.resource;

import com.securenotes.api.dto.AuthResponse;
import com.securenotes.api.dto.LoginRequest;
import com.securenotes.api.dto.RegisterRequest;
import com.securenotes.api.service.AuthService;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Public authentication endpoints for local user registration and login.
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@PermitAll
@Tag(name = "Authentication")
public class AuthResource {

    private final AuthService authService;

    /**
     * Creates the authentication resource.
     *
     * @param authService authentication service dependency
     */
    public AuthResource(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registers a new user and returns a bearer token.
     *
     * @param request validated registration request
     * @return authentication response containing a signed JWT
     */
    @POST
    @Path("/register")
    @Operation(summary = "Register a new user and return a bearer token")
    public AuthResponse register(@Valid RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * Authenticates an existing user and returns a bearer token.
     *
     * @param request validated login request
     * @return authentication response containing a signed JWT
     */
    @POST
    @Path("/login")
    @Operation(summary = "Authenticate an existing user and return a bearer token")
    public AuthResponse login(@Valid LoginRequest request) {
        return authService.login(request);
    }
}
