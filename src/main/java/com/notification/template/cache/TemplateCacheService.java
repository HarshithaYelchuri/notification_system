package com.notification.template.cache;

import com.notification.template.domain.NotificationTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.redis.ttl-minutes:60}")
    private long ttlMinutes;

    public void cacheTemplate(NotificationTemplate template) {
        if (template == null || template.getTenantId() == null) {
            return;
        }

        try {
            Duration ttl = Duration.ofMinutes(ttlMinutes);
            if (template.getId() != null) {
                String idKey = getIdKey(template.getTenantId(), template.getId());
                redisTemplate.opsForValue().set(idKey, template, ttl);
            }
            if (template.getName() != null) {
                String nameKey = getNameKey(template.getTenantId(), template.getName());
                redisTemplate.opsForValue().set(nameKey, template, ttl);
            }
            log.debug("Cached template [id={}, name={}] for tenant [{}]", template.getId(), template.getName(), template.getTenantId());
        } catch (Exception e) {
            log.error("Failed to cache template in Redis: {}", e.getMessage(), e);
        }
    }

    public NotificationTemplate getTemplateById(String tenantId, String id) {
        try {
            String key = getIdKey(tenantId, id);
            Object obj = redisTemplate.opsForValue().get(key);
            if (obj instanceof NotificationTemplate template) {
                log.debug("Cache HIT for template id [{}] for tenant [{}]", id, tenantId);
                return template;
            }
        } catch (Exception e) {
            log.error("Redis read error for template id [{}]: {}", id, e.getMessage(), e);
        }
        log.debug("Cache MISS for template id [{}] for tenant [{}]", id, tenantId);
        return null;
    }

    public NotificationTemplate getTemplateByName(String tenantId, String name) {
        try {
            String key = getNameKey(tenantId, name);
            Object obj = redisTemplate.opsForValue().get(key);
            if (obj instanceof NotificationTemplate template) {
                log.debug("Cache HIT for template name [{}] for tenant [{}]", name, tenantId);
                return template;
            }
        } catch (Exception e) {
            log.error("Redis read error for template name [{}]: {}", name, e.getMessage(), e);
        }
        log.debug("Cache MISS for template name [{}] for tenant [{}]", name, tenantId);
        return null;
    }

    public void evictTemplate(String tenantId, String id, String name) {
        try {
            if (id != null) {
                redisTemplate.delete(getIdKey(tenantId, id));
            }
            if (name != null) {
                redisTemplate.delete(getNameKey(tenantId, name));
            }
            log.debug("Evicted cache for template [id={}, name={}] for tenant [{}]", id, name, tenantId);
        } catch (Exception e) {
            log.error("Failed to evict template cache: {}", e.getMessage(), e);
        }
    }

    private String getIdKey(String tenantId, String id) {
        return "tenant:" + tenantId + ":template:id:" + id;
    }

    private String getNameKey(String tenantId, String name) {
        return "tenant:" + tenantId + ":template:name:" + name;
    }
}
