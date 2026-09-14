package com.restaurantpos.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class DeliveryProviderDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private String name;
        private String code;
        private String providerType;
        private String status; // CONNECTED, DISCONNECTED, ERROR, DISABLED
        private String apiBaseUrl;
        private boolean hasApiKey;
        private boolean hasSecret;
        private String maskedApiKey;
        private String restaurantId;
        private String webhookUrl;
        private boolean hasWebhookSecret;
        private boolean autoAccept;
        private boolean autoPrintKitchen;
        private boolean autoPrintReceipt;
        private boolean soundNotification;
        private boolean autoSync;
        private int syncIntervalSeconds;
        private String commissionType;
        private BigDecimal commissionValue;
        private String defaultOrderSource;
        private String defaultPaymentType;
        private Instant lastSyncAt;
        private Instant lastConnectionTestAt;
        private String lastConnectionStatus;
        private String lastConnectionError;
        private long totalOrders;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "Nomi kiritilishi shart")
        private String name;

        @NotBlank(message = "Kodi kiritilishi shart (masalan, YANDEX_EATS)")
        private String code;

        @NotBlank(message = "Provayder turi kiritilishi shart (YANDEX, UZUM, GLOVO, CUSTOM)")
        private String providerType;

        private String apiBaseUrl;
        private String apiKey;
        private String clientId;
        private String secret;
        private String restaurantId;
        private String webhookUrl;
        private String webhookSecret;

        private Boolean autoAccept = false;
        private Boolean autoPrintKitchen = true;
        private Boolean autoPrintReceipt = false;
        private Boolean soundNotification = true;
        private Boolean autoSync = true;
        private Integer syncIntervalSeconds = 60;

        private String commissionType = "PERCENTAGE";
        private BigDecimal commissionValue = BigDecimal.ZERO;
        private String defaultPaymentType = "ONLINE";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String name;
        private String apiBaseUrl;
        private String apiKey;
        private String clientId;
        private String secret;
        private String restaurantId;
        private String webhookUrl;
        private String webhookSecret;

        private Boolean autoAccept;
        private Boolean autoPrintKitchen;
        private Boolean autoPrintReceipt;
        private Boolean soundNotification;
        private Boolean autoSync;
        private Integer syncIntervalSeconds;

        private String commissionType;
        private BigDecimal commissionValue;
        private String defaultPaymentType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestConnectionResponse {
        private boolean success;
        private String message;
        private String errorCode;
        private long responseTimeMs;
    }
}
