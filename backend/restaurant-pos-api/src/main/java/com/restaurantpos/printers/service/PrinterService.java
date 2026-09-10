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

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
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

    private static final Pattern IP_PATTERN = Pattern.compile(
            "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$"
    );

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
                .orElseThrow(() -> PosException.notFound("Tenant not found"));

        validatePrinterRequest(request);

        if (printerRepository.existsByTenantIdAndNameIgnoreCaseAndDeletedAtIsNull(tenantId, request.getName().trim())) {
            throw PosException.badRequest("Ushbu nomli printer allaqachon mavjud: " + request.getName());
        }

        Printer printer = new Printer();
        printer.setTenant(tenant);
        printer.setName(request.getName().trim());
        printer.setModel(request.getModel() != null ? request.getModel().trim() : null);
        printer.setConnectionType(Printer.ConnectionType.valueOf(request.getConnectionType().toUpperCase().replace("/", "")));
        printer.setIpAddress(request.getIpAddress() != null ? request.getIpAddress().trim() : null);
        printer.setPort(request.getPort() != null ? request.getPort() : 9100);
        printer.setWindowsPrinterName(request.getWindowsPrinterName() != null ? request.getWindowsPrinterName().trim() : null);
        printer.setPaperWidth(request.getPaperWidth() != null ? request.getPaperWidth() : 80);
        printer.setCharacterEncoding(request.getCharacterEncoding() != null ? request.getCharacterEncoding() : "UTF-8");
        printer.setPurpose(Printer.PrinterPurpose.valueOf(request.getPurpose().toUpperCase()));
        printer.setAutoPrint(request.getAutoPrint() != null ? request.getAutoPrint() : true);
        printer.setDefault(request.getIsDefault() != null ? request.getIsDefault() : false);
        printer.setStatus(Printer.PrinterStatus.ONLINE);
        printer.setActive(true);

        if (request.getFallbackPrinterId() != null) {
            Printer fallback = printerRepository.findByIdAndTenantIdAndDeletedAtIsNull(request.getFallbackPrinterId(), tenantId)
                    .orElse(null);
            printer.setFallbackPrinter(fallback);
        }

        Printer saved = printerRepository.save(printer);

        // Handle assignment if kitchen provided
        PrinterAssignment assignment = null;
        if (request.getKitchenId() != null && printer.getPurpose() == Printer.PrinterPurpose.KITCHEN) {
            assignment = assignKitchen(tenant, saved, request.getKitchenId(), request.getIsPrimary() != null ? request.getIsPrimary() : true);
        } else if (printer.getPurpose() == Printer.PrinterPurpose.CASHIER) {
            assignment = assignCashier(tenant, saved);
        }

        auditLogService.logChange(tenantId, userId, "CREATE", "PRINTER", saved.getId(), null,
                saved.getName() + " (" + saved.getConnectionType() + ")", "Yangi printer yaratildi");

        return toResponse(saved, assignment);
    }

    @Transactional
    public PrinterDto.Response updatePrinter(UUID tenantId, UUID userId, UUID id, PrinterDto.UpdateRequest request) {
        Printer printer = printerRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Printer topilmadi: " + id));

        String oldVal = printer.getName() + " [" + printer.getConnectionType() + ", " + printer.getPurpose() + "]";

        if (request.getName() != null && !request.getName().isBlank()) {
            printer.setName(request.getName().trim());
        }
        if (request.getModel() != null) printer.setModel(request.getModel().trim());
        if (request.getConnectionType() != null) {
            printer.setConnectionType(Printer.ConnectionType.valueOf(request.getConnectionType().toUpperCase().replace("/", "")));
        }
        if (request.getIpAddress() != null) printer.setIpAddress(request.getIpAddress().trim());
        if (request.getPort() != null) printer.setPort(request.getPort());
        if (request.getWindowsPrinterName() != null) printer.setWindowsPrinterName(request.getWindowsPrinterName().trim());
        if (request.getPaperWidth() != null) printer.setPaperWidth(request.getPaperWidth());
        if (request.getCharacterEncoding() != null) printer.setCharacterEncoding(request.getCharacterEncoding());
        if (request.getPurpose() != null) {
            printer.setPurpose(Printer.PrinterPurpose.valueOf(request.getPurpose().toUpperCase()));
        }
        if (request.getStatus() != null) {
            printer.setStatus(Printer.PrinterStatus.valueOf(request.getStatus().toUpperCase()));
        }
        if (request.getActive() != null) printer.setActive(request.getActive());
        if (request.getAutoPrint() != null) printer.setAutoPrint(request.getAutoPrint());
        if (request.getIsDefault() != null) printer.setDefault(request.getIsDefault());

        if (request.getFallbackPrinterId() != null) {
            if (request.getFallbackPrinterId().equals(printer.getId())) {
                throw PosException.badRequest("Printer o'ziga o'zi zaxira (fallback) printer bo'la olmaydi");
            }
            Printer fallback = printerRepository.findByIdAndTenantIdAndDeletedAtIsNull(request.getFallbackPrinterId(), tenantId)
                    .orElse(null);
            printer.setFallbackPrinter(fallback);
        }

        Printer saved = printerRepository.save(printer);

        // Update kitchen assignment if specified
        PrinterAssignment assignment = null;
        if (request.getKitchenId() != null) {
            Tenant tenant = printer.getTenant();
            assignment = assignKitchen(tenant, saved, request.getKitchenId(), request.getIsPrimary() != null ? request.getIsPrimary() : true);
        } else {
            assignment = assignmentRepository.findByTenantIdAndPrinterIdAndDeletedAtIsNull(tenantId, id)
                    .stream().filter(a -> a.isActive() && a.isPrimary()).findFirst().orElse(null);
        }

        String newVal = saved.getName() + " [" + saved.getConnectionType() + ", " + saved.getPurpose() + "]";
        auditLogService.logChange(tenantId, userId, "UPDATE", "PRINTER", saved.getId(), oldVal, newVal, "Printer tahrirlandi");

        return toResponse(saved, assignment);
    }

    @Transactional
    public void deletePrinter(UUID tenantId, UUID userId, UUID id) {
        Printer printer = printerRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Printer topilmadi: " + id));

        // Deactivate active assignments
        List<PrinterAssignment> assignments = assignmentRepository.findByTenantIdAndPrinterIdAndDeletedAtIsNull(tenantId, id);
        for (PrinterAssignment a : assignments) {
            a.setActive(false);
            a.setDeletedAt(Instant.now());
            assignmentRepository.save(a);
        }

        printer.setActive(false);
        printer.setDeletedAt(Instant.now());
        printerRepository.save(printer);

        auditLogService.logChange(tenantId, userId, "DELETE", "PRINTER", id, printer.getName(), null, "Printer o'chirildi (soft-delete)");
    }

    @Transactional
    public PrinterDto.TestPrintResult testPrint(UUID tenantId, UUID id) {
        Printer printer = printerRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Printer topilmadi: " + id));

        Instant now = Instant.now();
        printer.setLastCheckedAt(now);

        Printer.ConnectionType type = printer.getConnectionType();
        String target = "";

        try {
            if (type == Printer.ConnectionType.NETWORK || type == Printer.ConnectionType.TCPIP) {
                String ip = printer.getIpAddress();
                int port = printer.getPort() != null ? printer.getPort() : 9100;
                target = ip + ":" + port;

                if (ip == null || ip.isBlank()) {
                    throw new IllegalArgumentException("Network printer uchun IP manzil kiritilmagan");
                }

                // Attempt real socket connection with 2500ms timeout
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(ip, port), 2500);

                    // Send ESC/POS test packet
                    try (OutputStream out = socket.getOutputStream()) {
                        byte[] testTicket = buildTestTicket(printer);
                        out.write(testTicket);
                        out.flush();
                    }
                }

                printer.setStatus(Printer.PrinterStatus.ONLINE);
                printer.setLastSuccessfulPrintAt(now);
                printer.setLastError(null);
                printerRepository.save(printer);

                log.info("Printer test SUCCESS: {} ({})", printer.getName(), target);
                return PrinterDto.TestPrintResult.builder()
                        .success(true)
                        .message("Printer muvaffaqiyatli ishlayapti")
                        .printerName(printer.getName())
                        .connectionType(type.name())
                        .target(target)
                        .testedAt(now)
                        .build();

            } else if (type == Printer.ConnectionType.WINDOWS || type == Printer.ConnectionType.USB) {
                String winName = printer.getWindowsPrinterName();
                target = winName != null ? winName : "Default Windows Spooler";

                PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
                boolean found = false;

                if (winName != null && !winName.isBlank()) {
                    for (PrintService ps : services) {
                        if (ps.getName().equalsIgnoreCase(winName.trim())) {
                            found = true;
                            break;
                        }
                    }
                } else if (services.length > 0) {
                    found = true;
                    target = services[0].getName();
                }

                if (!found) {
                    List<String> installed = Arrays.stream(services).map(PrintService::getName).collect(Collectors.toList());
                    throw new IllegalStateException("Windows tizimida '" + winName + "' nomli printer topilmadi. O'rnatilgan printerlar: " + installed);
                }

                printer.setStatus(Printer.PrinterStatus.ONLINE);
                printer.setLastSuccessfulPrintAt(now);
                printer.setLastError(null);
                printerRepository.save(printer);

                return PrinterDto.TestPrintResult.builder()
                        .success(true)
                        .message("Printer muvaffaqiyatli ishlayapti (Windows Spooler tayyor)")
                        .printerName(printer.getName())
                        .connectionType(type.name())
                        .target(target)
                        .testedAt(now)
                        .build();
            }

            throw new UnsupportedOperationException("Ulanish turi qo'llab-quvvatlanmaydi: " + type);

        } catch (Exception ex) {
            String reason = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            if (ex instanceof java.net.SocketTimeoutException) {
                reason = "Printerga ulanish vaqti tugadi (" + target + " javob bermadi, 2500ms)";
            } else if (ex instanceof java.net.ConnectException) {
                reason = "Printerga ulanish rad etildi (" + target + " porti yopiq yoki printer o'chiq)";
            }

            log.warn("Printer test FAILED: {} ({}) -> {}", printer.getName(), target, reason);

            printer.setStatus(Printer.PrinterStatus.OFFLINE);
            printer.setLastError(reason);
            printerRepository.save(printer);

            return PrinterDto.TestPrintResult.builder()
                    .success(false)
                    .message("Printerga ulanish imkoni bo'lmadi: " + reason)
                    .printerName(printer.getName())
                    .connectionType(type.name())
                    .target(target)
                    .errorDetails(reason)
                    .testedAt(now)
                    .build();
        }
    }

    private byte[] buildTestTicket(Printer printer) {
        String timeStr = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.of("Asia/Tashkent"))
                .format(Instant.now());

        StringBuilder sb = new StringBuilder();
        sb.append("\n================================\n");
        sb.append("        TEST PRINT SUCCESS      \n");
        sb.append("================================\n");
        sb.append("Printer: ").append(printer.getName()).append("\n");
        sb.append("Model:   ").append(printer.getModel() != null ? printer.getModel() : "Universal POS").append("\n");
        sb.append("Purpose: ").append(printer.getPurpose()).append("\n");
        sb.append("Type:    ").append(printer.getConnectionType()).append("\n");
        sb.append("Time:    ").append(timeStr).append("\n");
        sb.append("Status:  ONLINE & OPERATIONAL\n");
        sb.append("================================\n\n\n\n");

        byte[] rawText = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] escInit = new byte[]{0x1B, 0x40}; // ESC @
        byte[] cut = new byte[]{0x1D, 0x56, 0x41, 0x10}; // GS V A 16 (cut)

        byte[] full = new byte[escInit.length + rawText.length + cut.length];
        System.arraycopy(escInit, 0, full, 0, escInit.length);
        System.arraycopy(rawText, 0, full, escInit.length, rawText.length);
        System.arraycopy(cut, 0, full, escInit.length + rawText.length, cut.length);
        return full;
    }

    @Transactional
    public PrinterAssignment assignKitchen(Tenant tenant, Printer printer, UUID kitchenId, boolean isPrimary) {
        Kitchen kitchen = kitchenRepository.findById(kitchenId)
                .orElseThrow(() -> PosException.notFound("Kitchen topilmadi: " + kitchenId));

        if (isPrimary) {
            // Unset previous primary assignments for this kitchen
            List<PrinterAssignment> existing = assignmentRepository.findByTenantIdAndKitchenIdAndActiveTrueAndDeletedAtIsNull(
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
        // Unset previous primary cashier printers
        List<PrinterAssignment> existing = assignmentRepository.findByTenantIdAndPurposeAndPrimaryTrueAndActiveTrueAndDeletedAtIsNull(
                tenant.getId(), "CASHIER");
        for (PrinterAssignment ea : existing) {
            if (!ea.getPrinter().getId().equals(printer.getId())) {
                ea.setPrimary(false);
                assignmentRepository.save(ea);
            }
        }

        PrinterAssignment assignment = assignmentRepository.findByTenantIdAndPrinterIdAndDeletedAtIsNull(tenant.getId(), printer.getId())
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

    private void validatePrinterRequest(PrinterDto.CreateRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw PosException.badRequest("Printer nomi kiritilishi shart");
        }

        String typeStr = request.getConnectionType().toUpperCase().replace("/", "");
        if ("NETWORK".equals(typeStr) || "TCPIP".equals(typeStr)) {
            if (request.getIpAddress() == null || request.getIpAddress().isBlank()) {
                throw PosException.badRequest("Network/TCP-IP printeri uchun IP manzil kiritilishi shart");
            }
            if (request.getPort() != null && (request.getPort() < 1 || request.getPort() > 65535)) {
                throw PosException.badRequest("Port raqami 1 va 65535 orasida bo'lishi kerak");
            }
        }

        if ("KITCHEN".equalsIgnoreCase(request.getPurpose()) && request.getKitchenId() == null) {
            log.info("Kitchen purpose without initial kitchen assignment");
        }

        if (request.getPaperWidth() != null && request.getPaperWidth() != 58 && request.getPaperWidth() != 80) {
            throw PosException.badRequest("Qog'oz kengligi 58mm yoki 80mm bo'lishi kerak");
        }
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
