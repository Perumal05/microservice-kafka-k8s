package com.shopsphere.notification.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shopsphere.notification.dto.request.CreateNotificationRequest;
import com.shopsphere.notification.dto.response.NotificationResponse;
import com.shopsphere.notification.event.PaymentFailedEvent;
import com.shopsphere.notification.event.PaymentSucceededEvent;
import com.shopsphere.notification.model.entity.NotificationChannel;
import com.shopsphere.notification.model.entity.NotificationStatus;
import com.shopsphere.notification.model.entity.NotificationType;
import com.shopsphere.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentEventNotificationConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private NotificationService notificationService;
    private PaymentEventNotificationConsumer consumer;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        consumer = new PaymentEventNotificationConsumer(objectMapper, notificationService);
        when(notificationService.createNotification(any(CreateNotificationRequest.class)))
                .thenReturn(new NotificationResponse(1L, 100L, NotificationType.PAYMENT_SUCCESS, NotificationChannel.EMAIL,
                        "user-100@shopsphere.local", "s", "m", NotificationStatus.PENDING, Instant.now(), null, null));
    }

    @Test
    void onMessage_PaymentSucceeded_CreatesPaymentSuccessNotification() throws Exception {
        PaymentSucceededEvent event = new PaymentSucceededEvent("evt-1", "PaymentSucceeded", 1, Instant.now(), "corr-1",
                55L, "ORD-55", 100L, 900L, new BigDecimal("99.99"), "USD");

        consumer.onMessage(objectMapper.writeValueAsString(event));

        ArgumentCaptor<CreateNotificationRequest> captor = ArgumentCaptor.forClass(CreateNotificationRequest.class);
        verify(notificationService).createNotification(captor.capture());
        assertEquals(100L, captor.getValue().userId());
        assertEquals(NotificationType.PAYMENT_SUCCESS, captor.getValue().type());
        verify(notificationService).sendNotification(eq(1L), eq(false));
    }

    @Test
    void onMessage_PaymentFailed_CreatesPaymentFailedNotification() throws Exception {
        PaymentFailedEvent event = new PaymentFailedEvent("evt-2", "PaymentFailed", 1, Instant.now(), "corr-2",
                56L, "ORD-56", 100L, 901L, "Card declined");

        consumer.onMessage(objectMapper.writeValueAsString(event));

        ArgumentCaptor<CreateNotificationRequest> captor = ArgumentCaptor.forClass(CreateNotificationRequest.class);
        verify(notificationService).createNotification(captor.capture());
        assertEquals(NotificationType.PAYMENT_FAILED, captor.getValue().type());
        assertTrue(captor.getValue().message().contains("Card declined"));
    }

    @Test
    void onMessage_UnrecognizedEventType_IsIgnored() {
        assertDoesNotThrow(() -> consumer.onMessage("{\"eventType\":\"Unknown\"}"));
        verifyNoInteractions(notificationService);
    }

    @Test
    void onMessage_MalformedJson_IsLoggedNotThrown() {
        assertDoesNotThrow(() -> consumer.onMessage("not json"));
        verifyNoInteractions(notificationService);
    }
}
