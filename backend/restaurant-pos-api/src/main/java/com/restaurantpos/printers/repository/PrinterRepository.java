package com.restaurantpos.printers.repository;

import com.restaurantpos.printers.entity.Printer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrinterRepository extends JpaRepository<Printer, UUID> {

    List<Printer> findByTenantIdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID tenantId);

    Optional<Printer> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<Printer> findByTenantIdAndIsDefaultTrueAndDeletedAtIsNull(UUID tenantId);

    List<Printer> findByTenantIdAndPurposeAndDeletedAtIsNull(UUID tenantId, Printer.PrinterPurpose purpose);

    boolean existsByTenantIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID tenantId, String name);
    boolean existsByTenantIdAndWindowsPrinterNameIgnoreCaseAndDeletedAtIsNull(UUID tenantId, String windowsPrinterName);
    Optional<Printer> findByTenantIdAndWindowsPrinterNameIgnoreCaseAndDeletedAtIsNull(UUID tenantId, String windowsPrinterName);
}
