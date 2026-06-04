package com.securenotes.api.resource;

import com.securenotes.api.dto.NoteRequest;
import com.securenotes.api.dto.NoteResponse;
import com.securenotes.api.dto.ShareRequest;
import com.securenotes.api.dto.ShareResponse;
import com.securenotes.api.service.NoteService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Context;
import java.net.URI;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Protected note endpoints for authenticated users with the {@code user} role.
 *
 * <p>Ownership and share permissions are enforced by {@link NoteService}; shared
 * notes are read-only for recipients.</p>
 */
@Path("/notes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
@Tag(name = "Notes")
public class NoteResource {

    private final NoteService noteService;

    @Context
    UriInfo uriInfo;

    /**
     * Creates the note resource.
     *
     * @param noteService note service dependency
     */
    public NoteResource(NoteService noteService) {
        this.noteService = noteService;
    }

    /**
     * Creates a note owned by the authenticated user.
     *
     * @param request validated note request
     * @return HTTP 201 response with the created note and location header
     */
    @POST
    @Operation(summary = "Create a note owned by the authenticated user")
    public Response create(@Valid NoteRequest request) {
        NoteResponse response = noteService.create(request);
        URI location = uriInfo.getAbsolutePathBuilder().path(response.id().toString()).build();
        return Response.created(location).entity(response).build();
    }

    /**
     * Lists notes the authenticated user can read.
     *
     * @return owned and explicitly shared notes
     */
    @GET
    @Operation(summary = "List notes owned by or shared with the authenticated user")
    public List<NoteResponse> list() {
        return noteService.listAccessible();
    }

    /**
     * Retrieves a readable note by identifier.
     *
     * @param id note identifier
     * @return note when owned by or shared with the requester
     */
    @GET
    @Path("/{id}")
    @Operation(summary = "Get a note if owned by or shared with the authenticated user")
    public NoteResponse get(@PathParam("id") Long id) {
        return noteService.get(id);
    }

    /**
     * Updates a note owned by the authenticated user.
     *
     * @param id note identifier
     * @param request validated note request
     * @return updated note response
     */
    @PUT
    @Path("/{id}")
    @Operation(summary = "Update a note owned by the authenticated user")
    public NoteResponse update(@PathParam("id") Long id, @Valid NoteRequest request) {
        return noteService.update(id, request);
    }

    /**
     * Deletes a note owned by the authenticated user.
     *
     * @param id note identifier
     * @return HTTP 204 response when deletion succeeds
     */
    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete a note owned by the authenticated user")
    public Response delete(@PathParam("id") Long id) {
        noteService.delete(id);
        return Response.noContent().build();
    }

    /**
     * Shares an owned note with another user as read-only.
     *
     * @param id note identifier
     * @param request validated share request
     * @return HTTP 201 for a new share or HTTP 200 for an existing share
     */
    @POST
    @Path("/{id}/share")
    @Operation(summary = "Share a note as read-only with another user")
    public Response share(@PathParam("id") Long id, @Valid ShareRequest request) {
        NoteService.ShareResult result = noteService.share(id, request);
        Response.ResponseBuilder builder = result.created()
                ? Response.status(Response.Status.CREATED)
                : Response.ok();
        ShareResponse response = result.response();
        return builder.entity(response).build();
    }
}
