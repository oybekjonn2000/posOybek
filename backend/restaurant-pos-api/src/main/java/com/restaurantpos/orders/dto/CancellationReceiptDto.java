package com.restaurantpos.orders.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class CancellationReceiptDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelItemRequest {
        @NotBlank(message = "Bekor qilish sababi majburiy")
        private String reason;

        @Positive(message = "Miqdor musbat bo'lishi kerak")
        private BigDecimal quantity; // If null or >= item.quantity, cancels entire item
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelOrderRequest {
        @NotBlank(message = "Bekor qilish sababi majburiy")
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private String receiptNumber;
        private UUID orderId;
        private String orderNumber;
        private UUID tableId;
        private String tableName;
        private UUID cancelledById;
        private String cancelledByName;
        private String reason;
        private UUID itemId;
        private String itemName;
        private BigDecimal cancelledQuantity;
        private BigDecimal unitPrice;
        private BigDecimal totalAmount;
        private boolean fullOrder;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancellationResult {
        private OrderDto.Response order;
        private Response receipt;
    }
}
