package com.restaurantpos.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

public class DeliveryMappingDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductMappingResponse {
        private UUID id;
        private UUID providerId;
        private String providerName;
        private String externalProductId;
        private String externalProductName;
        private UUID posProductId;
        private String posProductName;
        private String posProductCategory;
        private boolean autoMapped;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryMappingResponse {
        private UUID id;
        private UUID providerId;
        private String providerName;
        private String externalCategoryId;
        private String externalCategoryName;
        private UUID posCategoryId;
        private String posCategoryName;
        private Instant createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MapProductRequest {
        @NotNull(message = "Provayder ID kiritilishi shart")
        private UUID providerId;

        @NotBlank(message = "Tashqi mahsulot ID kiritilishi shart")
        private String externalProductId;

        private String externalProductName;

        @NotNull(message = "POS mahsulot ID kiritilishi shart")
        private UUID posProductId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MapCategoryRequest {
        @NotNull(message = "Provayder ID kiritilishi shart")
        private UUID providerId;

        @NotBlank(message = "Tashqi kategoriya ID kiritilishi shart")
        private String externalCategoryId;

        private String externalCategoryName;

        @NotNull(message = "POS kategoriya ID kiritilishi shart")
        private UUID posCategoryId;
    }
}
