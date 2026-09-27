package com.notification.audit.repository;

import com.notification.audit.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    List<AuditLog> findByNotificationIdAndTenantIdOrderByTimestampDesc(String notificationId, String tenantId);

    Page<AuditLog> findByTenantId(String tenantId, Pageable pageable);
}
