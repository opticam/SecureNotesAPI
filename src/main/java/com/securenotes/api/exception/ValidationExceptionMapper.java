package com.securenotes.api.exception;

import com.securenotes.api.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Converts bean-validation failures into predictable HTTP 400 responses.
 */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    /**
     * Maps validation constraint violations to a JSON error payload.
     *
     * @param exception validation exception raised by Jakarta Validation
     * @return HTTP 400 response containing validation details
     */
    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<String> details = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .sorted(Comparator.naturalOrder())
                .toList();
        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                Response.Status.BAD_REQUEST.getStatusCode(),
                Response.Status.BAD_REQUEST.getReasonPhrase(),
                details);
        return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
    }
}
