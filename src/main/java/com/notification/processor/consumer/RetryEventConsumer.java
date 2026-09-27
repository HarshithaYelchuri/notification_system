package com.notification.processor.consumer;

import com.notification.common.context.TenantContext;
import com.notification.publisher.GenericPublisher;
import com.notification.publisher.event.NotificationEnrichedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetryEventConsumer {

    private final GenericPublisher genericPublisher;

    @Value("${app.kafka.topics.enriched:notification.enriched}")
    private String enrichedTopic;

    @KafkaListener(topics = "${app.kafka.topics.retry:notification.retry}", groupId = "notification-retry-group")
    public void consumeRetryEvent(
            @Payload NotificationEnrichedEvent event,
            @Header(name = "X-Tenant-Id", required = false) String tenantId,
            @Header(name = "X-Correlation-Id", required = false) String correlationId) {

        String effectiveTenant = tenantId != null ? tenantId : event.getTenantId();
        String effectiveCorrelation = correlationId != null ? correlationId : event.getCorrelationId();

        try {
            TenantContext.setTenantId(effectiveTenant);
            MDC.put("tenantId", effectiveTenant);
            MDC.put("correlationId", effectiveCorrelation);

            // Bounded Exponential Backoff calculation: (2^retryCount) * 1000ms
            long backoffMs = (long) Math.pow(2, event.getRetryCount()) * 1000L;
            log.info("Applying retry backoff delay [{}] ms for notification [id={}, attempt={}]",
                    backoffMs, event.getNotificationId(), event.getRetryCount());

            try {
                Thread.sleep(Math.min(backoffMs, 10000L));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            Map<String, String> headers = Map.of(
                    "X-Tenant-Id", effectiveTenant,
                    "X-Correlation-Id", effectiveCorrelation
            );

            log.info("Re-queueing notification [id={}] to enriched topic for retry delivery", event.getNotificationId());
            genericPublisher.publish(enrichedTopic, event.getNotificationId(), event, headers);

        } finally {
            TenantContext.clear();
            MDC.remove("tenantId");
            MDC.remove("correlationId");
        }
    }
}
