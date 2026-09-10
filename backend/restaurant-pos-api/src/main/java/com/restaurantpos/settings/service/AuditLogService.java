package com.restaurantpos.settings.service;

import com.restaurantpos.settings.entity.AuditLog;
import com.restaurantpos.settings.repository.AuditLogRepository;
import com.restaurantpos.tenants.entity.Tenant;
import com.restaurantpos.tenants.repository.TenantRepository;
import com.restaurantpos.users.entity.User;
import com.restaurantpos.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    @Transactional
    public void logChange(UUID tenantId, UUID userId, String action, String entityType, UUID entityId,
                          String oldValue, String newValue, String notes) {
        try {
            Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
            if (tenant == null) return;

            User user = userId != null ? userRepository.findById(userId).orElse(null) : null;

            AuditLog logEntry = new AuditLog();
            logEntry.setTenant(tenant);
            logEntry.setUser(user);
            logEntry.setAction(action);
            logEntry.setEntityType(entityType);
            logEntry.setEntityId(entityId);
            logEntry.setOldValue(oldValue);
            logEntry.setNewValue(newValue);
            logEntry.setNotes(notes);

            auditLogRepository.save(logEntry);
            log.info("Audit log saved: action={}, entity={}, id={}, user={}", action, entityType, entityId,
                    user != null ? user.getUsername() : "SYSTEM");
        } catch (Exception e) {
            log.error("Failed to write audit log: {}", e.getMessage());
        }
    }
}
