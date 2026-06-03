package com.securenotes.api.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * Read-only access grant from a note owner to another user.
 */
@Entity
@Table(
        name = "note_shares",
        uniqueConstraints = @UniqueConstraint(name = "uq_note_shares_note_user", columnNames = {"note_id", "shared_with_user_id"})
)
public class ShareEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "note_id", nullable = false)
    public NoteEntity note;

    @ManyToOne(optional = false)
    @JoinColumn(name = "shared_with_user_id", nullable = false)
    public AppUser sharedWithUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;
}
