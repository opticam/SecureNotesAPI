package com.securenotes.api.dto;

import java.time.Instant;

public record AuthResponse(String token, String tokenType, Instant expiresAt) {
}
