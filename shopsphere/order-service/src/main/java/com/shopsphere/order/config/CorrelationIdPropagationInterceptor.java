package com.shopsphere.order.config;

import com.shopsphere.order.filter.CorrelationIdFilter;
import com.shopsphere.order.filter.CorrelationIdHolder;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Propagates the inbound request's {@code X-Correlation-ID} onto every
 * outbound RestClient call, so downstream services can correlate log lines
 * for a single logical request across service boundaries.
 */
public class CorrelationIdPropagationInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        String correlationId = CorrelationIdHolder.get();
        if (correlationId != null && !correlationId.isBlank()) {
            request.getHeaders().set(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId);
        }
        return execution.execute(request, body);
    }
}
