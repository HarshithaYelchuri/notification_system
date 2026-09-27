package com.notification.audit.consumer;

import com.notification.audit.service.AuditService;
import com.notification.common.context.TenantContext;
import com.notification.publisher.event.NotificationAuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditService auditService;

    @KafkaListener(topics = "${app.kafka.topics.audit:notification.audit}", groupId = "notification-audit-group")
    public void consumeAuditLog(
            @Payload NotificationAuditEvent event,
            @Header(name = "X-Tenant-Id", required = false) String tenantId,
            @Header(name = "X-Correlation-Id", required = false) String correlationId) {

        try {
            if (tenantId != null) {
                TenantContext.setTenantId(tenantId);
                MDC.put("tenantId", tenantId);
            }
            if (correlationId != null) {
                MDC.put("correlationId", correlationId);
            }

            log.debug("Consumed audit event for notification [id={}, status={}]", event.getNotificationId(), event.getStatus());
            auditService.saveAuditLog(event);

        } finally {
            TenantContext.clear();
            MDC.remove("tenantId");
            MDC.remove("correlationId");
        }
    }
}
