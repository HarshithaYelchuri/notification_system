package com.notification.audit.service;

import com.notification.audit.domain.AuditLog;
import com.notification.audit.repository.AuditLogRepository;
import com.notification.publisher.event.NotificationAuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void saveAuditLog(NotificationAuditEvent event) {
        try {
            AuditLog logEntity = AuditLog.builder()
                    .tenantId(event.getTenantId())
                    .notificationId(event.getNotificationId())
                    .correlationId(event.getCorrelationId())
                    .notificationType(event.getNotificationType())
                    .status(event.getStatus())
                    .recipient(event.getRecipient())
                    .details(event.getDetails())
                    .errorMessage(event.getErrorMessage())
                    .retryCount(event.getRetryCount())
                    .timestamp(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now())
                    .build();

            auditLogRepository.save(logEntity);
            log.debug("Persisted audit record for notification [id={}, status={}]", event.getNotificationId(), event.getStatus());
        } catch (Exception e) {
            log.error("Failed to persist audit log record: {}", e.getMessage(), e);
        }
    }
}
