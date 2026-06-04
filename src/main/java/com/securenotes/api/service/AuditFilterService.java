package com.securenotes.api.service;

import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.UUID;

/**
 * Cross-cutting audit filter. Runs for EVERY request, so audit coverage cannot
 * be forgotten on an individual endpoint.
 *
 * <ul>
 *   <li><b>AU-2 / AU-12</b> — generates an audit record for each access attempt
 *       and its outcome (HTTP status), regardless of success or failure.</li>
 *   <li><b>AU-3</b> — supplies subject, source, target, outcome, correlation id.</li>
 *   <li><b>AU-4 / incident response (IR-4)</b> — the correlation id is echoed
 *       to the client in {@code X-Correlation-Id} so a caller's report can be
 *       tied back to server records.</li>
 * </ul>
 *
 * <p>Note: the JWT principal is request-scoped; CDI injects a context-aware
 * proxy, which is safe to hold in this application-scoped provider.</p>
 */
@Provider
@ApplicationScoped
public class AuditFilterService implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String CORRELATION_PROP = "audit.correlationId";
    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    @Inject
    AuditLoggerService auditLogger;

    @Inject
    JsonWebToken jwt;

    @Inject
    RoutingContext routingContext;

    /**
     * Assigns a new server-generated correlation identifier to the inbound request.
     *
     * <p>The filter deliberately ignores any caller-supplied correlation value so
     * external clients cannot spoof identifiers that appear in audit records.</p>
     *
     * @param requestContext request metadata and per-request properties
     */
    @Override
    public void filter(ContainerRequestContext requestContext) {
        // One id per request, used to correlate the (later) response record and
        // any handler-emitted records. Honor an inbound id only if you trust the
        // upstream proxy; here we always mint our own to avoid spoofed ids.
        requestContext.setProperty(CORRELATION_PROP, UUID.randomUUID().toString());
    }

    /**
     * Emits a structured audit record for the completed request and echoes the
     * correlation identifier to the client.
     *
     * <p>The record includes subject, source, target, outcome, event type, and
     * correlation id. HTTP {@code 401} and {@code 403} responses are classified as
     * {@code ACCESS_DENIED}; all other responses are classified as
     * {@code API_ACCESS}.</p>
     *
     * @param requestContext request metadata captured by JAX-RS
     * @param responseContext response metadata, including final HTTP status
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) {
        String correlationId = String.valueOf(requestContext.getProperty(CORRELATION_PROP));
        int status = responseContext.getStatus();

        // AU-3: derive the record fields.
        String subject = subject();
        String target = requestContext.getMethod() + " " + requestContext.getUriInfo().getPath();
        String source = sourceAddress();
        String outcome = (status < 400 ? "SUCCESS" : "DENIED_OR_ERROR") + ":" + status;
        String eventType = (status == 401 || status == 403) ? "ACCESS_DENIED" : "API_ACCESS";

        auditLogger.record(eventType, subject, source, target, outcome, correlationId);

        // Echo correlation id for client-side traceability (IR-4 support).
        responseContext.getHeaders().putSingle(CORRELATION_HEADER, correlationId);
    }

    /**
     * Resolves a non-sensitive subject identifier for the audit record.
     *
     * @return JWT principal name, or {@code anonymous} when unauthenticated or unavailable
     */
    private String subject() {
        try {
            String name = jwt.getName();
            return (name == null || name.isBlank()) ? "anonymous" : name;
        } catch (RuntimeException e) {
            // No active token on this request.
            return "anonymous";
        }
    }

    /**
     * Resolves the best-effort client source address.
     *
     * <p>With proxy address forwarding disabled, this is the immediate peer. In a
     * production deployment behind a trusted proxy, forwarding should be enabled
     * so the audit source reflects the real client address.</p>
     *
     * @return remote host address, or {@code -} when unavailable
     */
    private String sourceAddress() {
        try {
            return routingContext.request().remoteAddress().hostAddress();
        } catch (RuntimeException e) {
            return "-";
        }
    }
}
