package com.notification.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class NotificationMetrics {

    private final MeterRegistry meterRegistry;

    public void incrementNotificationRequests(String tenantId, String type, String status) {
        Counter.builder("notification_requests_total")
                .description("Total number of notification requests")
                .tag("tenant", tenantId != null ? tenantId : "unknown")
                .tag("type", type != null ? type : "unknown")
                .tag("status", status != null ? status : "unknown")
                .register(meterRegistry)
                .increment();
    }

    public void recordProcessingLatency(String tenantId, String type, long durationMs) {
        Timer.builder("notification_processing_latency_seconds")
                .description("Latency of notification processing in seconds")
                .tag("tenant", tenantId != null ? tenantId : "unknown")
                .tag("type", type != null ? type : "unknown")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void incrementRetries(String tenantId, String type) {
        Counter.builder("notification_retries_total")
                .description("Total number of notification delivery retries")
                .tag("tenant", tenantId != null ? tenantId : "unknown")
                .tag("type", type != null ? type : "unknown")
                .register(meterRegistry)
                .increment();
    }

    public void incrementDlq(String tenantId, String type) {
        Counter.builder("notification_dlq_total")
                .description("Total number of notifications sent to DLQ")
                .tag("tenant", tenantId != null ? tenantId : "unknown")
                .tag("type", type != null ? type : "unknown")
                .register(meterRegistry)
                .increment();
    }
}
