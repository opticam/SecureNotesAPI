package com.securenotes.api.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Instant;

/**
 * Centralized security audit logger.
 *
 * <p>Implements the content and accountability requirements of the AU family:</p>
 * <ul>
 *   <li><b>AU-2</b> — auditable events: authn outcomes and access decisions.</li>
 *   <li><b>AU-3</b> — record content: <i>what</i> happened, <i>when</i> (UTC),
 *       <i>who</i> (subject), <i>where</i> (source address + target), and the
 *       <i>outcome</i>. A correlation id ties multi-line activity together.</li>
 *   <li><b>AU-8</b> — timestamps are UTC ISO-8601 ({@link Instant}).</li>
 *   <li><b>AU-9</b> — records go to a dedicated "AUDIT" category so deployment
 *       can route them to a protected, append-only sink the app cannot rewrite.</li>
 * </ul>
 *
 * <p><b>Do not</b> place secrets, full tokens, passwords, or unnecessary PII in
 * audit records (AU-3 / privacy). We log the subject identifier only.</p>
 */
@ApplicationScoped
public class AuditLoggerService {

    // Separate, independently-routable category (configured in application.properties).
    private static final Logger AUDIT = Logger.getLogger("AUDIT");

    /**
     * Emit one structured audit record. Fields are escaped to keep the record
     * single-line and tamper-resistant (no injected newlines from input -> log
     * forging defense, supporting AU-9).
     *
     * @param eventType eventType for audit record
     * @param subject the user making the request
     * @param sourceAddress IP address of the request
     * @param target the endpoint or action requested
     * @param outcome the result of the request
     * @param correlationId id of the session to tie requests together
     */
     public void record(String eventType,
                       String subject,
                       String sourceAddress,
                       String target,
                       String outcome,
                       String correlationId) {

        AUDIT.infof("event=%s ts=%s subject=%s src=%s target=%s outcome=%s corr=%s",
                sanitizeForAudit(eventType),
                Instant.now(),
                sanitizeForAudit(subject),
                sanitizeForAudit(sourceAddress),
                sanitizeForAudit(target),
                sanitizeForAudit(outcome),
                sanitizeForAudit(correlationId));
    }

    /**
     * Strip CR/LF and control characters so attacker-controlled values (e.g. a
     * crafted principal name) cannot forge additional log lines. Defends the
     * integrity of audit records (AU-9).
     *
     * @param value raw audit field value
     * @return cleaned string
     */
    static String sanitizeForAudit(String value) {
        if (value == null) {
            return "-";
        }
        return value.replaceAll("[\\r\\n\\t\\p{Cntrl}]", "_");
    }
}
