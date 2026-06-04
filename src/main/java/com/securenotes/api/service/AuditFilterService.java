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

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // One id per request, used to correlate the (later) response record and
        // any handler-emitted records. Honor an inbound id only if you trust the
        // upstream proxy; here we always mint our own to avoid spoofed ids.
        requestContext.setProperty(CORRELATION_PROP, UUID.randomUUID().toString());
    }

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

    /** Non-sensitive subject identifier, or "anonymous" when unauthenticated. */
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
     * Best-effort client address. With proxy-address-forwarding disabled
     * (default), this is the immediate peer; behind a TRUSTED proxy you would
     * enable forwarding so this reflects the real client (AU-3 accuracy).
     */
    private String sourceAddress() {
        try {
            return routingContext.request().remoteAddress().hostAddress();
        } catch (RuntimeException e) {
            return "-";
        }
    }
}
