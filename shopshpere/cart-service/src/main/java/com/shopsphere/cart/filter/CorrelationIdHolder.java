package com.shopsphere.cart.filter;

/**
 * Holds the correlation ID for the current request thread so outbound calls
 * (if any are added later) can propagate it without threading it through
 * every method signature.
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
