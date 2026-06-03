package com.securenotes.api.dto;

import com.securenotes.api.entity.NoteEntity;
import java.time.Instant;

public record NoteResponse(
        Long id,
        Long ownerId,
        String content,
        boolean ownedByRequester,
        boolean sharedReadOnly,
        Instant createdAt,
        Instant updatedAt
) {
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
