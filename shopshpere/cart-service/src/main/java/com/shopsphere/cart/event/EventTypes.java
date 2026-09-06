package com.shopsphere.cart.event;

/**
 * String values used in the {@code eventType} field of every event JSON
 * payload. Consumers branch on these values, not on Java class names.
 * <p>
 * Cart Service only cares about {@link #PAYMENT_SUCCEEDED} in this stage -
 * the other constants are listed for completeness/documentation of what
 * else can appear on {@link KafkaTopics#PAYMENT_EVENTS}, even though this
 * service ignores them.
 */
public final class EventTypes {

    public static final String PAYMENT_SUCCEEDED = "PaymentSucceeded";
    public static final String PAYMENT_FAILED = "PaymentFailed";

    private EventTypes() {
    }
}
