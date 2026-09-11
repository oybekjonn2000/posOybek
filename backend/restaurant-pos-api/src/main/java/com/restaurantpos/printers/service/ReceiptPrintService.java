package com.restaurantpos.printers.service;

import com.restaurantpos.common.exception.PosException;
import com.restaurantpos.orders.entity.Order;
import com.restaurantpos.orders.entity.OrderItem;
import com.restaurantpos.payments.entity.Payment;
import com.restaurantpos.printers.entity.Printer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptPrintService {

    private final WindowsPrintService windowsPrintService;

    /**
     * Formats and prints a customer receipt to the cashier Windows printer.
     */
    public void printReceipt(Printer printer, Order order, Payment payment, boolean isReprint) {
        if (printer.getStatus() == Printer.PrinterStatus.OFFLINE) {
            throw PosException.badRequest("Printer OFFLINE holatda: " + printer.getName());
        }

        int width = printer.getPaperWidth() <= 58 ? 58 : 80;
        int maxChars = width == 58 ? 32 : 42;

        ZoneId zoneId = ZoneId.of("Asia/Tashkent");
        Instant receiptTime = (payment != null && payment.getPaidAt() != null) ? payment.getPaidAt() : Instant.now();
        String dateStr = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(zoneId).format(receiptTime);
        String timeStr = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(zoneId).format(receiptTime);

        String tableName = order.getTable() != null ? order.getTable().getName() : "Olib ketish";
        String waiterName = (order.getWaiter() != null && order.getWaiter().getFullName() != null)
                ? order.getWaiter().getFullName() : "Noma'lum";
        String cashierName = payment != null && payment.getCashier() != null
                ? payment.getCashier().getFullName() : "Kassir";

        String divider = width == 58 ? "--------------------------------\n" : "------------------------------------------\n";
        String doubleDivider = width == 58 ? "================================\n" : "==========================================\n";

        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(doubleDivider);
        sb.append(centerText("OYBEK RESTAURANT & LOUNGE", maxChars)).append("\n");
        sb.append(centerText("Qarshi sh., Mustaqillik shox ko'chasi", maxChars)).append("\n");
        sb.append(centerText("Tel: +998 90 123 45 67", maxChars)).append("\n");
        sb.append(doubleDivider);

        if (isReprint) {
            sb.append(centerText("*** NUSXA / REPRINT CHEK ***", maxChars)).append("\n");
            sb.append(doubleDivider);
        }

        String checkNum = payment != null && payment.getPaymentNumber() != null
                ? payment.getPaymentNumber() : order.getOrderNumber();

        sb.append(String.format("BUYURTMA RAQAMI: %s\n", order.getOrderNumber()));
        sb.append(String.format("CHEK RAQAMI:     %s\n", checkNum));
        sb.append(String.format("STOL RAQAMI:     %s\n", tableName));
        sb.append(String.format("OFITSIANT:       %s\n", waiterName));
        sb.append(String.format("KASSIR:          %s\n", cashierName));
        sb.append(String.format("SANA:            %s\n", dateStr));
        sb.append(String.format("VAQT:            %s\n", timeStr));
        sb.append(divider);

        if (width == 58) {
            sb.append("MAHSULOT          NARX  DONA   JAMI\n");
            sb.append(divider);
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    if (item.isVoided()) continue;
                    sb.append(truncate(item.getProductName(), 32)).append("\n");
                    sb.append(String.format("  %7.0f x %2.0f = %12.0f\n",
                            item.getUnitPrice(),
                            item.getQuantity(),
                            item.getSubtotal()));
                }
            }
        } else {
            sb.append("MAHSULOT                NARX   DONA     JAMI\n");
            sb.append(divider);
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    if (item.isVoided()) continue;
                    sb.append(truncate(item.getProductName(), 42)).append("\n");
                    sb.append(String.format("   %8.0f x %2.0f = %18.0f\n",
                            item.getUnitPrice(),
                            item.getQuantity(),
                            item.getSubtotal()));
                }
            }
        }

        sb.append(divider);
        sb.append(String.format("JAMI SUMMA:       %12.0f so'm\n", order.getSubtotal()));
        if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("CHEGIRMA:        -%12.0f so'm\n", order.getDiscountAmount()));
        }
        if (order.getTaxAmount() != null && order.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("SOLIQ (QQS):     +%12.0f so'm\n", order.getTaxAmount()));
        }

        sb.append(doubleDivider);
        sb.append(String.format("YAKUNIY SUMMA:    %12.0f so'm\n", order.getTotal()));

        if (payment != null) {
            String payMethod = payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : "NAQD";
            if ("CASH".equalsIgnoreCase(payMethod)) payMethod = "NAQD";
            else if ("CARD".equalsIgnoreCase(payMethod)) payMethod = "KARTA";

            sb.append(String.format("TO'LOV TURI:      %12s\n", payMethod));
            sb.append(String.format("TO'LANGAN SUMMA:  %12.0f so'm\n", payment.getAmount()));
            if (payment.getChangeAmount() != null && payment.getChangeAmount().compareTo(BigDecimal.ZERO) > 0) {
                sb.append(String.format("QAYTIM:           %12.0f so'm\n", payment.getChangeAmount()));
            }
        }

        sb.append(doubleDivider);
        sb.append(centerText("XARIDINGIZ UCHUN RAHMAT!", maxChars)).append("\n");
        sb.append(centerText("YANA KUTIB QOLAMIZ!", maxChars)).append("\n");
        sb.append("\n\n\n\n");

        String textContent = sb.toString();
        byte[] rawBytes = buildEscPosPayload(textContent);

        String winName = printer.getWindowsPrinterName();
        log.info("Sending cashier receipt to Windows printer '{}' for order '{}'", winName, order.getOrderNumber());

        windowsPrintService.print(winName, rawBytes, textContent);
    }

    private byte[] buildEscPosPayload(String text) {
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] escInit = new byte[]{0x1B, 0x40}; // ESC @
        byte[] cut = new byte[]{0x1D, 0x56, 0x41, 0x10}; // GS V A 16

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
