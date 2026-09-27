package com.notification.notification.domain;

public enum NotificationStatus {
    PENDING,
    INGESTED,
    ENRICHED,
    DELIVERED,
    FAILED,
    RETRYING,
    DLQ
}
