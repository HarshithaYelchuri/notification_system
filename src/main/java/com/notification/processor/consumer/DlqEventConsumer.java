package com.notification.processor.consumer;

import com.notification.common.context.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DlqEventConsumer {

    @KafkaListener(topics = "${app.kafka.topics.dlq:notification.dlq}", groupId = "notification-dlq-group")
    public void consumeDlqEvent(
            @Payload Object payload,
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

            log.error("CRITICAL: Message received in Dead Letter Queue (DLQ). Payload: {}", payload);

        } finally {
            TenantContext.clear();
            MDC.remove("tenantId");
            MDC.remove("correlationId");
        }
    }
}
