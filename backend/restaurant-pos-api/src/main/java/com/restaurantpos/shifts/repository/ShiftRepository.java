package com.restaurantpos.shifts.repository;

import com.restaurantpos.shifts.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, UUID> {

    Optional<Shift> findByTenantIdAndStatus(UUID tenantId, Shift.ShiftStatus status);

    Optional<Shift> findByTenantIdAndCashierIdAndStatus(UUID tenantId, UUID cashierId, Shift.ShiftStatus status);

    Optional<Shift> findByIdAndTenantId(UUID id, UUID tenantId);

    // Reports uchun
    List<Shift> findByTenantIdAndOpenedAtBetween(UUID tenantId, Instant from, Instant to);
}
