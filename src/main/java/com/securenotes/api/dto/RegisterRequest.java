package com.securenotes.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "username is required")
        @Size(min = 3, max = 100, message = "username must be between 3 and 100 characters")
        String username,
        @NotBlank(message = "password is required")
        @Size(min = 12, max = 128, message = "password must be between 12 and 128 characters")
        String password
) {
}
