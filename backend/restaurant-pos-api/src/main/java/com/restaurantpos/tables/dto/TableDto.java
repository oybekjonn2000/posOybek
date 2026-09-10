package com.restaurantpos.tables.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

public class TableDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZoneResponse {
        private UUID id;
        private String name;
        private String description;
        private int sortOrder;
        private boolean active;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private UUID zoneId;
        private String zoneName;
        private String tableNumber;
        private String name;
        private int capacity;
        private String shape;
        private int posX;
        private int posY;
        private int width;
        private int height;
        private String status;
        private UUID currentOrderId;
        private String activeOrderNumber;
        private Integer itemCount;
        private BigDecimal totalAmount;
        private boolean active;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateZoneRequest {
        @NotBlank(message = "Zona nomi kiritilishi shart (masalan: Zal, Ko'cha, Ayvon, Podval)")
        private String name;
        private String description;
        private int sortOrder = 0;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private UUID zoneId;
        private String zoneName;
        @NotBlank(message = "Table number is required")
        private String tableNumber;
        private String name;
        private int capacity = 4;
        private String shape = "rectangle";
        private int posX = 0;
        private int posY = 0;
        private int width = 100;
        private int height = 80;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateLayoutRequest {
        private int posX;
        private int posY;
        private int width;
        private int height;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateStatusRequest {
        @NotNull(message = "Status is required")
        private String status;
        private UUID currentOrderId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private UUID zoneId;
        private String tableNumber;
        private String name;
        private int capacity = 4;
        private String shape = "rectangle";
        private Boolean active;
    }
}
