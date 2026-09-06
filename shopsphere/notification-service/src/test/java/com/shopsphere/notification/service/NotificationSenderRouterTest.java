package com.shopsphere.notification.service;

import com.shopsphere.notification.exception.NotificationSendingException;
import com.shopsphere.notification.model.entity.Notification;
import com.shopsphere.notification.model.entity.NotificationChannel;
import com.shopsphere.notification.model.entity.NotificationStatus;
import com.shopsphere.notification.model.entity.NotificationType;
import com.shopsphere.notification.service.sender.EmailNotificationSender;
import com.shopsphere.notification.service.sender.NotificationSenderRouterImpl;
import com.shopsphere.notification.service.sender.PushNotificationSender;
import com.shopsphere.notification.service.sender.SmsNotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotificationSenderRouterTest {

    private NotificationSenderRouterImpl router;

    @BeforeEach
    void setUp() {
        router = new NotificationSenderRouterImpl(List.of(
                new EmailNotificationSender(),
                new SmsNotificationSender(),
                new PushNotificationSender()
        ));
    }

    private Notification buildNotification(NotificationChannel channel) {
        return Notification.builder()
                .id(1L)
                .userId(10L)
                .type(NotificationType.PAYMENT_SUCCESS)
                .channel(channel)
                .recipient("user@example.com")
                .subject("Payment")
                .message("Your payment was successful.")
                .status(NotificationStatus.PENDING)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void route_EmailChannel_ShouldSucceed() {
        Notification n = buildNotification(NotificationChannel.EMAIL);
        assertDoesNotThrow(() -> router.route(n, false));
    }

    @Test
    void route_SmsChannel_ShouldSucceed() {
        Notification n = buildNotification(NotificationChannel.SMS);
        n.setRecipient("+919876543210");
        assertDoesNotThrow(() -> router.route(n, false));
    }

    @Test
    void route_PushChannel_ShouldSucceed() {
        Notification n = buildNotification(NotificationChannel.PUSH);
        n.setRecipient("device-token-abc123");
        assertDoesNotThrow(() -> router.route(n, false));
    }

    @Test
    void route_EmailChannel_SimulateFailure_ShouldThrowNotificationSendingException() {
        Notification n = buildNotification(NotificationChannel.EMAIL);
        assertThrows(NotificationSendingException.class, () -> router.route(n, true));
    }

    @Test
    void route_SmsChannel_SimulateFailure_ShouldThrowNotificationSendingException() {
        Notification n = buildNotification(NotificationChannel.SMS);
        n.setRecipient("+919876543210");
        assertThrows(NotificationSendingException.class, () -> router.route(n, true));
    }

    @Test
    void route_PushChannel_SimulateFailure_ShouldThrowNotificationSendingException() {
        Notification n = buildNotification(NotificationChannel.PUSH);
        n.setRecipient("device-token-abc123");
        assertThrows(NotificationSendingException.class, () -> router.route(n, true));
    }
}
