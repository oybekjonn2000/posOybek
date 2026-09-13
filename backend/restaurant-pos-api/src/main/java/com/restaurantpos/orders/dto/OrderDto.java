package com.restaurantpos.orders.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class OrderDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private String orderNumber;
        private String orderType;
        private String status;
        private UUID tableId;
        private String tableNumber;
        private String tableName;
        private UUID customerId;
        private String customerName;
        private UUID waiterId;
        private String waiterName;
        private int guestCount;
        private BigDecimal subtotal;
        private BigDecimal discountAmount;
        private BigDecimal discountPercent;
        private BigDecimal taxAmount;
        private BigDecimal total;
        private String notes;
        private String kitchenNotes;
        private Instant openedAt;
        private Instant sentToKitchenAt;
        private Instant readyAt;
        private Instant paidAt;
        private Instant closedAt;
        private UUID cashierId;
        private String cashierName;
        private String paymentMethod;
        private BigDecimal paidAmount;
        private BigDecimal changeAmount;
        private List<ItemResponse> items;
        private int version;
        private String receiptPrintStatus;
        private Instant receiptPrintedAt;
        private String receiptPrintError;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemResponse {
        private UUID id;
        private UUID productId;
        private UUID kitchenId;
        private String kitchenName;
        private String productName;
        private String productSku;
        private String imageUrl;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal discountAmount;
        private BigDecimal subtotal;
        private String notes;
        private String kitchenStatus;
        private BigDecimal sentQuantity;
        private BigDecimal deliveredQuantity;
        private BigDecimal cancelledQuantity;
        private BigDecimal remainingToSend;
        private boolean voided;
        private String voidReason;
        private java.time.Instant voidedAt;
        private String voidedByName;
        private List<ModifierResponse> modifiers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModifierResponse {
        private UUID id;
        private UUID modifierId;
        private String modifierName;
        private BigDecimal price;
        private int quantity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private UUID tableId;
        private UUID customerId;
        private String orderType = "DINE_IN"; // DINE_IN, TAKEAWAY, DELIVERY
        private int guestCount = 1;
        private String notes;
        private String kitchenNotes;
        private List<@Valid ItemRequest> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemRequest {
        @NotNull(message = "Product ID is required")
        private UUID productId;

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.001", message = "Quantity must be greater than 0")
        @DecimalMax(value = "9999", message = "Quantity exceeds maximum allowed limit")
        private BigDecimal quantity = BigDecimal.ONE;

        private String notes;
        private List<@Valid ModifierRequest> modifiers;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModifierRequest {
        @NotNull(message = "Modifier ID is required")
        private UUID modifierId;

        @Min(value = 1, message = "Modifier quantity must be at least 1")
        @Max(value = 100, message = "Modifier quantity exceeds limit")
        private int quantity = 1;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddItemsRequest {
        @NotEmpty(message = "Items list cannot be empty")
        private List<@Valid ItemRequest> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendToKitchenRequest {
        private List<ItemRequest> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoidItemRequest {
        @NotNull(message = "Void reason is required")
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplyDiscountRequest {
        @DecimalMin(value = "0.0", message = "Discount percent must be non-negative")
        @DecimalMax(value = "100.0", message = "Discount percent cannot exceed 100")
        private BigDecimal percent;

        @DecimalMin(value = "0.0", message = "Discount amount must be non-negative")
        private BigDecimal amount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateStatusRequest {
        @NotNull(message = "Status is required")
        private String status;
    }
}
