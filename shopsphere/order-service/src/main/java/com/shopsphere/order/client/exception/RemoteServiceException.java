package com.shopsphere.order.client.exception;

/**
 * Base, unchecked exception type for failures that occur while calling
 * another microservice over HTTP.
 * <p>
 * Client implementations translate low-level {@code RestClient} exceptions
 * (status codes, connection failures, timeouts) into this hierarchy so that
 * controllers and orchestration logic never have to deal with raw HTTP
 * client exceptions directly.
 */
public class RemoteServiceException extends RuntimeException {

    private final String serviceName;
    private final Integer statusCode;

    public RemoteServiceException(String serviceName, String message) {
        this(serviceName, null, message, null);
    }

    public RemoteServiceException(String serviceName, String message, Throwable cause) {
        this(serviceName, null, message, cause);
    }

    public RemoteServiceException(String serviceName, Integer statusCode, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
        this.statusCode = statusCode;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
