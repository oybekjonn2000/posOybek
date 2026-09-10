package com.restaurantpos.payments.dto;

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
        private BigDecimal amount;

        private BigDecimal cashAmount = BigDecimal.ZERO;
        private BigDecimal cardAmount = BigDecimal.ZERO;
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
