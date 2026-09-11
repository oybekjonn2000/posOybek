package com.restaurantpos.printers.service;

import com.restaurantpos.common.exception.PosException;
import com.restaurantpos.kitchen.entity.Kitchen;
import com.restaurantpos.kitchen.repository.KitchenRepository;
import com.restaurantpos.printers.dto.PrinterDto;
import com.restaurantpos.printers.entity.Printer;
import com.restaurantpos.printers.entity.PrinterAssignment;
import com.restaurantpos.printers.repository.PrinterAssignmentRepository;
import com.restaurantpos.printers.repository.PrinterRepository;
import com.restaurantpos.settings.service.AuditLogService;
import com.restaurantpos.tenants.entity.Tenant;
import com.restaurantpos.tenants.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrinterService {

    private final PrinterRepository printerRepository;
    private final PrinterAssignmentRepository assignmentRepository;
    private final KitchenRepository kitchenRepository;
    private final TenantRepository tenantRepository;
    private final AuditLogService auditLogService;
    private final PrinterDiscoveryService printerDiscoveryService;
    private final WindowsPrintService windowsPrintService;

    /**
     * Discovers all real Windows installed printers on this host.
     */
    public List<PrinterDto.AvailablePrinterDto> getAvailableWindowsPrinters() {
        return printerDiscoveryService.discoverPrinters();
    }

    /**
     * Refreshes real-time status of all configured POS printers against Windows
     * subsystem.
     */
    @Transactional
    public List<PrinterDto.Response> refreshPrintersStatus(UUID tenantId) {
        List<Printer> printers = printerRepository.findByTenantIdAndDeletedAtIsNullOrderByCreatedAtAsc(tenantId);

        for (Printer p : printers) {
            String winName = p.getWindowsPrinterName();
            if (winName == null || winName.isBlank()) {
                p.setStatus(Printer.PrinterStatus.UNKNOWN);
                continue;
            }

            String currentWinStatus = printerDiscoveryService.checkPrinterStatus(winName);
            try {
                Printer.PrinterStatus newStatus = Printer.PrinterStatus.valueOf(currentWinStatus);
                p.setStatus(newStatus);
                p.setLastCheckedAt(Instant.now());
                if (newStatus == Printer.PrinterStatus.NOT_FOUND) {
                    p.setLastError("Printer Windows tizimida topilmadi (o'chirilgan yoki nomi o'zgargan)");
                } else if (newStatus == Printer.PrinterStatus.ONLINE) {
                    p.setLastError(null);
                }
            } catch (Exception e) {
                p.setStatus(Printer.PrinterStatus.UNKNOWN);
            }
            printerRepository.save(p);
        }

        return getPrinters(tenantId);
    }

    @Transactional(readOnly = true)
    public List<PrinterDto.Response> getPrinters(UUID tenantId) {
        List<Printer> printers = printerRepository.findByTenantIdAndDeletedAtIsNullOrderByCreatedAtAsc(tenantId);
        List<PrinterAssignment> assignments = assignmentRepository.findByTenantIdAndDeletedAtIsNull(tenantId);

        Map<UUID, PrinterAssignment> primaryAssignments = assignments.stream()
                .filter(a -> a.isActive() && a.isPrimary())
                .collect(Collectors.toMap(a -> a.getPrinter().getId(), a -> a, (k1, k2) -> k1));

        return printers.stream()
                .map(p -> toResponse(p, primaryAssignments.get(p.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PrinterDto.Response getPrinterById(UUID tenantId, UUID id) {
        Printer printer = printerRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Printer topilmadi: " + id));

        PrinterAssignment assignment = assignmentRepository.findByTenantIdAndPrinterIdAndDeletedAtIsNull(tenantId, id)
                .stream().filter(a -> a.isActive() && a.isPrimary()).findFirst().orElse(null);

        return toResponse(printer, assignment);
    }

    @Transactional
    public PrinterDto.Response createPrinter(UUID tenantId, UUID userId, PrinterDto.CreateRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> PosException.notFound("Tenant topilmadi"));

        String systemPrinterName = request.getResolvedSystemPrinterName();
        if (systemPrinterName == null || systemPrinterName.isBlank()) {
            throw PosException.badRequest("Windows printerini tanlash majburiy");
        }
        systemPrinterName = systemPrinterName.trim();

        // 1. Strict Validation: Must be a real Windows installed printer!
        if (!printerDiscoveryService.isPrinterInstalledInWindows(systemPrinterName)) {
            throw PosException.badRequest(
                    "Windows tizimida '" + systemPrinterName + "' nomli printer topilmadi. " +
                            "Faqat kompyuterda o'rnatilgan Windows printerlarini tanlang.");
        }

        // 2. Strict Validation: Duplicate printer prevention!
        if (printerRepository.existsByTenantIdAndWindowsPrinterNameIgnoreCaseAndDeletedAtIsNull(tenantId,
                systemPrinterName)) {
            throw PosException
                    .badRequest("Ushbu Windows printeri POS tizimiga allaqachon qo'shilgan: " + systemPrinterName);
        }

        // 3. Validation: Purpose and Kitchen
        String purposeStr = request.getPurpose() != null ? request.getPurpose().trim().toUpperCase() : "KITCHEN";
        if ("RECEIPT".equals(purposeStr) || "BAR".equals(purposeStr) || "OTHER".equals(purposeStr)) {
            purposeStr = "CASHIER";
        }
        Printer.PrinterPurpose purpose;
        try {
            purpose = Printer.PrinterPurpose.valueOf(purposeStr);
        } catch (IllegalArgumentException e) {
            throw PosException.badRequest("Noto'g'ri printer turi: " + purposeStr + ". Faqat KITCHEN yoki CASHIER bo'lishi mumkin.");
        }

        if (purpose == Printer.PrinterPurpose.KITCHEN && request.getKitchenId() == null) {
            throw PosException.badRequest("Oshxona printeri uchun oshxona bo'limini tanlash majburiy");
        }

        // Display name defaults to Windows printer name if omitted
        String displayName = request.getName() != null && !request.getName().trim().isBlank()
                ? request.getName().trim()
                : systemPrinterName;

        int paperWidth = request.getPaperWidth() != null && request.getPaperWidth() == 58 ? 58 : 80;

        // Query initial Windows status
        String initialStatusStr = printerDiscoveryService.checkPrinterStatus(systemPrinterName);
        Printer.PrinterStatus status;
        try {
            status = Printer.PrinterStatus.valueOf(initialStatusStr);
        } catch (Exception e) {
            status = Printer.PrinterStatus.UNKNOWN;
        }

        Printer printer = new Printer();
        printer.setTenant(tenant);
        printer.setName(displayName);
        printer.setModel(request.getModel() != null ? request.getModel().trim() : null);
        printer.setConnectionType(Printer.ConnectionType.WINDOWS);
        printer.setWindowsPrinterName(systemPrinterName);
        printer.setPaperWidth(paperWidth);
        printer.setCharacterEncoding(request.getCharacterEncoding() != null ? request.getCharacterEncoding() : "UTF-8");
        printer.setPurpose(purpose);
        printer.setAutoPrint(request.getAutoPrint() != null ? request.getAutoPrint() : true);
        printer.setDefault(request.getIsDefault() != null ? request.getIsDefault() : false);
        printer.setStatus(status);
        printer.setActive(true);
        printer.setLastCheckedAt(Instant.now());

        if (request.getFallbackPrinterId() != null) {
            Printer fallback = printerRepository
                    .findByIdAndTenantIdAndDeletedAtIsNull(request.getFallbackPrinterId(), tenantId)
                    .orElse(null);
            printer.setFallbackPrinter(fallback);
        }

        Printer saved = printerRepository.save(printer);

        // Handle assignment with single-primary enforcement
        PrinterAssignment assignment = null;
        if (purpose == Printer.PrinterPurpose.KITCHEN && request.getKitchenId() != null) {
            boolean isPrimary = request.getIsPrimary() != null ? request.getIsPrimary() : true;
            assignment = assignKitchen(tenant, saved, request.getKitchenId(), isPrimary);
        } else if (purpose == Printer.PrinterPurpose.CASHIER) {
            assignment = assignCashier(tenant, saved);
        }

        auditLogService.logChange(tenantId, userId, "CREATE", "PRINTER", saved.getId(), null,
                saved.getName() + " [Windows: " + saved.getWindowsPrinterName() + "]",
                "Yangi Windows printeri qo'shildi");

        return toResponse(saved, assignment);
    }

    @Transactional
    public PrinterDto.Response updatePrinter(UUID tenantId, UUID userId, UUID id, PrinterDto.UpdateRequest request) {
        Printer printer = printerRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Printer topilmadi: " + id));

        String oldVal = printer.getName() + " [Windows: " + printer.getWindowsPrinterName() + ", "
                + printer.getPurpose() + "]";

        if (request.getName() != null && !request.getName().isBlank()) {
            printer.setName(request.getName().trim());
        }
        String resolvedSysName = request.getResolvedSystemPrinterName();
        if (resolvedSysName != null && !resolvedSysName.isBlank()) {
            printer.setWindowsPrinterName(resolvedSysName.trim());
        }
        if (request.getModel() != null) {
            printer.setModel(request.getModel().trim());
        }
        if (request.getPaperWidth() != null) {
            printer.setPaperWidth(request.getPaperWidth() == 58 ? 58 : 80);
        }
        if (request.getPurpose() != null && !request.getPurpose().isBlank()) {
            String pStr = request.getPurpose().trim().toUpperCase();
            if ("RECEIPT".equals(pStr) || "BAR".equals(pStr) || "OTHER".equals(pStr)) {
                pStr = "CASHIER";
            }
            try {
                printer.setPurpose(Printer.PrinterPurpose.valueOf(pStr));
            } catch (IllegalArgumentException e) {
                throw PosException.badRequest("Noto'g'ri printer turi: " + pStr + ". Faqat KITCHEN yoki CASHIER bo'lishi mumkin.");
            }
        }
        if (request.getAutoPrint() != null) {
            printer.setAutoPrint(request.getAutoPrint());
        }
        if (request.getIsDefault() != null) {
            printer.setDefault(request.getIsDefault());
        }
        if (request.getActive() != null) {
            printer.setActive(request.getActive());
        }
        if (request.getStatus() != null) {
            try {
                printer.setStatus(Printer.PrinterStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (Exception ignored) {
            }
        }

        if (request.getFallbackPrinterId() != null) {
            if (request.getFallbackPrinterId().equals(printer.getId())) {
                throw PosException.badRequest("Printer o'ziga o'zi zaxira (fallback) printer bo'la olmaydi");
            }
            Printer fallback = printerRepository
                    .findByIdAndTenantIdAndDeletedAtIsNull(request.getFallbackPrinterId(), tenantId)
                    .orElse(null);
            printer.setFallbackPrinter(fallback);
        }

        Printer saved = printerRepository.save(printer);

        // Update kitchen assignment if purpose is KITCHEN
        PrinterAssignment assignment = null;
        if (saved.getPurpose() == Printer.PrinterPurpose.KITCHEN && request.getKitchenId() != null) {
            Tenant tenant = printer.getTenant();
            boolean isPrimary = request.getIsPrimary() != null ? request.getIsPrimary() : true;
            assignment = assignKitchen(tenant, saved, request.getKitchenId(), isPrimary);
        } else if (saved.getPurpose() == Printer.PrinterPurpose.CASHIER) {
            assignment = assignCashier(printer.getTenant(), saved);
        } else {
            assignment = assignmentRepository.findByTenantIdAndPrinterIdAndDeletedAtIsNull(tenantId, id)
                    .stream().filter(a -> a.isActive() && a.isPrimary()).findFirst().orElse(null);
        }

        String newVal = saved.getName() + " [Windows: " + saved.getWindowsPrinterName() + ", " + saved.getPurpose()
                + "]";
        auditLogService.logChange(tenantId, userId, "UPDATE", "PRINTER", saved.getId(), oldVal, newVal,
                "Printer tahrirlandi");

        return toResponse(saved, assignment);
    }

    @Transactional
    public void deletePrinter(UUID tenantId, UUID userId, UUID id) {
        Printer printer = printerRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Printer topilmadi: " + id));

        // Soft-deactivate POS assignments only
        List<PrinterAssignment> assignments = assignmentRepository
                .findByTenantIdAndPrinterIdAndDeletedAtIsNull(tenantId, id);
        for (PrinterAssignment a : assignments) {
            a.setActive(false);
            a.setDeletedAt(Instant.now());
            assignmentRepository.save(a);
        }

        printer.setActive(false);
        printer.setDeletedAt(Instant.now());
        printerRepository.save(printer);

        auditLogService.logChange(tenantId, userId, "DELETE", "PRINTER", id,
                printer.getName() + " [Windows: " + printer.getWindowsPrinterName() + "]",
                null, "Printer POS konfiguratsiyasidan o'chirildi (Windows printerni saqlagan holda)");
    }

    /**
     * Executes a real test print job on the configured Windows printer.
     */
    @Transactional
    public PrinterDto.TestPrintResult testPrint(UUID tenantId, UUID id) {
        Printer printer = printerRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Printer topilmadi: " + id));

        Instant now = Instant.now();
        printer.setLastCheckedAt(now);

        String winName = printer.getWindowsPrinterName();
        if (winName == null || winName.isBlank()) {
            throw PosException.badRequest("Printer uchun Windows printer nomi ko'rsatilmagan");
        }

        try {
            // Build the required Test Print receipt layout
            String timeStr = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.of("Asia/Tashkent"))
                    .format(now);

            StringBuilder sb = new StringBuilder();
            sb.append("\n================================\n");
            sb.append("        TEST PRINT              \n");
            sb.append("================================\n");
            sb.append("Restaurant POS\n\n");
            sb.append("Printer:\n").append(printer.getName()).append("\n\n");
            sb.append("Windows printer:\n").append(winName).append("\n\n");
            sb.append("Date:\n").append(timeStr).append("\n\n");
            sb.append("Status:\nSUCCESS\n");
            sb.append("================================\n\n\n\n");

            String textContent = sb.toString();
            byte[] rawBytes = buildEscPosPayload(textContent);

            // Execute real print via WindowsPrintService
            windowsPrintService.print(winName, rawBytes, textContent);

            printer.setStatus(Printer.PrinterStatus.ONLINE);
            printer.setLastSuccessfulPrintAt(now);
            printer.setLastError(null);
            printerRepository.save(printer);

            log.info("Test print SUCCESS for '{}' (Windows: '{}')", printer.getName(), winName);

            return PrinterDto.TestPrintResult.builder()
                    .success(true)
                    .message("Test cheki Windows printerga muvaffaqiyatli yuborildi")
                    .printerName(printer.getName())
                    .connectionType("WINDOWS")
                    .target(winName)
                    .testedAt(now)
                    .build();

        } catch (Exception ex) {
            String reason = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            log.warn("Test print FAILED for '{}' (Windows: '{}'): {}", printer.getName(), winName, reason);

            printer.setStatus(Printer.PrinterStatus.OFFLINE);
            printer.setLastError(reason);
            printerRepository.save(printer);

            return PrinterDto.TestPrintResult.builder()
                    .success(false)
                    .message("Printerga ma'lumot yuborib bo'lmadi: " + reason)
                    .printerName(printer.getName())
                    .connectionType("WINDOWS")
                    .target(winName)
                    .errorDetails(reason)
                    .testedAt(now)
                    .build();
        }
    }

    @Transactional
    public PrinterAssignment assignKitchen(Tenant tenant, Printer printer, UUID kitchenId, boolean isPrimary) {
        Kitchen kitchen = kitchenRepository.findById(kitchenId)
                .orElseThrow(() -> PosException.notFound("Kitchen topilmadi: " + kitchenId));

        if (isPrimary) {
            // Enforce strictly one primary printer per kitchen
            List<PrinterAssignment> existing = assignmentRepository
                    .findByTenantIdAndKitchenIdAndActiveTrueAndDeletedAtIsNull(
                            tenant.getId(), kitchenId);
            for (PrinterAssignment ea : existing) {
                if (!ea.getPrinter().getId().equals(printer.getId())) {
                    ea.setPrimary(false);
                    assignmentRepository.save(ea);
                }
            }
        }

        PrinterAssignment assignment = assignmentRepository.findByTenantIdAndKitchenIdAndActiveTrueAndDeletedAtIsNull(
                tenant.getId(), kitchenId).stream()
                .filter(a -> a.getPrinter().getId().equals(printer.getId()))
                .findFirst()
                .orElseGet(() -> {
                    PrinterAssignment pa = new PrinterAssignment();
                    pa.setTenant(tenant);
                    pa.setPrinter(printer);
                    pa.setKitchen(kitchen);
                    pa.setPurpose("KITCHEN");
                    return pa;
                });

        assignment.setPrimary(isPrimary);
        assignment.setActive(true);
        return assignmentRepository.save(assignment);
    }

    @Transactional
    public PrinterAssignment assignCashier(Tenant tenant, Printer printer) {
        // Enforce single primary cashier printer
        List<PrinterAssignment> existing = assignmentRepository
                .findByTenantIdAndPurposeAndPrimaryTrueAndActiveTrueAndDeletedAtIsNull(
                        tenant.getId(), "CASHIER");
        for (PrinterAssignment ea : existing) {
            if (!ea.getPrinter().getId().equals(printer.getId())) {
                ea.setPrimary(false);
                assignmentRepository.save(ea);
            }
        }

        PrinterAssignment assignment = assignmentRepository
                .findByTenantIdAndPrinterIdAndDeletedAtIsNull(tenant.getId(), printer.getId())
                .stream().filter(PrinterAssignment::isActive).findFirst()
                .orElseGet(() -> {
                    PrinterAssignment pa = new PrinterAssignment();
                    pa.setTenant(tenant);
                    pa.setPrinter(printer);
                    pa.setPurpose("CASHIER");
                    return pa;
                });

        assignment.setPrimary(true);
        assignment.setActive(true);
        return assignmentRepository.save(assignment);
    }

    private byte[] buildEscPosPayload(String text) {
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] escInit = new byte[] { 0x1B, 0x40 }; // ESC @
        byte[] cut = new byte[] { 0x1D, 0x56, 0x41, 0x10 }; // GS V A 16 (Cut)

        byte[] result = new byte[escInit.length + textBytes.length + cut.length];
        System.arraycopy(escInit, 0, result, 0, escInit.length);
        System.arraycopy(textBytes, 0, result, escInit.length, textBytes.length);
        System.arraycopy(cut, 0, result, escInit.length + textBytes.length, cut.length);
        return result;
    }

    public PrinterDto.Response toResponse(Printer p, PrinterAssignment assignment) {
        UUID kitchenId = null;
        String kitchenName = null;
        boolean isPrimary = false;

        if (assignment != null && assignment.getKitchen() != null) {
            kitchenId = assignment.getKitchen().getId();
            kitchenName = assignment.getKitchen().getName();
            isPrimary = assignment.isPrimary();
        }

        return PrinterDto.Response.builder()
                .id(p.getId())
                .name(p.getName())
                .model(p.getModel())
                .connectionType(p.getConnectionType().name())
                .ipAddress(p.getIpAddress())
                .port(p.getPort())
                .windowsPrinterName(p.getWindowsPrinterName())
                .systemPrinterName(p.getWindowsPrinterName())
                .paperWidth(p.getPaperWidth())
                .characterEncoding(p.getCharacterEncoding())
                .purpose(p.getPurpose().name())
                .status(p.getStatus().name())
                .active(p.isActive())
                .isDefault(p.isDefault())
                .autoPrint(p.isAutoPrint())
                .fallbackPrinterId(p.getFallbackPrinter() != null ? p.getFallbackPrinter().getId() : null)
                .fallbackPrinterName(p.getFallbackPrinter() != null ? p.getFallbackPrinter().getName() : null)
                .assignedKitchenId(kitchenId)
                .assignedKitchenName(kitchenName)
                .isPrimaryForKitchen(isPrimary)
                .lastCheckedAt(p.getLastCheckedAt())
                .lastSuccessfulPrintAt(p.getLastSuccessfulPrintAt())
                .lastError(p.getLastError())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
