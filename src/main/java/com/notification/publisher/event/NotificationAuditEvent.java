package com.notification.publisher.event;

import com.notification.notification.domain.NotificationStatus;
import com.notification.notification.domain.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationAuditEvent {

    private String notificationId;
    private String correlationId;
    private String tenantId;
    private String templateId;
    private NotificationType notificationType;
    private NotificationStatus status;
    private String recipient;
    private String details;
    private String errorMessage;
    private int retryCount;
    private LocalDateTime timestamp;
}
