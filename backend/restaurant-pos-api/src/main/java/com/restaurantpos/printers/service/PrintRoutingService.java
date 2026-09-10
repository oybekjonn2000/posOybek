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

import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
                log.info("Kitchen {} auto_print is OFF. Skipping ticket print.", kitchen.getName());
                continue;
            }

            Optional<PrinterAssignment> assignment = assignmentRepository
                    .findByTenantIdAndKitchenIdAndPrimaryTrueAndActiveTrueAndDeletedAtIsNull(tenantId, kitchen.getId());

            if (assignment.isEmpty()) {
                log.warn("Print Routing: {} oshxonasiga birlamchi printer biriktirilmagan! Buyurtma paneldan ko'rinadi.",
                        kitchen.getName());
                continue;
            }

            Printer printer = assignment.get().getPrinter();
            if (!printer.isAutoPrint() || !printer.isActive()) {
                log.info("Printer {} is inactive or auto_print is disabled.", printer.getName());
                continue;
            }

            printKitchenTicketSafely(printer, order, kitchen, kitchenItems, false);
        }
    }

    @Async
    @Transactional
    public void routeAndPrintReceipt(Order order, Payment payment) {
        UUID tenantId = order.getTenant().getId();

        // 1. Check autoPrint setting in payments
        boolean autoPrintSetting = appSettingRepository.findByTenantIdAndCategoryAndKey(tenantId, "PAYMENTS", "autoPrintReceiptAfterPayment")
                .map(s -> Boolean.parseBoolean(s.getValue()))
                .orElse(true);

        if (!autoPrintSetting) {
            log.info("Receipt auto-print after payment is disabled in settings.");
            return;
        }

        // 2. Find Cashier printer
        Printer cashierPrinter = findCashierPrinter(tenantId);
        if (cashierPrinter == null) {
            log.warn("Print Routing: Kassa printeri topilmadi. To'lov muvaffaqiyatli yakunlandi.");
            return;
        }

        printReceiptSafely(cashierPrinter, order, payment, false);
    }

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

    @Transactional
    public void reprintOrderReceipt(UUID tenantId, UUID orderId) {
        Order order = orderRepository.findByIdAndTenantIdAndDeletedAtIsNull(orderId, tenantId)
                .orElseThrow(() -> PosException.notFound("Buyurtma topilmadi: " + orderId));

        Payment payment = paymentRepository.findByOrderId(orderId)
                .stream().findFirst().orElse(null);

        Printer cashierPrinter = findCashierPrinter(tenantId);
        if (cashierPrinter == null) {
            throw PosException.badRequest("Kassa chek printeri topilmadi");
        }

        printReceiptSafely(cashierPrinter, order, payment, true);
    }

    private Printer findCashierPrinter(UUID tenantId) {
        // Look up in assignments first
        Optional<PrinterAssignment> cashierAssignment = assignmentRepository
                .findByTenantIdAndPurposeAndPrimaryTrueAndActiveTrueAndDeletedAtIsNull(tenantId, "CASHIER")
                .stream().findFirst();

        if (cashierAssignment.isPresent() && cashierAssignment.get().getPrinter().isActive()) {
            return cashierAssignment.get().getPrinter();
        }

        // Otherwise default printer
        return printerRepository.findByTenantIdAndIsDefaultTrueAndDeletedAtIsNull(tenantId)
                .orElseGet(() -> {
                    List<Printer> cashierPrinters = printerRepository.findByTenantIdAndPurposeAndDeletedAtIsNull(
                            tenantId, Printer.PrinterPurpose.CASHIER);
                    return cashierPrinters.isEmpty() ? null : cashierPrinters.get(0);
                });
    }

    private void printKitchenTicketSafely(Printer targetPrinter, Order order, Kitchen kitchen,
                                          List<OrderItem> items, boolean isReprint) {
        try {
            byte[] ticketBytes = buildKitchenTicketBytes(order, kitchen, items, isReprint, targetPrinter.getPaperWidth());
            sendToPrinterDevice(targetPrinter, ticketBytes);
            targetPrinter.setStatus(Printer.PrinterStatus.ONLINE);
            targetPrinter.setLastSuccessfulPrintAt(Instant.now());
            targetPrinter.setLastError(null);
            printerRepository.save(targetPrinter);
            log.info("Kitchen ticket successfully printed to {} for {}", targetPrinter.getName(), kitchen.getName());
        } catch (Exception ex) {
            log.warn("Print error on primary printer {}: {}", targetPrinter.getName(), ex.getMessage());
            targetPrinter.setStatus(Printer.PrinterStatus.OFFLINE);
            targetPrinter.setLastError("Chop etishda xatolik: " + ex.getMessage());
            printerRepository.save(targetPrinter);

            // Fallback Printer
            if (targetPrinter.getFallbackPrinter() != null && targetPrinter.getFallbackPrinter().isActive()) {
                Printer fallback = targetPrinter.getFallbackPrinter();
                log.info("Attempting fallback printer {} for kitchen {}", fallback.getName(), kitchen.getName());
                try {
                    byte[] ticketBytes = buildKitchenTicketBytes(order, kitchen, items, isReprint, fallback.getPaperWidth());
                    sendToPrinterDevice(fallback, ticketBytes);
                    fallback.setStatus(Printer.PrinterStatus.ONLINE);
                    fallback.setLastSuccessfulPrintAt(Instant.now());
                    printerRepository.save(fallback);
                    log.info("Fallback print SUCCESS: {}", fallback.getName());
                } catch (Exception fex) {
                    log.error("Fallback printer {} also failed: {}", fallback.getName(), fex.getMessage());
                    fallback.setStatus(Printer.PrinterStatus.OFFLINE);
                    fallback.setLastError("Fallback chop etishda xatolik: " + fex.getMessage());
                    printerRepository.save(fallback);
                }
            }
        }
    }

    private void printReceiptSafely(Printer cashierPrinter, Order order, Payment payment, boolean isReprint) {
        try {
            byte[] receiptBytes = buildReceiptBytes(order, payment, isReprint, cashierPrinter.getPaperWidth());
            sendToPrinterDevice(cashierPrinter, receiptBytes);
            cashierPrinter.setStatus(Printer.PrinterStatus.ONLINE);
            cashierPrinter.setLastSuccessfulPrintAt(Instant.now());
            cashierPrinter.setLastError(null);
            printerRepository.save(cashierPrinter);
            log.info("Receipt successfully printed to {}", cashierPrinter.getName());
        } catch (Exception ex) {
            log.warn("Receipt print error on printer {}: {}", cashierPrinter.getName(), ex.getMessage());
            cashierPrinter.setStatus(Printer.PrinterStatus.OFFLINE);
            cashierPrinter.setLastError("Kassa cheki chiqarishda xatolik: " + ex.getMessage());
            printerRepository.save(cashierPrinter);
        }
    }

    private void sendToPrinterDevice(Printer printer, byte[] data) throws Exception {
        Printer.ConnectionType type = printer.getConnectionType();

        if (type == Printer.ConnectionType.NETWORK || type == Printer.ConnectionType.TCPIP) {
            String ip = printer.getIpAddress();
            int port = printer.getPort() != null ? printer.getPort() : 9100;

            if (ip == null || ip.isBlank()) {
                throw new IllegalStateException("IP manzil ko'rsatilmagan");
            }

            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(ip, port), 2500);
                try (OutputStream out = socket.getOutputStream()) {
                    out.write(data);
                    out.flush();
                }
            }
        } else {
            // WINDOWS or USB: simulate or use Spooler if attached
            log.info("Simulated/Windows raw print job of {} bytes dispatched to {}", data.length, printer.getName());
        }
    }

    private byte[] buildKitchenTicketBytes(Order order, Kitchen kitchen, List<OrderItem> items,
                                           boolean isReprint, int paperWidth) {
        String timeStr = DateTimeFormatter.ofPattern("HH:mm:ss")
                .withZone(ZoneId.of("Asia/Tashkent"))
                .format(Instant.now());

        String tableName = order.getTable() != null ? order.getTable().getName() : "Olib ketish";
        String waiterName = order.getWaiter() != null ? order.getWaiter().getFullName() : "Kassir";

        StringBuilder sb = new StringBuilder();
        sb.append("\n================================\n");
        if (isReprint) {
            sb.append(" *** QAYTA CHOP ETILDI *** \n");
            sb.append("================================\n");
        }
        sb.append("       ").append(kitchen.getName().toUpperCase()).append("\n");
        sb.append("================================\n");
        sb.append("BUYURTMA: ").append(order.getOrderNumber()).append("\n");
        sb.append("STOL:     ").append(tableName).append("\n");
        sb.append("XODIM:    ").append(waiterName).append("\n");
        sb.append("VAQT:     ").append(timeStr).append("\n");
        sb.append("--------------------------------\n");

        for (OrderItem item : items) {
            sb.append(String.format("%-22s x%2.0f\n", truncate(item.getProductName(), 22), item.getQuantity()));
            if (item.getNotes() != null && !item.getNotes().isBlank()) {
                sb.append("  * ").append(item.getNotes()).append("\n");
            }
        }

        sb.append("--------------------------------\n");
        if (order.getNotes() != null && !order.getNotes().isBlank()) {
            sb.append("Izoh: ").append(order.getNotes()).append("\n");
        }
        sb.append("\n\n\n\n");

        byte[] textBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] escInit = new byte[]{0x1B, 0x40}; // ESC @
        byte[] cut = new byte[]{0x1D, 0x56, 0x41, 0x10}; // GS V A 16 (cut)

        byte[] result = new byte[escInit.length + textBytes.length + cut.length];
        System.arraycopy(escInit, 0, result, 0, escInit.length);
        System.arraycopy(textBytes, 0, result, escInit.length, textBytes.length);
        System.arraycopy(cut, 0, result, escInit.length + textBytes.length, cut.length);
        return result;
    }

    private byte[] buildReceiptBytes(Order order, Payment payment, boolean isReprint, int paperWidth) {
        String timeStr = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.of("Asia/Tashkent"))
                .format(Instant.now());

        String tableName = order.getTable() != null ? order.getTable().getName() : "Olib ketish";
        String cashierName = payment != null && payment.getCashier() != null ? payment.getCashier().getFullName() : "Kassir";

        StringBuilder sb = new StringBuilder();
        sb.append("\n================================\n");
        sb.append("   OYBEK RESTAURANT & LOUNGE    \n");
        sb.append("   Qarshi sh., Mustaqillik shox \n");
        sb.append("   Tel: +998 90 123 45 67       \n");
        sb.append("================================\n");
        if (isReprint) {
            sb.append(" *** NUSXA / REPRINT CHEK ***  \n");
            sb.append("================================\n");
        }
        sb.append("CHEK:     ").append(payment != null ? payment.getPaymentNumber() : order.getOrderNumber()).append("\n");
        sb.append("STOL:     ").append(tableName).append("\n");
        sb.append("KASSIR:   ").append(cashierName).append("\n");
        sb.append("SANA:     ").append(timeStr).append("\n");
        sb.append("--------------------------------\n");
        sb.append("MAHSULOT          MIQDOR   SUMMA\n");
        sb.append("--------------------------------\n");

        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                if (item.isVoided()) continue;
                String line = String.format("%-16s %2.0f x %7.0f\n",
                        truncate(item.getProductName(), 16),
                        item.getQuantity(),
                        item.getSubtotal());
                sb.append(line);
            }
        }

        sb.append("--------------------------------\n");
        sb.append(String.format("ORALIQ JAMI:      %12.0f\n", order.getSubtotal()));
        if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("CHEGIRMA:        -%12.0f\n", order.getDiscountAmount()));
        }
        if (order.getTaxAmount() != null && order.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("SOLIQ (QQS):     +%12.0f\n", order.getTaxAmount()));
        }
        sb.append("================================\n");
        sb.append(String.format("TO'LANISHI KERAK: %12.0f so'm\n", order.getTotal()));
        if (payment != null) {
            sb.append(String.format("TO'LOV USULI:     %12s\n", payment.getPaymentMethod()));
            sb.append(String.format("TO'LANDI:         %12.0f so'm\n", payment.getAmount()));
            if (payment.getChangeAmount() != null && payment.getChangeAmount().compareTo(BigDecimal.ZERO) > 0) {
                sb.append(String.format("QAYTIM:           %12.0f so'm\n", payment.getChangeAmount()));
            }
        }
        sb.append("================================\n");
        sb.append("    XARIDINGIZ UCHUN RAHMAT!    \n");
        sb.append("      YANA KUTIB QOLAMIZ!       \n");
        sb.append("\n\n\n\n");

        byte[] textBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] escInit = new byte[]{0x1B, 0x40};
        byte[] cut = new byte[]{0x1D, 0x56, 0x41, 0x10};

        byte[] result = new byte[escInit.length + textBytes.length + cut.length];
        System.arraycopy(escInit, 0, result, 0, escInit.length);
        System.arraycopy(textBytes, 0, result, escInit.length, textBytes.length);
        System.arraycopy(cut, 0, result, escInit.length + textBytes.length, cut.length);
        return result;
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max);
    }
}
