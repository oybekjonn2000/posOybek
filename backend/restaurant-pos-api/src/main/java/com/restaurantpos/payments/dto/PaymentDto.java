package com.restaurantpos.payments.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private UUID orderId;
        private String paymentNumber;
        private String paymentMethod;
        private String status;
        private BigDecimal amount;
        private BigDecimal cashAmount;
        private BigDecimal cardAmount;
        private BigDecimal changeAmount;
        private boolean refund;
        private String referenceNumber;
        private String notes;
        private Instant paidAt;
        private UUID cashierId;
        private String cashierName;
        private String receiptPrintStatus;
        private String receiptPrintError;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessRequest {
        @NotNull(message = "Order ID is required")
        private UUID orderId;

        @NotNull(message = "Payment method is required")
        private String paymentMethod; // CASH, CARD, OTHER, MIXED

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "To'lov summasi 0 dan katta bo'lishi kerak")
        private BigDecimal amount;

        @DecimalMin(value = "0.0", message = "Naqd summa manfiy bo'lishi mumkin emas")
        private BigDecimal cashAmount = BigDecimal.ZERO;

        @DecimalMin(value = "0.0", message = "Karta summasi manfiy bo'lishi mumkin emas")
        private BigDecimal cardAmount = BigDecimal.ZERO;

        @DecimalMin(value = "0.0", message = "Qaytim summasi manfiy bo'lishi mumkin emas")
        private BigDecimal changeAmount = BigDecimal.ZERO;

        private String referenceNumber;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundRequest {
        @NotNull(message = "Refund reason is required")
        private String reason;
    }
}
