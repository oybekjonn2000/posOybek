package com.restaurantpos.products.repository;

import com.restaurantpos.products.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByTenantIdAndDeletedAtIsNullOrderBySortOrderAsc(UUID tenantId);

    List<Category> findByTenantIdAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc(UUID tenantId);

    List<Category> findByTenantIdAndKitchenIdAndDeletedAtIsNullOrderBySortOrderAsc(UUID tenantId, UUID kitchenId);

    List<Category> findByTenantIdAndKitchenIdAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc(UUID tenantId, UUID kitchenId);

    long countByTenantIdAndKitchenIdAndDeletedAtIsNull(UUID tenantId, UUID kitchenId);

    Optional<Category> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    boolean existsByTenantIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID tenantId, String name);
}
