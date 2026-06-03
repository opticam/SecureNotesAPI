package com.securenotes.api.dto;

import com.securenotes.api.entity.ShareEntity;
import java.time.Instant;

public record ShareResponse(Long id, Long noteId, Long sharedWithUserId, Instant createdAt) {

    public static ShareResponse from(ShareEntity share) {
        return new ShareResponse(share.id, share.note.id, share.sharedWithUser.id, share.createdAt);
    }
}
