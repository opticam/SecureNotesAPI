package com.securenotes.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating or updating a note.
 *
 * @param content note body owned by the authenticated user
 */
public record NoteRequest(
        @NotBlank(message = "content is required")
        @Size(max = 10000, message = "content must be 10000 characters or fewer")
        String content
) {
}
