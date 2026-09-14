package com.restaurantpos.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DeliveryOrderDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private UUID providerId;
        private String providerName;
        private String providerCode;
        private String externalOrderId;
        private UUID posOrderId;
        private String posOrderNumber;
        private String status;
        private String customerName;
        private String customerPhone;
        private String deliveryAddress;
        private String addressApartment;
        private String addressEntrance;
        private String addressFloor;
        private String addressComment;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private BigDecimal subtotal;
        private BigDecimal deliveryFee;
        private BigDecimal commission;
        private BigDecimal discount;
        private BigDecimal serviceFee;
        private BigDecimal total;
        private String paymentType;
        private String paymentStatus;
        private String courierName;
        private String courierPhone;
        private String courierVehicle;
        private String courierStatus;
        private String errorMessage;
        private int retryCount;
        private Instant createdAt;

        @Builder.Default
        private List<ItemResponse> items = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemResponse {
        private UUID id;
        private String externalProductId;
        private UUID posProductId;
        private String posProductName;
        private String name;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal total;
        private String mappingStatus;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AcceptRequest {
        private Integer preparationMinutes = 20;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RejectRequest {
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelRequest {
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManualMapItemRequest {
        private String externalProductId;
        private UUID posProductId;
    }
}
