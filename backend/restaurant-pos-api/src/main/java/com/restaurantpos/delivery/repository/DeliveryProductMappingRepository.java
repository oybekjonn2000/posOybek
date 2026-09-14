package com.restaurantpos.delivery.repository;

import com.restaurantpos.delivery.entity.DeliveryProductMappingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryProductMappingRepository extends JpaRepository<DeliveryProductMappingEntity, UUID> {

    List<DeliveryProductMappingEntity> findByTenantIdAndProviderId(UUID tenantId, UUID providerId);

    Page<DeliveryProductMappingEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<DeliveryProductMappingEntity> findByProviderIdAndExternalProductId(UUID providerId, String externalProductId);

    boolean existsByProviderIdAndExternalProductId(UUID providerId, String externalProductId);
}
