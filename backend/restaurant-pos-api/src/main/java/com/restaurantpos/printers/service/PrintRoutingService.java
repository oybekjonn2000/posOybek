package com.restaurantpos.printers.service;

import com.restaurantpos.common.exception.PosException;
import com.restaurantpos.kitchen.entity.Kitchen;
import com.restaurantpos.kitchen.entity.KitchenTicket;
import com.restaurantpos.kitchen.repository.KitchenTicketRepository;
import com.restaurantpos.orders.entity.Order;
import com.restaurantpos.orders.entity.OrderItem;
import com.restaurantpos.orders.repository.OrderRepository;
import com.restaurantpos.payments.entity.Payment;
import com.restaurantpos.payments.repository.PaymentRepository;
import com.restaurantpos.printers.entity.Printer;
import com.restaurantpos.printers.entity.PrinterAssignment;
import com.restaurantpos.printers.repository.PrinterAssignmentRepository;
import com.restaurantpos.printers.repository.PrinterRepository;
import com.restaurantpos.settings.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrintRoutingService {

    private final PrinterRepository printerRepository;
    private final PrinterAssignmentRepository assignmentRepository;
    private final KitchenTicketRepository kitchenTicketRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final AppSettingRepository appSettingRepository;
    private final KitchenPrintService kitchenPrintService;
    private final ReceiptPrintService receiptPrintService;

    /**
     * Dispatches order items to their respective kitchen station primary printers.
     * CRITICAL: Printer errors or unassigned printers MUST NEVER abort or roll back the order.
     */
    @Async
    @Transactional
    public void routeAndPrintKitchenTickets(Order order, List<OrderItem> items) {
        if (items == null || items.isEmpty()) return;

        Map<Kitchen, List<OrderItem>> byKitchen = items.stream()
                .filter(i -> i.getKitchen() != null)
                .collect(Collectors.groupingBy(OrderItem::getKitchen));

        UUID tenantId = order.getTenant().getId();

        for (Map.Entry<Kitchen, List<OrderItem>> entry : byKitchen.entrySet()) {
            Kitchen kitchen = entry.getKey();
            List<OrderItem> kitchenItems = entry.getValue();

            if (!kitchen.isAutoPrint()) {
                log.info("Kitchen '{}' auto_print is OFF. Skipping ticket print.", kitchen.getName());
                continue;
            }

            Optional<PrinterAssignment> assignment = assignmentRepository
                    .findByTenantIdAndKitchenIdAndPrimaryTrueAndActiveTrueAndDeletedAtIsNull(tenantId, kitchen.getId());

            if (assignment.isEmpty()) {
                log.warn("Print Routing: '{}' oshxonasiga birlamchi printer biriktirilmagan! Buyurtma paneldan ko'rinadi.",
                        kitchen.getName());
                continue;
            }

            Printer printer = assignment.get().getPrinter();
            if (!printer.isAutoPrint() || !printer.isActive()) {
                log.info("Printer '{}' is inactive or auto_print is disabled.", printer.getName());
                continue;
            }

            printKitchenTicketSafely(printer, order, kitchen, kitchenItems, false);
        }
    }

    /**
     * Routes single item cancellation ticket to the respective kitchen printer.
     */
    @Async
    @Transactional
    public void routeAndPrintKitchenCancellation(Order order, Kitchen kitchen, String productName,
                                                 java.math.BigDecimal quantity, String reason,
                                                 String cancelledByName, String receiptNumber) {
        if (kitchen == null) return;
        UUID tenantId = order.getTenant().getId();

        Printer printer = resolveKitchenPrinter(tenantId, kitchen);
        if (printer == null) {
            log.warn("Print Routing: '{}' oshxonasi uchun faol printer topilmadi! Bekor cheki chop etilmadi.",
                    kitchen.getName());
            return;
        }

        printKitchenCancellationSafely(printer, order, kitchen, productName, quantity, reason, cancelledByName, receiptNumber);
    }

    /**
     * Routes entire order cancellation to all affected kitchens' printers.
     */
    @Async
    @Transactional
    public void routeAndPrintFullOrderCancellation(Order order, String reason,
                                                   String cancelledByName, String receiptNumber) {
        if (order.getItems() == null || order.getItems().isEmpty()) return;

        Map<Kitchen, List<OrderItem>> byKitchen = order.getItems().stream()
                .filter(i -> i.getKitchen() != null)
                .collect(Collectors.groupingBy(OrderItem::getKitchen));

        UUID tenantId = order.getTenant().getId();

        for (Map.Entry<Kitchen, List<OrderItem>> entry : byKitchen.entrySet()) {
            Kitchen kitchen = entry.getKey();
            List<OrderItem> kitchenItems = entry.getValue();

            Printer printer = resolveKitchenPrinter(tenantId, kitchen);
            if (printer == null) {
                log.warn("Print Routing: '{}' oshxonasi uchun printer topilmadi! Bekor cheki chop etilmadi.",
                        kitchen.getName());
                continue;
            }

            printBatchKitchenCancellationSafely(printer, order, kitchen, kitchenItems, reason, cancelledByName, receiptNumber);
        }
    }

    private Printer resolveKitchenPrinter(UUID tenantId, Kitchen kitchen) {
        if (kitchen == null) return null;

        // 1. Primary assigned printer for this kitchen
        Optional<PrinterAssignment> assignment = assignmentRepository
                .findByTenantIdAndKitchenIdAndPrimaryTrueAndActiveTrueAndDeletedAtIsNull(tenantId, kitchen.getId());
        if (assignment.isPresent() && assignment.get().getPrinter().isActive()) {
            return assignment.get().getPrinter();
        }

        // 2. Any active assignment for this kitchen
        List<PrinterAssignment> anyAssignments = assignmentRepository
                .findByTenantIdAndKitchenIdAndActiveTrueAndDeletedAtIsNull(tenantId, kitchen.getId());
        for (PrinterAssignment pa : anyAssignments) {
            if (pa.getPrinter().isActive()) return pa.getPrinter();
        }

        // 3. Any active printer with purpose KITCHEN
        List<Printer> kitchenPrinters = printerRepository
                .findByTenantIdAndPurposeAndDeletedAtIsNull(tenantId, Printer.PrinterPurpose.KITCHEN);
        for (Printer kp : kitchenPrinters) {
            if (kp.isActive()) return kp;
        }

        // 4. Default printer in system (if exists)
        Optional<Printer> defaultPrinter = printerRepository
                .findByTenantIdAndIsDefaultTrueAndDeletedAtIsNull(tenantId);
        if (defaultPrinter.isPresent() && defaultPrinter.get().isActive()) {
            return defaultPrinter.get();
        }

        // 5. Any active printer in tenant
        List<Printer> any = printerRepository.findByTenantIdAndDeletedAtIsNullOrderByCreatedAtAsc(tenantId);
        for (Printer p : any) {
            if (p.isActive()) return p;
        }

        return null;
    }

    /**
     * Dispatches receipt print job to the primary cashier printer.
     * Guaranteed duplicate prevention & safe non-blocking execution.
     */
    @Transactional
    public Order routeAndPrintReceipt(Order order, Payment payment) {
        UUID tenantId = order.getTenant().getId();
        UUID orderId = order.getId();
        UUID paymentId = payment != null ? payment.getId() : null;

        // 1. DUPLICATE CHECK: If order is already PRINTED, skip duplicate print
        if (order.getReceiptPrintStatus() == Order.ReceiptPrintStatus.PRINTED) {
            log.info("Duplicate receipt print prevented for order '{}' ({}) - already PRINTED.",
                    order.getOrderNumber(), orderId);
            return order;
        }

        order.setReceiptPrintStatus(Order.ReceiptPrintStatus.PRINTING);
        order.setReceiptPrintAttempts(order.getReceiptPrintAttempts() + 1);
        order = orderRepository.save(order);

        // 2. Setting check
        boolean autoPrintSetting = appSettingRepository.findByTenantIdAndCategoryAndKey(tenantId, "PAYMENTS", "autoPrintReceiptAfterPayment")
                .map(s -> Boolean.parseBoolean(s.getValue()))
                .orElse(true);

        if (!autoPrintSetting) {
            log.info("Receipt auto-print after payment is disabled in settings for order '{}'", order.getOrderNumber());
            order.setReceiptPrintStatus(Order.ReceiptPrintStatus.NOT_PRINTED);
            return orderRepository.save(order);
        }

        // 3. Find Cashier Printer
        Printer cashierPrinter = findCashierPrinter(tenantId);
        if (cashierPrinter == null) {
            log.error("[PRINT_FAILED] Receipt print failed for orderId: {}, paymentId: {}, error: Kassa chek printeri topilmadi yoki sozlanmagan",
                    orderId, paymentId);
            order.setReceiptPrintStatus(Order.ReceiptPrintStatus.PRINT_FAILED);
            order.setReceiptPrintError("Kassa chek printeri topilmadi yoki sozlanmagan");
            return orderRepository.save(order);
        }

        return printReceiptSafely(cashierPrinter, order, payment, false);
    }

    /**
     * Reprints a kitchen ticket without creating new orders or tickets.
     */
    @Transactional
    public void reprintKitchenTicket(UUID tenantId, UUID ticketId) {
        KitchenTicket ticket = kitchenTicketRepository.findById(ticketId)
                .orElseThrow(() -> PosException.notFound("Kitchen ticket topilmadi: " + ticketId));

        Order order = ticket.getOrder();
        Kitchen kitchen = ticket.getKitchen();

        List<OrderItem> items = order.getItems().stream()
                .filter(i -> kitchen.getId().equals(i.getKitchen().getId()) && !i.isVoided())
                .collect(Collectors.toList());

        Optional<PrinterAssignment> assignment = assignmentRepository
                .findByTenantIdAndKitchenIdAndPrimaryTrueAndActiveTrueAndDeletedAtIsNull(tenantId, kitchen.getId());

        if (assignment.isEmpty()) {
            throw PosException.badRequest("Ushbu oshxonaga printer biriktirilmagan: " + kitchen.getName());
        }

        Printer printer = assignment.get().getPrinter();
        printKitchenTicketSafely(printer, order, kitchen, items, true);
    }

    /**
     * Reprints all kitchen tickets for an order without creating new orders or tickets.
     */
    @Transactional
    public void reprintAllKitchenTicketsForOrder(UUID tenantId, UUID orderId) {
        Order order = orderRepository.findByIdAndTenantIdAndDeletedAtIsNull(orderId, tenantId)
                .orElseThrow(() -> PosException.notFound("Buyurtma topilmadi: " + orderId));

        List<OrderItem> items = order.getItems().stream()
                .filter(i -> !i.isVoided() && i.getKitchen() != null)
                .collect(Collectors.toList());

        if (items.isEmpty()) {
            throw PosException.badRequest("Buyurtmada oshxona taomlari yo'q");
        }

        Map<Kitchen, List<OrderItem>> byKitchen = items.stream()
                .collect(Collectors.groupingBy(OrderItem::getKitchen));

        for (Map.Entry<Kitchen, List<OrderItem>> entry : byKitchen.entrySet()) {
            Kitchen kitchen = entry.getKey();
            List<OrderItem> kitchenItems = entry.getValue();

            Optional<PrinterAssignment> assignment = assignmentRepository
                    .findByTenantIdAndKitchenIdAndPrimaryTrueAndActiveTrueAndDeletedAtIsNull(tenantId, kitchen.getId());

            if (assignment.isEmpty()) {
                log.warn("Reprint: '{}' oshxonasiga printer biriktirilmagan", kitchen.getName());
                continue;
            }

            Printer printer = assignment.get().getPrinter();
            printKitchenTicketSafely(printer, order, kitchen, kitchenItems, true);
        }
    }

    /**
     * Reprints an order receipt without creating new payments or billing.
     */
    @Transactional
    public void reprintOrderReceipt(UUID tenantId, UUID orderId) {
        Order order = orderRepository.findByIdAndTenantIdAndDeletedAtIsNull(orderId, tenantId)
                .orElseThrow(() -> PosException.notFound("Buyurtma topilmadi: " + orderId));

        Payment payment = paymentRepository.findByOrderId(orderId)
                .stream().findFirst().orElse(null);

        Printer cashierPrinter = findCashierPrinter(tenantId);
        if (cashierPrinter == null) {
            log.error("[REPRINT_FAILED] Kassa chek printeri topilmadi for orderId: {}, paymentId: {}",
                    orderId, payment != null ? payment.getId() : "NONE");
            order.setReceiptPrintStatus(Order.ReceiptPrintStatus.PRINT_FAILED);
            order.setReceiptPrintError("Kassa chek printeri topilmadi yoki sozlanmagan");
            orderRepository.save(order);
            throw PosException.badRequest("Kassa chek printeri topilmadi yoki sozlanmagan");
        }

        order.setReceiptPrintAttempts(order.getReceiptPrintAttempts() + 1);
        printReceiptSafely(cashierPrinter, order, payment, true);
    }

    public Printer findCashierPrinter(UUID tenantId) {
        // 1. Look up in assignments first for purpose CASHIER or RECEIPT
        List<PrinterAssignment> assignments = assignmentRepository
                .findByTenantIdAndActiveTrueAndDeletedAtIsNull(tenantId);

        Optional<PrinterAssignment> cashierAssignment = assignments.stream()
                .filter(a -> "CASHIER".equalsIgnoreCase(a.getPurpose())
                        && a.isPrimary() && a.getPrinter().isActive())
                .findFirst();

        if (cashierAssignment.isPresent()) {
            return cashierAssignment.get().getPrinter();
        }

        // 2. Default printer
        Optional<Printer> defaultPrinter = printerRepository.findByTenantIdAndIsDefaultTrueAndDeletedAtIsNull(tenantId);
        if (defaultPrinter.isPresent() && defaultPrinter.get().isActive()) {
            return defaultPrinter.get();
        }

        // 3. Printer with purpose CASHIER directly
        List<Printer> cashierPrinters = printerRepository.findByTenantIdAndPurposeAndDeletedAtIsNull(
                tenantId, Printer.PrinterPurpose.CASHIER);
        for (Printer p : cashierPrinters) {
            if (p.isActive()) return p;
        }

        // 4. Fallback to any active configured printer
        List<Printer> anyPrinters = printerRepository.findByTenantIdAndDeletedAtIsNullOrderByCreatedAtAsc(tenantId);
        for (Printer p : anyPrinters) {
            if (p.isActive()) return p;
        }
        return null;
    }

    private void printKitchenTicketSafely(Printer targetPrinter, Order order, Kitchen kitchen,
                                          List<OrderItem> items, boolean isReprint) {
        try {
            kitchenPrintService.printKitchenTicket(targetPrinter, order, kitchen, items, isReprint);
            targetPrinter.setStatus(Printer.PrinterStatus.ONLINE);
            targetPrinter.setLastSuccessfulPrintAt(Instant.now());
            targetPrinter.setLastError(null);
            printerRepository.save(targetPrinter);
            log.info("Kitchen ticket successfully printed to '{}' for '{}'", targetPrinter.getName(), kitchen.getName());
        } catch (Exception ex) {
            log.warn("Print error on kitchen primary printer '{}': {}", targetPrinter.getName(), ex.getMessage());
            targetPrinter.setStatus(Printer.PrinterStatus.OFFLINE);
            targetPrinter.setLastError("Chop etishda xatolik: " + ex.getMessage());
            printerRepository.save(targetPrinter);

            // Fallback Printer if configured
            if (targetPrinter.getFallbackPrinter() != null && targetPrinter.getFallbackPrinter().isActive()) {
                Printer fallback = targetPrinter.getFallbackPrinter();
                log.info("Attempting fallback printer '{}' for kitchen '{}'", fallback.getName(), kitchen.getName());
                try {
                    kitchenPrintService.printKitchenTicket(fallback, order, kitchen, items, isReprint);
                    fallback.setStatus(Printer.PrinterStatus.ONLINE);
                    fallback.setLastSuccessfulPrintAt(Instant.now());
                    printerRepository.save(fallback);
                    log.info("Fallback print SUCCESS: '{}'", fallback.getName());
                } catch (Exception fex) {
                    log.error("Fallback printer '{}' also failed: {}", fallback.getName(), fex.getMessage());
                    fallback.setStatus(Printer.PrinterStatus.OFFLINE);
                    fallback.setLastError("Fallback chop etishda xatolik: " + fex.getMessage());
                    printerRepository.save(fallback);
                }
            }
        }
    }

    private void printKitchenCancellationSafely(Printer targetPrinter, Order order, Kitchen kitchen,
                                                String productName, java.math.BigDecimal quantity, String reason,
                                                String cancelledByName, String receiptNumber) {
        try {
            kitchenPrintService.printKitchenCancellationTicket(targetPrinter, order, kitchen,
                    productName, quantity, reason, cancelledByName, receiptNumber);
            targetPrinter.setStatus(Printer.PrinterStatus.ONLINE);
            targetPrinter.setLastSuccessfulPrintAt(Instant.now());
            targetPrinter.setLastError(null);
            printerRepository.save(targetPrinter);
            log.info("Kitchen cancellation ticket successfully printed to '{}' for '{}'",
                    targetPrinter.getName(), kitchen.getName());
        } catch (Exception ex) {
            log.warn("Print error on kitchen cancellation printer '{}': {}", targetPrinter.getName(), ex.getMessage());
            targetPrinter.setStatus(Printer.PrinterStatus.OFFLINE);
            targetPrinter.setLastError("Bekor cheki chop etishda xatolik: " + ex.getMessage());
            printerRepository.save(targetPrinter);

            if (targetPrinter.getFallbackPrinter() != null && targetPrinter.getFallbackPrinter().isActive()) {
                Printer fallback = targetPrinter.getFallbackPrinter();
                try {
                    kitchenPrintService.printKitchenCancellationTicket(fallback, order, kitchen,
                            productName, quantity, reason, cancelledByName, receiptNumber);
                    fallback.setStatus(Printer.PrinterStatus.ONLINE);
                    fallback.setLastSuccessfulPrintAt(Instant.now());
                    printerRepository.save(fallback);
                    log.info("Fallback cancellation print SUCCESS: '{}'", fallback.getName());
                } catch (Exception fex) {
                    log.error("Fallback printer '{}' also failed: {}", fallback.getName(), fex.getMessage());
                    fallback.setStatus(Printer.PrinterStatus.OFFLINE);
                    fallback.setLastError("Fallback bekor cheki chop etishda xatolik: " + fex.getMessage());
                    printerRepository.save(fallback);
                }
            }
        }
    }

    private void printBatchKitchenCancellationSafely(Printer targetPrinter, Order order, Kitchen kitchen,
                                                     List<OrderItem> items, String reason,
                                                     String cancelledByName, String receiptNumber) {
        try {
            kitchenPrintService.printKitchenCancellationTickets(targetPrinter, order, kitchen,
                    items, reason, cancelledByName, receiptNumber);
            targetPrinter.setStatus(Printer.PrinterStatus.ONLINE);
            targetPrinter.setLastSuccessfulPrintAt(Instant.now());
            targetPrinter.setLastError(null);
            printerRepository.save(targetPrinter);
            log.info("Batch kitchen cancellation ticket successfully printed to '{}' for '{}'",
                    targetPrinter.getName(), kitchen.getName());
        } catch (Exception ex) {
            log.warn("Print error on batch kitchen cancellation printer '{}': {}", targetPrinter.getName(), ex.getMessage());
            targetPrinter.setStatus(Printer.PrinterStatus.OFFLINE);
            targetPrinter.setLastError("Bekor cheki chop etishda xatolik: " + ex.getMessage());
            printerRepository.save(targetPrinter);

            if (targetPrinter.getFallbackPrinter() != null && targetPrinter.getFallbackPrinter().isActive()) {
                Printer fallback = targetPrinter.getFallbackPrinter();
                try {
                    kitchenPrintService.printKitchenCancellationTickets(fallback, order, kitchen,
                            items, reason, cancelledByName, receiptNumber);
                    fallback.setStatus(Printer.PrinterStatus.ONLINE);
                    fallback.setLastSuccessfulPrintAt(Instant.now());
                    printerRepository.save(fallback);
                    log.info("Fallback batch cancellation print SUCCESS: '{}'", fallback.getName());
                } catch (Exception fex) {
                    log.error("Fallback printer '{}' also failed: {}", fallback.getName(), fex.getMessage());
                    fallback.setStatus(Printer.PrinterStatus.OFFLINE);
                    fallback.setLastError("Fallback bekor cheki chop etishda xatolik: " + fex.getMessage());
                    printerRepository.save(fallback);
                }
            }
        }
    }

    private Order printReceiptSafely(Printer cashierPrinter, Order order, Payment payment, boolean isReprint) {
        UUID orderId = order.getId();
        UUID paymentId = payment != null ? payment.getId() : null;
        String printerName = cashierPrinter != null ? cashierPrinter.getName() : "UNKNOWN";

        if (isReprint) {
            log.info("[REPRINT_STARTED] Receipt reprint started for orderId: {}, paymentId: {}, printer: '{}'",
                    orderId, paymentId, printerName);
        } else {
            log.info("[PRINT_STARTED] Receipt print started for orderId: {}, paymentId: {}, printer: '{}'",
                    orderId, paymentId, printerName);
        }

        try {
            receiptPrintService.printReceipt(cashierPrinter, order, payment, isReprint);
            cashierPrinter.setStatus(Printer.PrinterStatus.ONLINE);
            cashierPrinter.setLastSuccessfulPrintAt(Instant.now());
            cashierPrinter.setLastError(null);
            printerRepository.save(cashierPrinter);

            order.setReceiptPrintStatus(Order.ReceiptPrintStatus.PRINTED);
            order.setReceiptPrintedAt(Instant.now());
            order.setReceiptPrintError(null);
            order = orderRepository.save(order);

            if (isReprint) {
                log.info("[REPRINT_SUCCESS] Receipt reprint successful for orderId: {}, paymentId: {}, printer: '{}'",
                        orderId, paymentId, printerName);
            } else {
                log.info("[PRINT_SUCCESS] Receipt printed successfully for orderId: {}, paymentId: {}, printer: '{}'",
                        orderId, paymentId, printerName);
            }
            return order;
        } catch (Exception ex) {
            String errorMsg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            cashierPrinter.setStatus(Printer.PrinterStatus.OFFLINE);
            cashierPrinter.setLastError("Kassa cheki chiqarishda xatolik: " + errorMsg);
            printerRepository.save(cashierPrinter);

            order.setReceiptPrintStatus(Order.ReceiptPrintStatus.PRINT_FAILED);
            order.setReceiptPrintError(errorMsg);
            order = orderRepository.save(order);

            if (isReprint) {
                log.error("[REPRINT_FAILED] Receipt reprint failed for orderId: {}, paymentId: {}, printer: '{}', error: {}",
                        orderId, paymentId, printerName, errorMsg);
                throw PosException.badRequest("Chekni qayta chop etishda xatolik: " + errorMsg);
            } else {
                log.error("[PRINT_FAILED] Receipt print failed for orderId: {}, paymentId: {}, printer: '{}', error: {}",
                        orderId, paymentId, printerName, errorMsg);
            }
            return order;
        }
    }
}
