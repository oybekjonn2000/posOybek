package com.restaurantpos.inventory.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class InventoryDto {

    @Data
    public static class CreateRequest {
        private String name;
        private String sku;
        private String unit;
        private BigDecimal quantity;
        private BigDecimal minQuantity;
        private BigDecimal costPrice;
        private String category;
        private String notes;
    }

    @Data
    public static class AdjustRequest {
        private BigDecimal quantity;   // positive = qo'shish, negative = ayirish
        private String type;           // PURCHASE, ADJUSTMENT, WASTE, RETURN
        private String notes;
    }

    @Data @Builder
    public static class Response {
        private UUID id;
        private String name;
        private String sku;
        private String unit;
        private BigDecimal quantity;
        private BigDecimal minQuantity;
        private BigDecimal costPrice;
        private String category;
        private boolean lowStock;
        private boolean active;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data @Builder
    public static class TransactionResponse {
        private UUID id;
        private UUID itemId;
        private String itemName;
        private String type;
        private BigDecimal quantity;
        private BigDecimal quantityBefore;
        private BigDecimal quantityAfter;
        private String notes;
        private Instant createdAt;
    }
}
