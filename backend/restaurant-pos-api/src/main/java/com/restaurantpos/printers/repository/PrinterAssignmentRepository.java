package com.restaurantpos.printers.repository;

import com.restaurantpos.printers.entity.PrinterAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrinterAssignmentRepository extends JpaRepository<PrinterAssignment, UUID> {

    List<PrinterAssignment> findByTenantIdAndDeletedAtIsNull(UUID tenantId);

    List<PrinterAssignment> findByTenantIdAndActiveTrueAndDeletedAtIsNull(UUID tenantId);

    List<PrinterAssignment> findByTenantIdAndKitchenIdAndActiveTrueAndDeletedAtIsNull(UUID tenantId, UUID kitchenId);

    Optional<PrinterAssignment> findByTenantIdAndKitchenIdAndPrimaryTrueAndActiveTrueAndDeletedAtIsNull(UUID tenantId, UUID kitchenId);

    List<PrinterAssignment> findByTenantIdAndPrinterIdAndDeletedAtIsNull(UUID tenantId, UUID printerId);

    List<PrinterAssignment> findByTenantIdAndPurposeAndPrimaryTrueAndActiveTrueAndDeletedAtIsNull(UUID tenantId, String purpose);
}
