package com.securenotes.api.repository;

import com.securenotes.api.entity.AppUser;
import com.securenotes.api.entity.NoteEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

/**
 * Persistence operations for notes and note visibility queries.
 */
@ApplicationScoped
public class NoteRepository implements PanacheRepository<NoteEntity> {

    /**
     * Lists notes the user can read through ownership or explicit sharing.
     *
     * @param user authenticated user
     * @return readable notes ordered by most recent update
     */
    public List<NoteEntity> listAccessibleTo(AppUser user) {
        return getEntityManager()
                .createQuery(
                        """
                        select distinct note
                        from NoteEntity note
                        left join ShareEntity share
                            on share.note = note and share.sharedWithUser = :user
                        where note.owner = :user or share.id is not null
                        order by note.updatedAt desc
                        """,
                        NoteEntity.class)
                .setParameter("user", user)
                .getResultList();
    }
}
