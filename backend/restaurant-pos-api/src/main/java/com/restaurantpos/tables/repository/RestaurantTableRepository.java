package com.restaurantpos.tables.repository;

import com.restaurantpos.tables.entity.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, UUID> {

    List<RestaurantTable> findByTenantIdAndDeletedAtIsNullOrderByTableNumberAsc(UUID tenantId);

    List<RestaurantTable> findByTenantIdAndZoneIdAndDeletedAtIsNullOrderByTableNumberAsc(UUID tenantId, UUID zoneId);

    Optional<RestaurantTable> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<RestaurantTable> findByTenantIdAndTableNumberAndDeletedAtIsNull(UUID tenantId, String tableNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM RestaurantTable t WHERE t.id = :id AND t.tenant.id = :tenantId AND t.deletedAt IS NULL")
    Optional<RestaurantTable> findByIdWithLock(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
}

