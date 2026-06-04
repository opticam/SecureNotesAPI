package com.securenotes.api.repository;

import com.securenotes.api.entity.AppUser;
import com.securenotes.api.entity.NoteEntity;
import com.securenotes.api.entity.ShareEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/**
 * Persistence operations for read-only note shares.
 */
@ApplicationScoped
public class ShareRepository implements PanacheRepository<ShareEntity> {

    /**
     * Checks whether a user has explicit read-only access to a note.
     *
     * @param note note to check
     * @param user user whose share access is checked
     * @return true when a share exists
     */
    public boolean exists(NoteEntity note, AppUser user) {
        return count("note = ?1 and sharedWithUser = ?2", note, user) > 0;
    }

    /**
     * Finds the share record for a note and recipient.
     *
     * @param note shared note
     * @param user recipient user
     * @return matching share when present
     */
    public Optional<ShareEntity> findByNoteAndUser(NoteEntity note, AppUser user) {
        return find("note = ?1 and sharedWithUser = ?2", note, user).firstResultOptional();
    }
}
