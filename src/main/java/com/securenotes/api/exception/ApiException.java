package com.securenotes.api.exception;

import jakarta.ws.rs.core.Response;

/**
 * Domain exception that carries the HTTP status code the API should return.
 */
public class ApiException extends RuntimeException {

    private final Response.Status status;

    /**
     * Creates an API exception with an explicit HTTP status and message.
     *
     * @param status HTTP status to return to the client
     * @param message client-safe error message
     */
    public ApiException(Response.Status status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * Returns the HTTP status associated with this exception.
     *
     * @return response status
     */
    public Response.Status status() {
        return status;
    }
}
