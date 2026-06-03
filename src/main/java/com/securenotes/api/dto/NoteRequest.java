package com.securenotes.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoteRequest(
        @NotBlank(message = "content is required")
        @Size(max = 10000, message = "content must be 10000 characters or fewer")
        String content
) {
}
