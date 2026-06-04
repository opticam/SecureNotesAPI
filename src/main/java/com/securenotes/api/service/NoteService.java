package com.securenotes.api.service;

import com.securenotes.api.dto.NoteRequest;
import com.securenotes.api.dto.NoteResponse;
import com.securenotes.api.dto.ShareRequest;
import com.securenotes.api.dto.ShareResponse;
import com.securenotes.api.entity.AppUser;
import com.securenotes.api.entity.NoteEntity;
import com.securenotes.api.entity.ShareEntity;
import com.securenotes.api.exception.ApiException;
import com.securenotes.api.repository.NoteRepository;
import com.securenotes.api.repository.ShareRepository;
import com.securenotes.api.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;

/**
 * Enforces note ownership and read-only sharing rules for all note operations.
 */
@ApplicationScoped
public class NoteService {

    private final CurrentUserService currentUserService;
    private final NoteRepository noteRepository;
    private final ShareRepository shareRepository;
    private final UserRepository userRepository;

    /**
     * Creates the note service.
     *
     * @param currentUserService authenticated user resolver
     * @param noteRepository note persistence dependency
     * @param shareRepository share persistence dependency
     * @param userRepository user persistence dependency
     */
    public NoteService(
            CurrentUserService currentUserService,
            NoteRepository noteRepository,
            ShareRepository shareRepository,
            UserRepository userRepository) {
        this.currentUserService = currentUserService;
        this.noteRepository = noteRepository;
        this.shareRepository = shareRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a note owned by the authenticated user.
     *
     * @param request validated note request
     * @return created note response
     */
    @Transactional
    public NoteResponse create(NoteRequest request) {
        AppUser currentUser = currentUserService.requireCurrentUser();
        NoteEntity note = new NoteEntity();
        note.owner = currentUser;
        note.content = request.content();
        noteRepository.persistAndFlush(note);
        return NoteResponse.from(note, currentUser.id);
    }

    /**
     * Lists all notes readable by the authenticated user.
     *
     * @return owned and explicitly shared notes
     */
    @Transactional
    public List<NoteResponse> listAccessible() {
        AppUser currentUser = currentUserService.requireCurrentUser();
        return noteRepository.listAccessibleTo(currentUser).stream()
                .map(note -> NoteResponse.from(note, currentUser.id))
                .toList();
    }

    /**
     * Returns a note when the authenticated user owns it or has share access.
     *
     * @param id note identifier
     * @return readable note response
     * @throws ApiException when the note is not visible to the requester
     */
    @Transactional
    public NoteResponse get(Long id) {
        AppUser currentUser = currentUserService.requireCurrentUser();
        NoteEntity note = requireReadableNote(id, currentUser);
        return NoteResponse.from(note, currentUser.id);
    }

    /**
     * Updates a note only when the authenticated user is the owner.
     *
     * @param id note identifier
     * @param request validated note request
     * @return updated note response
     * @throws ApiException when the note is missing, hidden, or shared read-only
     */
    @Transactional
    public NoteResponse update(Long id, NoteRequest request) {
        AppUser currentUser = currentUserService.requireCurrentUser();
        NoteEntity note = requireWritableNote(id, currentUser);
        note.content = request.content();
        return NoteResponse.from(note, currentUser.id);
    }

    /**
     * Deletes a note only when the authenticated user is the owner.
     *
     * @param id note identifier
     * @throws ApiException when the note is missing, hidden, or shared read-only
     */
    @Transactional
    public void delete(Long id) {
        AppUser currentUser = currentUserService.requireCurrentUser();
        NoteEntity note = requireWritableNote(id, currentUser);
        noteRepository.delete(note);
    }

    /**
     * Grants read-only note access to another user.
     *
     * @param id note identifier
     * @param request validated share request
     * @return share result indicating whether a new row was created
     * @throws ApiException when the requester is not the owner or the target user is invalid
     */
    @Transactional
    public ShareResult share(Long id, ShareRequest request) {
        AppUser currentUser = currentUserService.requireCurrentUser();
        NoteEntity note = requireWritableNote(id, currentUser);
        AppUser targetUser = userRepository.findByUsername(request.username().trim())
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "user to share with was not found"));

        if (targetUser.id.equals(currentUser.id)) {
            throw new ApiException(Response.Status.BAD_REQUEST, "owners already have full access to their notes");
        }

        return shareRepository.findByNoteAndUser(note, targetUser)
                .map(existing -> new ShareResult(ShareResponse.from(existing), false))
                .orElseGet(() -> {
                    ShareEntity share = new ShareEntity();
                    share.note = note;
                    share.sharedWithUser = targetUser;
                    share.createdAt = Instant.now();
                    shareRepository.persistAndFlush(share);
                    return new ShareResult(ShareResponse.from(share), true);
                });
    }

    private NoteEntity requireReadableNote(Long id, AppUser currentUser) {
        NoteEntity note = noteRepository.findByIdOptional(id)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "note was not found"));
        if (note.owner.id.equals(currentUser.id) || shareRepository.exists(note, currentUser)) {
            return note;
        }
        // Generic 404; do not reveal whether the id ever existed.
        throw new ApiException(Response.Status.NOT_FOUND, "note was not found");
    }

    private NoteEntity requireWritableNote(Long id, AppUser currentUser) {
        NoteEntity note = noteRepository.findByIdOptional(id)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "note was not found"));
        if (note.owner.id.equals(currentUser.id)) {
            return note;
        }
        if (shareRepository.exists(note, currentUser)) {
            throw new ApiException(Response.Status.FORBIDDEN, "shared notes are read-only");
        }
        throw new ApiException(Response.Status.NOT_FOUND, "note was not found");
    }

    /**
     * Result of a share request.
     *
     * @param response share representation
     * @param created whether this request created a new share row
     */
    public record ShareResult(ShareResponse response, boolean created) {
    }
}
