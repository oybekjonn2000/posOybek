package com.restaurantpos.shifts.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ShiftDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private String shiftNumber;
        private String status;
        private UUID cashierId;
        private String cashierName;
        private BigDecimal openingCash;
        private BigDecimal closingCashExpected;
        private BigDecimal closingCashActual;
        private BigDecimal cashDifference;
        private BigDecimal totalSales;
        private BigDecimal totalCashSales;
        private BigDecimal totalCardSales;
        private BigDecimal totalRefunds;
        private BigDecimal totalDiscounts;
        private int ordersCount;
        private Instant openedAt;
        private Instant closedAt;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpenRequest {
        @NotNull(message = "Opening cash is required")
        private BigDecimal openingCash;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CloseRequest {
        @NotNull(message = "Actual cash count is required")
        private BigDecimal closingCashActual;
        private String notes;
    }
}
