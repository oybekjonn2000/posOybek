package com.restaurantpos.printers.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

public class PrinterDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private String name;
        private String model;
        private String connectionType;
        private String ipAddress;
        private Integer port;
        private String windowsPrinterName;
        private int paperWidth;
        private String characterEncoding;
        private String purpose;
        private String status;
        private boolean active;
        private boolean isDefault;
        private boolean autoPrint;
        private UUID fallbackPrinterId;
        private String fallbackPrinterName;
        private UUID assignedKitchenId;
        private String assignedKitchenName;
        private boolean isPrimaryForKitchen;
        private Instant lastCheckedAt;
        private Instant lastSuccessfulPrintAt;
        private String lastError;
        private Instant createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "Printer nomi kiritilishi shart")
        private String name;

        private String model;

        @NotBlank(message = "Ulanish turi kiritilishi shart (WINDOWS, USB, NETWORK, TCP/IP)")
        private String connectionType;

        private String ipAddress;
        private Integer port = 9100;
        private String windowsPrinterName;
        private Integer paperWidth = 80;
        private String characterEncoding = "UTF-8";

        @NotBlank(message = "Printer maqsadi kiritilishi shart (KITCHEN, CASHIER, RECEIPT, BAR, OTHER)")
        private String purpose;

        private Boolean autoPrint = true;
        private Boolean isDefault = false;
        private Boolean isPrimary = true;
        private UUID fallbackPrinterId;
        private UUID kitchenId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String name;
        private String model;
        private String connectionType;
        private String ipAddress;
        private Integer port;
        private String windowsPrinterName;
        private Integer paperWidth;
        private String characterEncoding;
        private String purpose;
        private String status;
        private Boolean active;
        private Boolean autoPrint;
        private Boolean isDefault;
        private Boolean isPrimary;
        private UUID fallbackPrinterId;
        private UUID kitchenId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestPrintResult {
        private boolean success;
        private String message;
        private String printerName;
        private String connectionType;
        private String target; // IP:Port or Windows Name
        private String errorDetails;
        private Instant testedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentResponse {
        private UUID id;
        private UUID printerId;
        private String printerName;
        private UUID kitchenId;
        private String kitchenName;
        private String purpose;
        private boolean isPrimary;
        private boolean active;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentRequest {
        private UUID printerId;
        private UUID kitchenId;
        private String purpose = "KITCHEN";
        private Boolean isPrimary = true;
    }
}
