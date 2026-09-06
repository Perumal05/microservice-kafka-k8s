package com.shopsphere.notification.service.impl;

import com.shopsphere.notification.dto.request.CreateNotificationRequest;
import com.shopsphere.notification.dto.response.NotificationResponse;
import com.shopsphere.notification.dto.response.PageResponse;
import com.shopsphere.notification.exception.InvalidNotificationStateException;
import com.shopsphere.notification.exception.NotificationSendingException;
import com.shopsphere.notification.exception.ResourceNotFoundException;
import com.shopsphere.notification.mapper.NotificationMapper;
import com.shopsphere.notification.model.entity.Notification;
import com.shopsphere.notification.model.entity.NotificationStatus;
import com.shopsphere.notification.repository.NotificationRepository;
import com.shopsphere.notification.service.NotificationService;
import com.shopsphere.notification.service.sender.NotificationSenderRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationSenderRouter senderRouter;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            NotificationMapper notificationMapper,
            NotificationSenderRouter senderRouter) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
        this.senderRouter = senderRouter;
    }

    @Override
    public NotificationResponse createNotification(CreateNotificationRequest request) {
        Notification notification = notificationMapper.toEntity(request);
        Notification saved = notificationRepository.save(notification);
        log.info("Created notification [id={}] for userId={} via channel={}",
                saved.getId(), saved.getUserId(), saved.getChannel());
        return notificationMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(Long notificationId) {
        Notification notification = findByIdOrThrow(notificationId);
        return notificationMapper.toResponse(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotificationsByUserId(Long userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NotificationResponse> responsePage = notificationRepository
                .findByUserId(userId, pageRequest)
                .map(notificationMapper::toResponse);
        return PageResponse.from(responsePage);
    }

    @Override
    public NotificationResponse sendNotification(Long notificationId, boolean simulateFailure) {
        Notification notification = findByIdOrThrow(notificationId);

        if (notification.getStatus() != NotificationStatus.PENDING) {
            throw new InvalidNotificationStateException(
                    String.format("Cannot send notification with status '%s'. Only PENDING notifications can be sent.",
                            notification.getStatus()));
        }

        try {
            senderRouter.route(notification, simulateFailure);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
            notification.setFailureReason(null);
            log.info("Notification [id={}] sent successfully via channel={}", notificationId, notification.getChannel());
        } catch (NotificationSendingException ex) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailureReason(ex.getMessage());
            log.warn("Notification [id={}] failed to send: {}", notificationId, ex.getMessage());
        }

        Notification updated = notificationRepository.save(notification);
        return notificationMapper.toResponse(updated);
    }

    @Override
    public NotificationResponse cancelNotification(Long notificationId) {
        Notification notification = findByIdOrThrow(notificationId);

        if (notification.getStatus() != NotificationStatus.PENDING) {
            throw new InvalidNotificationStateException(
                    String.format("Cannot cancel notification with status '%s'. Only PENDING notifications can be cancelled.",
                            notification.getStatus()));
        }

        notification.setStatus(NotificationStatus.CANCELLED);
        Notification updated = notificationRepository.save(notification);
        log.info("Notification [id={}] cancelled.", notificationId);
        return notificationMapper.toResponse(updated);
    }

    private Notification findByIdOrThrow(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found with id: " + notificationId));
    }
}
