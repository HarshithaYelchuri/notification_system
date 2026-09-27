package com.notification.template.repository;

import com.notification.template.domain.NotificationTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TemplateRepository extends MongoRepository<NotificationTemplate, String> {

    Optional<NotificationTemplate> findByIdAndTenantId(String id, String tenantId);

    Optional<NotificationTemplate> findByTenantIdAndName(String tenantId, String name);

    boolean existsByTenantIdAndName(String tenantId, String name);

    Page<NotificationTemplate> findByTenantId(String tenantId, Pageable pageable);

    Page<NotificationTemplate> findByTenantIdAndNameContainingIgnoreCase(String tenantId, String name, Pageable pageable);
}
