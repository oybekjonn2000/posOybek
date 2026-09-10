package com.restaurantpos.products.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

public class CategoryDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private UUID kitchenId;
        private String kitchenName;
        private String kitchenCode;
        private String name;
        private String nameUz;
        private String nameRu;
        private String nameEn;
        private String description;
        private String icon;
        private String color;
        private String imageUrl;
        private int sortOrder;
        private boolean active;
        private UUID parentId;
        private int productCount;
        private Instant createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotNull(message = "Oshxona tanlanishi kerak")
        private UUID kitchenId;

        @NotBlank(message = "Category name is required")
        private String name;

        private String nameUz;
        private String nameRu;
        private String nameEn;
        private String description;
        private String icon;
        private String color;
        private String imageUrl;
        private int sortOrder = 0;
        private UUID parentId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private UUID kitchenId;
        private String name;
        private String nameUz;
        private String nameRu;
        private String nameEn;
        private String description;
        private String icon;
        private String color;
        private String imageUrl;
        private Integer sortOrder;
        private Boolean active;
        private UUID parentId;
    }
}
