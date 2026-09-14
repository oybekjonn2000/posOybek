package com.restaurantpos.delivery.service;

import com.restaurantpos.delivery.entity.DeliveryIntegrationLogEntity;
import com.restaurantpos.delivery.entity.DeliveryProviderEntity;
import com.restaurantpos.delivery.repository.DeliveryIntegrationLogRepository;
import com.restaurantpos.tenants.entity.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryLogService {

    private final DeliveryIntegrationLogRepository logRepository;

    @Transactional
    public void logAction(Tenant tenant, DeliveryProviderEntity provider, String action, String externalId,
                          DeliveryIntegrationLogEntity.LogStatus status, Long timeMs, String errorMessage, String details) {
        try {
            DeliveryIntegrationLogEntity entry = new DeliveryIntegrationLogEntity();
            entry.setTenant(tenant);
            entry.setProvider(provider);
            entry.setAction(action);
            entry.setExternalId(externalId);
            entry.setStatus(status);
            entry.setRequestTimeMs(timeMs);
            entry.setErrorMessage(errorMessage);
            entry.setDetails(details);
            logRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to save delivery integration log: {}", e.getMessage());
        }
    }
}
