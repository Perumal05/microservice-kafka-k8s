package com.shopsphere.notification.mapper;

import com.shopsphere.notification.dto.request.CreateNotificationRequest;
import com.shopsphere.notification.dto.response.NotificationResponse;
import com.shopsphere.notification.model.entity.Notification;
import com.shopsphere.notification.model.entity.NotificationChannel;
import com.shopsphere.notification.model.entity.NotificationStatus;
import com.shopsphere.notification.model.entity.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class NotificationMapperTest {

    private NotificationMapper notificationMapper;

    @BeforeEach
    void setUp() {
        notificationMapper = new NotificationMapper();
    }

    @Test
    void toEntity_ShouldMapCreateRequestToNotification() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                10L, NotificationType.PAYMENT_SUCCESS, NotificationChannel.EMAIL,
                "user@example.com", "Payment Success", "Your payment was processed.", null);

        Notification notification = notificationMapper.toEntity(request);

        assertNotNull(notification);
        assertEquals(10L, notification.getUserId());
        assertEquals(NotificationType.PAYMENT_SUCCESS, notification.getType());
        assertEquals(NotificationChannel.EMAIL, notification.getChannel());
        assertEquals("user@example.com", notification.getRecipient());
        assertEquals("Payment Success", notification.getSubject());
        assertEquals("Your payment was processed.", notification.getMessage());
        assertEquals(NotificationStatus.PENDING, notification.getStatus());
    }

    @Test
    void toResponse_ShouldMapNotificationToResponse() {
        Instant now = Instant.now();
        Notification notification = Notification.builder()
                .id(1L)
                .userId(10L)
                .type(NotificationType.ORDER_SHIPPED)
                .channel(NotificationChannel.SMS)
                .recipient("+919876543210")
                .subject(null)
                .message("Your order has shipped!")
                .status(NotificationStatus.SENT)
                .createdAt(now)
                .sentAt(now)
                .build();

        NotificationResponse response = notificationMapper.toResponse(notification);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(10L, response.userId());
        assertEquals(NotificationType.ORDER_SHIPPED, response.type());
        assertEquals(NotificationChannel.SMS, response.channel());
        assertEquals(NotificationStatus.SENT, response.status());
        assertEquals(now, response.sentAt());
        assertNull(response.failureReason());
    }

    @Test
    void toEntity_NullRequest_ShouldReturnNull() {
        assertNull(notificationMapper.toEntity(null));
    }

    @Test
    void toResponse_NullNotification_ShouldReturnNull() {
        assertNull(notificationMapper.toResponse(null));
    }
}
