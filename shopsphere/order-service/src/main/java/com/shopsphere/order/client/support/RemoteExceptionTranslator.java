package com.shopsphere.order.client.support;

import com.shopsphere.order.client.exception.RemoteResourceNotFoundException;
import com.shopsphere.order.client.exception.RemoteServiceBadRequestException;
import com.shopsphere.order.client.exception.RemoteServiceException;
import com.shopsphere.order.client.exception.RemoteServiceTimeoutException;
import com.shopsphere.order.client.exception.RemoteServiceUnavailableException;
import org.springframework.http.HttpStatusCode;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Central place that maps low-level HTTP/connection failures onto the
 * client-facing {@link RemoteServiceException} hierarchy, so every
 * {@code Rest*ServiceClient} implementation handles errors consistently.
 */
public final class RemoteExceptionTranslator {

    private RemoteExceptionTranslator() {
    }

    /**
     * Builds the appropriate exception for a non-2xx HTTP response.
     */
    public static RemoteServiceException fromStatus(String serviceName, HttpStatusCode status, String responseBody) {
        int code = status.value();
        String message = "%s responded with HTTP %d: %s".formatted(serviceName, code, safeBody(responseBody));

        if (code == 404) {
            return new RemoteResourceNotFoundException(serviceName, message);
        }
        if (status.is4xxClientError()) {
            return new RemoteServiceBadRequestException(serviceName, code, message);
        }
        // 5xx and anything else unexpected
        return new RemoteServiceException(serviceName, code, message, null);
    }

    /**
     * Builds the appropriate exception when the request never received a
     * response at all (connection refused, DNS failure, or a timeout while
     * connecting/reading).
     */
    public static RemoteServiceException fromIoFailure(String serviceName, Throwable cause) {
        if (isTimeout(cause)) {
            return new RemoteServiceTimeoutException(serviceName, serviceName + " did not respond in time", cause);
        }
        return new RemoteServiceUnavailableException(serviceName, serviceName + " is unreachable", cause);
    }

    private static boolean isTimeout(Throwable cause) {
        Throwable current = cause;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean isConnectFailure(Throwable cause) {
        Throwable current = cause;
        while (current != null) {
            if (current instanceof ConnectException || current instanceof UnknownHostException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String safeBody(String body) {
        if (body == null || body.isBlank()) {
            return "(empty body)";
        }
        return body.length() > 300 ? body.substring(0, 300) + "..." : body;
    }
}
