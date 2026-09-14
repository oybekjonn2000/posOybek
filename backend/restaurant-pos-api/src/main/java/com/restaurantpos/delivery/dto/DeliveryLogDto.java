package com.restaurantpos.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

public class DeliveryLogDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private UUID providerId;
        private String providerName;
        private String action;
        private String externalId;
        private String status;
        private Long requestTimeMs;
        private String errorMessage;
        private String details;
        private Instant createdAt;
    }
}
