package com.shopsphere.order.filter;

/**
 * Holds the correlation ID for the current request thread so that outbound
 * REST client calls can propagate it without threading it through every
 * method signature.
 * <p>
 * This is intentionally a simple, temporary mechanism. It will be replaced
 * or complemented by OpenTelemetry trace-context propagation in a later
 * stage - see the class-level comment on {@link CorrelationIdFilter}.
 */
public final class CorrelationIdHolder {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private CorrelationIdHolder() {
    }

    public static void set(String correlationId) {
        CURRENT.set(correlationId);
    }

    public static String get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
