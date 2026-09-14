package com.restaurantpos.delivery.repository;

import com.restaurantpos.delivery.entity.DeliveryIntegrationLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DeliveryIntegrationLogRepository extends JpaRepository<DeliveryIntegrationLogEntity, UUID> {
    Page<DeliveryIntegrationLogEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Page<DeliveryIntegrationLogEntity> findByTenantIdAndProviderIdOrderByCreatedAtDesc(UUID tenantId, UUID providerId, Pageable pageable);
}
