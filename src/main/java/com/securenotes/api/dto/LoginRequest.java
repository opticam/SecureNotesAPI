package com.securenotes.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Credentials submitted by an existing user to obtain a JWT.
 *
 * @param username registered username
 * @param password plaintext password supplied for verification
 */
public record LoginRequest(
        @NotBlank(message = "username is required") String username,
        @NotBlank(message = "password is required") String password
) {
}
