package com.securenotes.api.dto;

import java.time.Instant;

/**
 * Authentication response returned after successful registration or login.
 *
 * @param token signed JWT used as a bearer token for protected endpoints
 * @param tokenType token type clients should send in the authorization header
 * @param expiresAt instant when the token is no longer valid
 */
public record AuthResponse(String token, String tokenType, Instant expiresAt) {
}
