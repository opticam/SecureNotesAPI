package com.securenotes.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ShareRequest(
        @NotBlank(message = "username is required") String username
) {
}
