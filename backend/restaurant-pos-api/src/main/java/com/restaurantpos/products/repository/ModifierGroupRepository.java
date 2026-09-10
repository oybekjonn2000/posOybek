package com.restaurantpos.products.repository;

import com.restaurantpos.products.entity.ModifierGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ModifierGroupRepository extends JpaRepository<ModifierGroup, UUID> {

    List<ModifierGroup> findByTenantIdAndDeletedAtIsNullOrderBySortOrderAsc(UUID tenantId);
}
