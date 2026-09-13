package com.restaurantpos.kitchen.repository;

import com.restaurantpos.kitchen.entity.Kitchen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KitchenRepository extends JpaRepository<Kitchen, UUID> {

    List<Kitchen> findByTenantIdAndDeletedAtIsNullOrderBySortOrderAsc(UUID tenantId);

    List<Kitchen> findByTenantIdAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc(UUID tenantId);

    Optional<Kitchen> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<Kitchen> findByTenantIdAndCodeAndDeletedAtIsNull(UUID tenantId, String code);

    Optional<Kitchen> findByTenantIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID tenantId, String name);

    boolean existsByTenantIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID tenantId, String name);

    boolean existsByTenantIdAndNameIgnoreCaseAndIdNotAndDeletedAtIsNull(UUID tenantId, String name, UUID id);

    boolean existsByTenantIdAndCodeIgnoreCaseAndDeletedAtIsNull(UUID tenantId, String code);

    boolean existsByTenantIdAndCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(UUID tenantId, String code, UUID id);

    boolean existsByTenantIdAndCodeIgnoreCase(UUID tenantId, String code);

    boolean existsByTenantIdAndCodeIgnoreCaseAndIdNot(UUID tenantId, String code, UUID id);
}
