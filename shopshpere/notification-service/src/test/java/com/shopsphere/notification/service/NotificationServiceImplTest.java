package com.shopsphere.notification.service;

import com.shopsphere.notification.dto.request.CreateNotificationRequest;
import com.shopsphere.notification.dto.response.NotificationResponse;
import com.shopsphere.notification.dto.response.PageResponse;
import com.shopsphere.notification.exception.InvalidNotificationStateException;
import com.shopsphere.notification.exception.ResourceNotFoundException;
import com.shopsphere.notification.mapper.NotificationMapper;
import com.shopsphere.notification.model.entity.Notification;
import com.shopsphere.notification.model.entity.NotificationChannel;
import com.shopsphere.notification.model.entity.NotificationStatus;
import com.shopsphere.notification.model.entity.NotificationType;
import com.shopsphere.notification.repository.NotificationRepository;
import com.shopsphere.notification.service.impl.NotificationServiceImpl;
import com.shopsphere.notification.service.sender.NotificationSenderRouter;
import com.shopsphere.notification.service.sender.NotificationSenderRouterImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceImplTest {

    private NotificationRepository notificationRepository;
    private NotificationMapper notificationMapper;
    private NotificationSenderRouter senderRouter;
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        notificationMapper = new NotificationMapper();
        senderRouter = mock(NotificationSenderRouter.class);
        notificationService = new NotificationServiceImpl(notificationRepository, notificationMapper, senderRouter);
    }

    private Notification buildNotification(Long id, NotificationStatus status) {
        return Notification.builder()
                .id(id)
                .userId(10L)
                .type(NotificationType.ORDER_CREATED)
                .channel(NotificationChannel.EMAIL)
                .recipient("user@example.com")
                .subject("Order Created")
                .message("Your order has been created.")
                .status(status)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void createNotification_ShouldReturnPendingNotification() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                10L, NotificationType.ORDER_CREATED, NotificationChannel.EMAIL,
                "user@example.com", "Order Created", "Your order has been created.", null);

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            n.setCreatedAt(Instant.now());
            return n;
        });

        NotificationResponse response = notificationService.createNotification(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(NotificationStatus.PENDING, response.status());
        assertEquals(NotificationChannel.EMAIL, response.channel());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void getNotificationById_ShouldReturnNotification() {
        Notification notification = buildNotification(1L, NotificationStatus.PENDING);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        NotificationResponse response = notificationService.getNotificationById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("user@example.com", response.recipient());
    }

    @Test
    void getNotificationById_NotFound_ShouldThrowResourceNotFoundException() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.getNotificationById(999L));
    }

    @Test
    void getNotificationsByUserId_ShouldReturnPagedResults() {
        Notification notification = buildNotification(1L, NotificationStatus.SENT);
        when(notificationRepository.findByUserId(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notification)));

        PageResponse<NotificationResponse> page = notificationService.getNotificationsByUserId(10L, 0, 10);

        assertNotNull(page);
        assertEquals(1, page.content().size());
        assertEquals(1L, page.content().get(0).id());
    }

    @Test
    void sendNotification_Success_ShouldTransitionToSent() {
        Notification notification = buildNotification(1L, NotificationStatus.PENDING);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(senderRouter).route(any(Notification.class), eq(false));

        NotificationResponse response = notificationService.sendNotification(1L, false);

        assertEquals(NotificationStatus.SENT, response.status());
        assertNotNull(response.sentAt());
        assertNull(response.failureReason());
        verify(senderRouter).route(any(Notification.class), eq(false));
    }

    @Test
    void sendNotification_SimulateFailure_ShouldTransitionToFailed() {
        Notification notification = buildNotification(1L, NotificationStatus.PENDING);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new com.shopsphere.notification.exception.NotificationSendingException("Simulated EMAIL delivery failure"))
                .when(senderRouter).route(any(Notification.class), eq(true));

        NotificationResponse response = notificationService.sendNotification(1L, true);

        assertEquals(NotificationStatus.FAILED, response.status());
        assertNotNull(response.failureReason());
        assertTrue(response.failureReason().contains("Simulated EMAIL delivery failure"));
    }

    @Test
    void sendNotification_AlreadySent_ShouldThrowInvalidNotificationStateException() {
        Notification notification = buildNotification(1L, NotificationStatus.SENT);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThrows(InvalidNotificationStateException.class, () -> notificationService.sendNotification(1L, false));
    }

    @Test
    void sendNotification_Cancelled_ShouldThrowInvalidNotificationStateException() {
        Notification notification = buildNotification(1L, NotificationStatus.CANCELLED);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThrows(InvalidNotificationStateException.class, () -> notificationService.sendNotification(1L, false));
    }

    @Test
    void cancelNotification_Pending_ShouldTransitionToCancelled() {
        Notification notification = buildNotification(1L, NotificationStatus.PENDING);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse response = notificationService.cancelNotification(1L);

        assertEquals(NotificationStatus.CANCELLED, response.status());
    }

    @Test
    void cancelNotification_AlreadySent_ShouldThrowInvalidNotificationStateException() {
        Notification notification = buildNotification(1L, NotificationStatus.SENT);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThrows(InvalidNotificationStateException.class, () -> notificationService.cancelNotification(1L));
    }

    @Test
    void cancelNotification_AlreadyFailed_ShouldThrowInvalidNotificationStateException() {
        Notification notification = buildNotification(1L, NotificationStatus.FAILED);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThrows(InvalidNotificationStateException.class, () -> notificationService.cancelNotification(1L));
    }
}
