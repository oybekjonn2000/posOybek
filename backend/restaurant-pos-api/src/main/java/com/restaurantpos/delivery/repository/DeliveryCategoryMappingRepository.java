package com.restaurantpos.delivery.repository;

import com.restaurantpos.delivery.entity.DeliveryCategoryMappingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryCategoryMappingRepository extends JpaRepository<DeliveryCategoryMappingEntity, UUID> {

    List<DeliveryCategoryMappingEntity> findByTenantIdAndProviderId(UUID tenantId, UUID providerId);

    Page<DeliveryCategoryMappingEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<DeliveryCategoryMappingEntity> findByProviderIdAndExternalCategoryId(UUID providerId, String externalCategoryId);

    boolean existsByProviderIdAndExternalCategoryId(UUID providerId, String externalCategoryId);
}
