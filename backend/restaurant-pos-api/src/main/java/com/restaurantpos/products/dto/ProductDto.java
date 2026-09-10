package com.restaurantpos.products.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ProductDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private UUID categoryId;
        private String categoryName;
        private UUID kitchenId;
        private String kitchenName;
        private String sku;
        private String barcode;
        private String name;
        private String nameUz;
        private String nameRu;
        private String nameEn;
        private String description;
        private String imageUrl;
        private String unit;
        private BigDecimal purchasePrice;
        private BigDecimal salePrice;
        private BigDecimal taxRate;
        private boolean taxable;
        private boolean active;
        private boolean available;
        private boolean trackStock;
        private BigDecimal minStockLevel;
        private BigDecimal currentStock;
        private int sortOrder;
        private int version;
        private List<ModifierGroupResponse> modifierGroups;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModifierGroupResponse {
        private UUID id;
        private String name;
        private String description;
        private boolean required;
        private int minSelections;
        private int maxSelections;
        private List<ModifierResponse> modifiers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModifierResponse {
        private UUID id;
        private String name;
        private BigDecimal price;
        private int sortOrder;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private UUID categoryId;
        private UUID kitchenId;
        private String sku;
        private String barcode;
        @NotBlank(message = "Product name is required")
        private String name;
        private String nameUz;
        private String nameRu;
        private String nameEn;
        private String description;
        private String imageUrl;
        private String unit = "piece";
        private BigDecimal purchasePrice = BigDecimal.ZERO;
        @NotNull(message = "Sale price is required")
        private BigDecimal salePrice;
        private BigDecimal taxRate = BigDecimal.ZERO;
        private boolean taxable = false;
        private boolean active = true;
        private boolean available = true;
        private boolean trackStock = false;
        private BigDecimal minStockLevel = BigDecimal.ZERO;
        private int sortOrder = 0;
        private List<UUID> modifierGroupIds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private UUID categoryId;
        private UUID kitchenId;
        private String sku;
        private String barcode;
        private String name;
        private String nameUz;
        private String nameRu;
        private String nameEn;
        private String description;
        private String imageUrl;
        private String unit;
        private BigDecimal purchasePrice;
        private BigDecimal salePrice;
        private BigDecimal taxRate;
        private Boolean taxable;
        private Boolean active;
        private Boolean available;
        private Boolean trackStock;
        private BigDecimal minStockLevel;
        private Integer sortOrder;
        private List<UUID> modifierGroupIds;
    }
}
