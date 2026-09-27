package com.notification.publisher.event;

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
public class NotificationEnrichedEvent {

    private String notificationId;
    private String correlationId;
    private String tenantId;
    private String templateId;
    private NotificationType notificationType;
    private String recipient;
    private String renderedContent;
    private int retryCount;
    private LocalDateTime timestamp;
}
