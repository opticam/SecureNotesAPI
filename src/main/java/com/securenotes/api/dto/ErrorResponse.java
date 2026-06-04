package com.securenotes.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * Consistent JSON error payload returned by API exception mappers.
 *
 * @param timestamp time when the API produced the error response
 * @param status HTTP status code
 * @param error HTTP reason phrase
 * @param details human-readable validation or domain error messages
 */
public record ErrorResponse(Instant timestamp, int status, String error, List<String> details) {
}
