package com.notification.notification.repository;

import com.notification.notification.domain.NotificationRecord;
import com.notification.notification.domain.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRecordRepository extends MongoRepository<NotificationRecord, String> {

    Optional<NotificationRecord> findByIdAndTenantId(String id, String tenantId);

    Optional<NotificationRecord> findByCorrelationIdAndTenantId(String correlationId, String tenantId);

    Page<NotificationRecord> findByTenantId(String tenantId, Pageable pageable);

    Page<NotificationRecord> findByTenantIdAndStatus(String tenantId, NotificationStatus status, Pageable pageable);
}
