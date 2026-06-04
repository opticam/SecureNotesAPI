package com.securenotes.api.dto;

import com.securenotes.api.entity.NoteEntity;
import java.time.Instant;

/**
 * Note representation returned to authenticated callers.
 *
 * @param id note identifier
 * @param ownerId identifier of the user who owns the note
 * @param content note body
 * @param ownedByRequester whether the authenticated user owns the note
 * @param sharedReadOnly whether the authenticated user only has shared read access
 * @param createdAt creation timestamp
 * @param updatedAt last update timestamp
 */
public record NoteResponse(
        Long id,
        Long ownerId,
        String content,
        boolean ownedByRequester,
        boolean sharedReadOnly,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Builds an API response from a persisted note and the current requester.
     *
     * @param note persisted note entity
     * @param requesterId authenticated user's identifier
     * @return note response with ownership flags derived for the requester
     */
    public static NoteResponse from(NoteEntity note, Long requesterId) {
        boolean owned = note.owner.id.equals(requesterId);
        return new NoteResponse(
                note.id,
                note.owner.id,
                note.content,
                owned,
                !owned,
                note.createdAt,
                note.updatedAt);
    }
}
