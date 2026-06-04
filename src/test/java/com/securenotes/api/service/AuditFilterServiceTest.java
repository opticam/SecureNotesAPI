package com.securenotes.api.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.net.SocketAddress;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;

class AuditFilterServiceTest {

    @Test
    void responseFilterRecordsDeniedAuditEventAndEchoesCorrelationId() {
        AuditFilterService filterService = new AuditFilterService();
        filterService.auditLogger = mock(AuditLoggerService.class);
        filterService.jwt = mock(JsonWebToken.class);
        filterService.routingContext = mock(RoutingContext.class);

        ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        HttpServerRequest request = mock(HttpServerRequest.class);
        SocketAddress remoteAddress = mock(SocketAddress.class);
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        String correlationId = "corr-123";

        when(requestContext.getProperty("audit.correlationId")).thenReturn(correlationId);
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("notes/1");
        when(responseContext.getStatus()).thenReturn(403);
        when(responseContext.getHeaders()).thenReturn(headers);
        when(filterService.jwt.getName()).thenReturn("alice");
        when(filterService.routingContext.request()).thenReturn(request);
        when(request.remoteAddress()).thenReturn(remoteAddress);
        when(remoteAddress.hostAddress()).thenReturn("127.0.0.1");

        filterService.filter(requestContext, responseContext);

        verify(filterService.auditLogger).record(
                eq("ACCESS_DENIED"),
                eq("alice"),
                eq("127.0.0.1"),
                eq("GET notes/1"),
                eq("DENIED_OR_ERROR:403"),
                eq(correlationId));
        assertNotNull(headers.getFirst("X-Correlation-Id"));
    }

    @Test
    void responseFilterFallsBackForAnonymousSubjectAndUnknownSource() {
        AuditFilterService filterService = new AuditFilterService();
        filterService.auditLogger = mock(AuditLoggerService.class);
        filterService.jwt = mock(JsonWebToken.class);
        filterService.routingContext = mock(RoutingContext.class);

        ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        String correlationId = "corr-456";

        when(requestContext.getProperty("audit.correlationId")).thenReturn(correlationId);
        when(requestContext.getMethod()).thenReturn("POST");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("auth/login");
        when(responseContext.getStatus()).thenReturn(200);
        when(responseContext.getHeaders()).thenReturn(headers);
        when(filterService.jwt.getName()).thenThrow(new IllegalStateException("no token"));
        when(filterService.routingContext.request()).thenThrow(new IllegalStateException("no routing context"));

        filterService.filter(requestContext, responseContext);

        verify(filterService.auditLogger).record(
                eq("API_ACCESS"),
                eq("anonymous"),
                eq("-"),
                eq("POST auth/login"),
                eq("SUCCESS:200"),
                eq(correlationId));
        assertNotNull(headers.getFirst("X-Correlation-Id"));
    }
}
