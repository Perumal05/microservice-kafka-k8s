package com.shopsphere.order.client.exception;

/**
 * Thrown when a remote service cannot be reached at all (connection refused,
 * DNS failure, connect timeout, etc). Distinct from a timeout that occurs
 * after the connection has been established.
 */
public class RemoteServiceUnavailableException extends RemoteServiceException {
    public RemoteServiceUnavailableException(String serviceName, String message, Throwable cause) {
        super(serviceName, message, cause);
    }
}
