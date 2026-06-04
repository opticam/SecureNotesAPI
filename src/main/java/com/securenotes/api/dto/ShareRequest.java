package com.securenotes.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for granting read-only note access to another user.
 *
 * @param username username that should receive read-only access
 */
public record ShareRequest(
        @NotBlank(message = "username is required") String username
) {
}
