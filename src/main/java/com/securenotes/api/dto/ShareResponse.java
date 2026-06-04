package com.securenotes.api.dto;

import com.securenotes.api.entity.ShareEntity;
import java.time.Instant;

/**
 * Representation of a persisted note share.
 *
 * @param id share identifier
 * @param noteId shared note identifier
 * @param sharedWithUserId recipient user identifier
 * @param createdAt timestamp when read-only access was granted
 */
public record ShareResponse(Long id, Long noteId, Long sharedWithUserId, Instant createdAt) {

    /**
     * Builds an API response from a persisted share.
     *
     * @param share persisted share entity
     * @return share response
     */
    public static ShareResponse from(ShareEntity share) {
        return new ShareResponse(share.id, share.note.id, share.sharedWithUser.id, share.createdAt);
    }
}
