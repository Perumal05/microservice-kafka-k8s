package com.shopsphere.cart.filter;

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
 * Assigns a correlation ID to every incoming request and echoes it back on
 * the response. If the caller already sent an {@code X-Correlation-ID}
 * header, that value is reused; otherwise a new UUID is generated.
 * <p>
 * This is a deliberately minimal, temporary mechanism (stage 3 of the
 * ShopSphere build-out). It will be replaced or complemented by
 * OpenTelemetry trace-context propagation in a dedicated observability
 * stage - do not extend this into a custom tracing system.
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
