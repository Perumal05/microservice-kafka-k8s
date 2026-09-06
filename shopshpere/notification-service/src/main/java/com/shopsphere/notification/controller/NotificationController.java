package com.shopsphere.notification.controller;

import com.shopsphere.notification.dto.request.CreateNotificationRequest;
import com.shopsphere.notification.dto.response.NotificationResponse;
import com.shopsphere.notification.dto.response.PageResponse;
import com.shopsphere.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notification Controller", description = "APIs for creating and dispatching multi-channel notifications (EMAIL, SMS, PUSH)")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    @Operation(summary = "Create a new notification in PENDING state")
    public ResponseEntity<NotificationResponse> createNotification(
            @Valid @RequestBody CreateNotificationRequest request) {
        NotificationResponse response = notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{notificationId}")
    @Operation(summary = "Get notification by ID")
    public ResponseEntity<NotificationResponse> getNotificationById(
            @PathVariable Long notificationId) {
        return ResponseEntity.ok(notificationService.getNotificationById(notificationId));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get paginated notification history for a user")
    public ResponseEntity<PageResponse<NotificationResponse>> getNotificationsByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.getNotificationsByUserId(userId, page, size));
    }

    @PostMapping("/{notificationId}/send")
    @Operation(summary = "Simulate sending a PENDING notification. Pass simulateFailure=true to test failure path.")
    public ResponseEntity<NotificationResponse> sendNotification(
            @PathVariable Long notificationId,
            @RequestParam(defaultValue = "false") boolean simulateFailure) {
        return ResponseEntity.ok(notificationService.sendNotification(notificationId, simulateFailure));
    }

    @PostMapping("/{notificationId}/cancel")
    @Operation(summary = "Cancel a PENDING notification")
    public ResponseEntity<NotificationResponse> cancelNotification(
            @PathVariable Long notificationId) {
        return ResponseEntity.ok(notificationService.cancelNotification(notificationId));
    }
}
