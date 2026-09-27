package com.notification.publisher.event;

import com.notification.notification.domain.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationIngestEvent {

    private String notificationId;
    private String correlationId;
    private String tenantId;
    private String templateId;
    private NotificationType notificationType;
    private String recipient;
    private Map<String, Object> dynamicVariables;
    private LocalDateTime timestamp;
}
