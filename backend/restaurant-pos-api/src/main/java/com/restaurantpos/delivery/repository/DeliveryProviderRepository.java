package com.restaurantpos.delivery.repository;

import com.restaurantpos.delivery.entity.DeliveryProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryProviderRepository extends JpaRepository<DeliveryProviderEntity, UUID> {

    List<DeliveryProviderEntity> findByTenantIdAndDeletedAtIsNull(UUID tenantId);

    Optional<DeliveryProviderEntity> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<DeliveryProviderEntity> findByTenantIdAndCodeAndDeletedAtIsNull(UUID tenantId, String code);

    Optional<DeliveryProviderEntity> findByCodeAndDeletedAtIsNull(String code);

    boolean existsByTenantIdAndCodeAndDeletedAtIsNull(UUID tenantId, String code);
}
