package com.shopsphere.notification.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shopsphere.notification.dto.request.CreateNotificationRequest;
import com.shopsphere.notification.dto.response.NotificationResponse;
import com.shopsphere.notification.event.OrderCreatedEvent;
import com.shopsphere.notification.model.entity.NotificationChannel;
import com.shopsphere.notification.model.entity.NotificationStatus;
import com.shopsphere.notification.model.entity.NotificationType;
import com.shopsphere.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderEventNotificationConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private NotificationService notificationService;
    private OrderEventNotificationConsumer consumer;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        consumer = new OrderEventNotificationConsumer(objectMapper, notificationService);
    }

    @Test
    void onMessage_OrderCreated_CreatesAndSendsNotification() throws Exception {
        when(notificationService.createNotification(any(CreateNotificationRequest.class)))
                .thenReturn(new NotificationResponse(1L, 100L, NotificationType.ORDER_CREATED, NotificationChannel.EMAIL,
                        "user-100@shopsphere.local", "subject", "message", NotificationStatus.PENDING, Instant.now(), null, null));

        OrderCreatedEvent event = new OrderCreatedEvent("evt-1", "OrderCreated", 1, Instant.now(), "corr-1",
                55L, "ORD-55", 100L, "USD", new BigDecimal("99.99"), "CARD",
                List.of(new OrderCreatedEvent.Item(10L, "SKU-1", 1, new BigDecimal("99.99"))));

        consumer.onMessage(objectMapper.writeValueAsString(event));

        ArgumentCaptor<CreateNotificationRequest> captor = ArgumentCaptor.forClass(CreateNotificationRequest.class);
        verify(notificationService).createNotification(captor.capture());
        assertEquals(100L, captor.getValue().userId());
        assertEquals(NotificationType.ORDER_CREATED, captor.getValue().type());
        assertEquals(NotificationChannel.EMAIL, captor.getValue().channel());
        assertTrue(captor.getValue().message().contains("ORD-55"));

        verify(notificationService).sendNotification(eq(1L), eq(false));
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
