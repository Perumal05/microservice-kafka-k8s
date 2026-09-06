package com.shopsphere.order.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Assigns a correlation ID to every incoming request and makes it available
 * for the duration of request processing.
 * <p>
 * If the caller already sent an {@code X-Correlation-ID} header, that value
 * is reused; otherwise a new UUID is generated. The value is always echoed
 * back on the response, and propagated on any outbound REST calls made
 * while handling the request (see the RestClient interceptor in
 * {@code RestClientConfig}).
 * <p>
 * This is a deliberately minimal, temporary mechanism. It is NOT a
 * replacement for distributed tracing - OpenTelemetry will be introduced in
 * a dedicated observability stage and will likely supersede this filter.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        CorrelationIdHolder.set(correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            CorrelationIdHolder.clear();
        }
    }
}
