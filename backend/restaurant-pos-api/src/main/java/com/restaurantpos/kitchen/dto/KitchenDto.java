package com.restaurantpos.kitchen.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class KitchenDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private String name;
        private String code;
        private String description;
        private int sortOrder;
        private boolean active;
        private String color;
        private boolean autoPrint;
        private boolean soundNotification;
        private Integer preparationTimeMinutes;
        private UUID printerId;
        private String printerName;
        private String printerStatus;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "Oshxona nomi kiritilishi shart")
        private String name;

        @NotBlank(message = "Oshxona kodi kiritilishi shart")
        private String code;

        private String description;
        private int sortOrder = 0;
        private String color;
        private Boolean autoPrint;
        private Boolean soundNotification;
        private Integer preparationTimeMinutes;
        private UUID printerId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String name;
        private String code;
        private String description;
        private Integer sortOrder;
        private Boolean active;
        private String color;
        private Boolean autoPrint;
        private Boolean soundNotification;
        private Integer preparationTimeMinutes;
        private UUID printerId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketResponse {
        private UUID id;
        private UUID orderId;
        private String orderNumber;
        private UUID tableId;
        private String tableNumber;
        private String tableName;
        private String waiterName;
        private UUID kitchenId;
        private String kitchenName;
        private String ticketNumber;
        private String status;
        private String notes;
        private Instant createdAt;
        private List<TicketItemResponse> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketItemResponse {
        private UUID id;
        private UUID productId;
        private String productName;
        private BigDecimal quantity;
        private String kitchenStatus;
        private String notes;
    }
}
