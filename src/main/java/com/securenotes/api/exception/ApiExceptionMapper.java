package com.securenotes.api.exception;

import com.securenotes.api.dto.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.Instant;
import java.util.List;

/**
 * Converts application domain exceptions into consistent JSON error responses.
 */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

    /**
     * Maps a domain exception to its configured HTTP response.
     *
     * @param exception application exception with a response status
     * @return JSON error response
     */
    @Override
    public Response toResponse(ApiException exception) {
        Response.Status status = exception.status();
        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                status.getStatusCode(),
                status.getReasonPhrase(),
                List.of(exception.getMessage()));
        return Response.status(status).entity(response).build();
    }
}
