package com.notification.notification.controller;

import com.notification.common.dto.ApiResponse;
import com.notification.common.dto.PageResponse;
import com.notification.notification.dto.NotificationStatusResponse;
import com.notification.notification.dto.SendNotificationRequest;
import com.notification.notification.dto.SendNotificationResponse;
import com.notification.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<ApiResponse<SendNotificationResponse>> sendNotification(
            @Valid @RequestBody SendNotificationRequest request) {
        SendNotificationResponse response = notificationService.sendNotification(request);
        return new ResponseEntity<>(ApiResponse.success(response, "Notification accepted for processing"), HttpStatus.ACCEPTED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationStatusResponse>> getNotificationStatus(@PathVariable String id) {
        NotificationStatusResponse response = notificationService.getNotificationStatus(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/correlation/{correlationId}")
    public ResponseEntity<ApiResponse<NotificationStatusResponse>> getNotificationByCorrelationId(@PathVariable String correlationId) {
        NotificationStatusResponse response = notificationService.getNotificationStatusByCorrelationId(correlationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationStatusResponse>>> searchNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<NotificationStatusResponse> response = notificationService.searchNotifications(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
