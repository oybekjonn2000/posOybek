package com.restaurantpos.printers.service;

import com.restaurantpos.common.exception.PosException;
import com.restaurantpos.kitchen.entity.Kitchen;
import com.restaurantpos.orders.entity.Order;
import com.restaurantpos.orders.entity.OrderItem;
import com.restaurantpos.printers.entity.Printer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KitchenPrintService {

    private final WindowsPrintService windowsPrintService;

    /**
     * Formats and prints a kitchen ticket for the specified kitchen and items.
     */
    public void printKitchenTicket(Printer printer, Order order, Kitchen kitchen,
                                   List<OrderItem> items, boolean isReprint) {
        if (printer.getStatus() == Printer.PrinterStatus.OFFLINE) {
            throw PosException.badRequest("Printer OFFLINE holatda: " + printer.getName());
        }

        int width = printer.getPaperWidth() <= 58 ? 58 : 80;
        int maxChars = width == 58 ? 32 : 42;

        String timeStr = DateTimeFormatter.ofPattern("HH:mm:ss")
                .withZone(ZoneId.of("Asia/Tashkent"))
                .format(Instant.now());

        String tableName = order.getTable() != null ? order.getTable().getName() : "Olib ketish";
        String waiterName = order.getWaiter() != null ? order.getWaiter().getFullName() : "Kassir";

        String divider = width == 58 ? "--------------------------------\n" : "------------------------------------------\n";
        String doubleDivider = width == 58 ? "================================\n" : "==========================================\n";

        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(doubleDivider);
        if (isReprint) {
            sb.append(centerText("*** QAYTA CHOP ETILDI ***", maxChars)).append("\n");
            sb.append(doubleDivider);
        }

        if (order.getOrderType() == Order.OrderType.DELIVERY) {
            sb.append(centerText("🚚 DELIVERY ORDER", maxChars)).append("\n");
            sb.append(doubleDivider);
        }

        sb.append(centerText("OSHXONA: " + kitchen.getName().toUpperCase(), maxChars)).append("\n");
        sb.append(doubleDivider);
        sb.append(String.format("BUYURTMA: %s\n", order.getOrderNumber()));
        if (order.getOrderType() == Order.OrderType.DELIVERY) {
            if (order.getCustomer() != null && order.getCustomer().getFullName() != null) {
                sb.append(String.format("MIJOZ:    %s\n", truncate(order.getCustomer().getFullName(), maxChars - 10)));
            }
            if (order.getDeliveryPhone() != null && !order.getDeliveryPhone().isBlank()) {
                sb.append(String.format("TEL:      %s\n", order.getDeliveryPhone()));
            }
            if (order.getDeliveryAddress() != null && !order.getDeliveryAddress().isBlank()) {
                sb.append(String.format("MANZIL:   %s\n", truncate(order.getDeliveryAddress(), maxChars - 10)));
            }
        } else {
            sb.append(String.format("STOL:     %s\n", tableName));
            sb.append(String.format("XODIM:    %s\n", waiterName));
        }
        sb.append(String.format("VAQT:     %s\n", timeStr));
        sb.append(divider);

        int nameCol = width == 58 ? 22 : 32;
        for (OrderItem item : items) {
            String name = truncate(item.getProductName(), nameCol);
            java.math.BigDecimal printQty;
            if (isReprint) {
                printQty = item.getQuantity();
            } else {
                java.math.BigDecimal rem = item.getRemainingQuantity();
                printQty = (rem != null && rem.compareTo(java.math.BigDecimal.ZERO) > 0) ? rem : item.getQuantity();
            }
            sb.append(String.format("%-" + nameCol + "s x%2.0f\n", name, printQty));
            if (!isReprint && item.getQuantity() != null && item.getQuantity().compareTo(printQty) > 0) {
                sb.append(String.format("  (Qo'shimcha: +%.0f, Jami: %.0f)\n", printQty, item.getQuantity()));
            }
            if (item.getNotes() != null && !item.getNotes().isBlank()) {
                sb.append("  * ").append(item.getNotes().trim()).append("\n");
            }
        }

        sb.append(divider);
        String notes = order.getKitchenNotes() != null && !order.getKitchenNotes().isBlank()
                ? order.getKitchenNotes() : order.getNotes();
        if (notes != null && !notes.isBlank()) {
            sb.append("Izoh: ").append(notes.trim()).append("\n");
            sb.append(divider);
        }

        sb.append("\n\n\n\n");

        String textContent = sb.toString();
        byte[] rawBytes = buildEscPosPayload(textContent);

        String winName = printer.getWindowsPrinterName();
        log.info("Sending kitchen ticket ({} items) to Windows printer '{}' for kitchen '{}'",
                items.size(), winName, kitchen.getName());

        windowsPrintService.print(winName, rawBytes, textContent);
    }

    /**
     * Formats and prints a cancellation ticket for a single item belonging to a kitchen.
     */
    public void printKitchenCancellationTicket(Printer printer, Order order, Kitchen kitchen,
                                               String productName, java.math.BigDecimal quantity,
                                               String reason, String cancelledByName, String receiptNumber) {
        if (printer.getStatus() == Printer.PrinterStatus.OFFLINE) {
            throw PosException.badRequest("Printer OFFLINE holatda: " + printer.getName());
        }

        int width = printer.getPaperWidth() <= 58 ? 58 : 80;
        int maxChars = width == 58 ? 32 : 42;

        String timeStr = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.of("Asia/Tashkent"))
                .format(Instant.now());

        String tableName = order.getTable() != null ? order.getTable().getName() : "Olib ketish";

        String divider = width == 58 ? "--------------------------------\n" : "------------------------------------------\n";
        String doubleDivider = width == 58 ? "================================\n" : "==========================================\n";

        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(doubleDivider);
        sb.append(centerText("!!! BEKOR QILINDI / OTMENA !!!", maxChars)).append("\n");
        sb.append(doubleDivider);

        sb.append(centerText("OSHXONA: " + kitchen.getName().toUpperCase(), maxChars)).append("\n");
        sb.append(doubleDivider);
        sb.append(String.format("BUYURTMA:    %s\n", order.getOrderNumber()));
        if (receiptNumber != null && !receiptNumber.isBlank()) {
            sb.append(String.format("CHEK RAQAMI: %s\n", receiptNumber));
        }
        sb.append(String.format("STOL:        %s\n", tableName));
        sb.append(String.format("BEKOR QILDI: %s\n", cancelledByName != null ? cancelledByName : "Xodim"));
        sb.append(String.format("VAQT:        %s\n", timeStr));
        sb.append(divider);

        sb.append("BEKOR BO'LGAN TAOM:\n");
        int nameCol = width == 58 ? 24 : 34;
        String name = truncate(productName, nameCol);
        sb.append(String.format(">> %-" + nameCol + "s x%2.0f\n", name, quantity));
        sb.append(divider);

        sb.append(String.format("SABABI: %s\n", reason != null ? reason : "Mijoz rad etdi"));
        sb.append(divider);

        sb.append(centerText("*** TAYYORLASH TO'XTATILSIN! ***", maxChars)).append("\n");
        sb.append(doubleDivider);
        sb.append("\n\n\n\n");

        String textContent = sb.toString();
        byte[] rawBytes = buildEscPosPayload(textContent);

        String winName = printer.getWindowsPrinterName();
        log.info("Sending kitchen CANCELLATION ticket to Windows printer '{}' for kitchen '{}'",
                winName, kitchen.getName());

        windowsPrintService.print(winName, rawBytes, textContent);
    }

    /**
     * Formats and prints a cancellation ticket for multiple items belonging to a kitchen.
     */
    public void printKitchenCancellationTickets(Printer printer, Order order, Kitchen kitchen,
                                                List<OrderItem> items, String reason,
                                                String cancelledByName, String receiptNumber) {
        if (printer.getStatus() == Printer.PrinterStatus.OFFLINE) {
            throw PosException.badRequest("Printer OFFLINE holatda: " + printer.getName());
        }

        int width = printer.getPaperWidth() <= 58 ? 58 : 80;
        int maxChars = width == 58 ? 32 : 42;

        String timeStr = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.of("Asia/Tashkent"))
                .format(Instant.now());

        String tableName = order.getTable() != null ? order.getTable().getName() : "Olib ketish";

        String divider = width == 58 ? "--------------------------------\n" : "------------------------------------------\n";
        String doubleDivider = width == 58 ? "================================\n" : "==========================================\n";

        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(doubleDivider);
        sb.append(centerText("!!! BEKOR QILINDI / OTMENA !!!", maxChars)).append("\n");
        sb.append(doubleDivider);

        sb.append(centerText("OSHXONA: " + kitchen.getName().toUpperCase(), maxChars)).append("\n");
        sb.append(doubleDivider);
        sb.append(String.format("BUYURTMA:    %s\n", order.getOrderNumber()));
        if (receiptNumber != null && !receiptNumber.isBlank()) {
            sb.append(String.format("CHEK RAQAMI: %s\n", receiptNumber));
        }
        sb.append(String.format("STOL:        %s\n", tableName));
        sb.append(String.format("BEKOR QILDI: %s\n", cancelledByName != null ? cancelledByName : "Xodim"));
        sb.append(String.format("VAQT:        %s\n", timeStr));
        sb.append(divider);

        sb.append("BEKOR BO'LGAN TAOMLAR:\n");
        int nameCol = width == 58 ? 24 : 34;
        for (OrderItem item : items) {
            String name = truncate(item.getProductName(), nameCol);
            java.math.BigDecimal qty = item.getCancelledQuantity() != null && item.getCancelledQuantity().compareTo(java.math.BigDecimal.ZERO) > 0
                    ? item.getCancelledQuantity() : item.getQuantity();
            sb.append(String.format(">> %-" + nameCol + "s x%2.0f\n", name, qty));
        }
        sb.append(divider);

        sb.append(String.format("SABABI: %s\n", reason != null ? reason : "Mijoz rad etdi"));
        sb.append(divider);

        sb.append(centerText("*** TAYYORLASH TO'XTATILSIN! ***", maxChars)).append("\n");
        sb.append(doubleDivider);
        sb.append("\n\n\n\n");

        String textContent = sb.toString();
        byte[] rawBytes = buildEscPosPayload(textContent);

        String winName = printer.getWindowsPrinterName();
        log.info("Sending batch kitchen CANCELLATION ticket ({} items) to Windows printer '{}' for kitchen '{}'",
                items.size(), winName, kitchen.getName());

        windowsPrintService.print(winName, rawBytes, textContent);
    }

    private byte[] buildEscPosPayload(String text) {
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] escInit = new byte[]{0x1B, 0x40}; // ESC @ (Initialize printer)
        byte[] cut = new byte[]{0x1D, 0x56, 0x41, 0x10}; // GS V A 16 (Cut paper)

        byte[] result = new byte[escInit.length + textBytes.length + cut.length];
        System.arraycopy(escInit, 0, result, 0, escInit.length);
        System.arraycopy(textBytes, 0, result, escInit.length, textBytes.length);
        System.arraycopy(cut, 0, result, escInit.length + textBytes.length, cut.length);
        return result;
    }

    private String centerText(String text, int width) {
        if (text.length() >= width) return text;
        int pad = (width - text.length()) / 2;
        return " ".repeat(pad) + text;
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max);
    }
}
