package com.securenotes.api.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AuditLoggerServiceTest {

    @Test
    void sanitizerReplacesUnsafeAuditCharacters() {
        assertEquals("user_name_bad_value", AuditLoggerService.sanitizeForAudit("user\nname\tbad\rvalue"));
    }

    @Test
    void recordAcceptsNullFieldsWithoutThrowing() {
        AuditLoggerService loggerService = new AuditLoggerService();

        assertDoesNotThrow(() -> loggerService.record(null, null, null, null, null, null));
    }
}
