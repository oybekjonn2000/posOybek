package com.restaurantpos.settings.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class SettingsDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestaurantSettings {
        private String name;
        private String logoUrl;
        private String address;
        private String city;
        private String country;
        private String phone;
        private String email;
        private String website;
        private String taxNumber; // INN / STIR
        private String currency;
        private String currencySymbol;
        private String timezone;
        private String language;
        private String workingHours;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneralSettings {
        private String defaultCurrency;
        private String dateFormat;
        private String timeFormat;
        private String language;
        private String theme;
        private boolean autoSave;
        private boolean autoRefresh;
        private boolean realTimeUpdates;
        private boolean confirmBeforeDelete;
        private boolean confirmBeforeCancelOrder;
        private boolean confirmBeforePayment;
        private boolean soundNotifications;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceiptSettings {
        private String restaurantName;
        private String logoUrl;
        private String address;
        private String phone;
        private String header;
        private String footer;
        private boolean showWaiter;
        private boolean showCashier;
        private boolean showTable;
        private boolean showOrderNumber;
        private boolean showDateTime;
        private boolean showPaymentMethod;
        private boolean showDiscount;
        private boolean showServiceCharge;
        private boolean showTax;
        private int paperWidth; // 58 or 80 mm
        private int numberOfCopies;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentSettings {
        private boolean cashEnabled;
        private boolean cardEnabled;
        private boolean clickEnabled;
        private boolean paymeEnabled;
        private boolean otherEnabled;
        private String defaultPaymentMethod;
        private boolean allowMixedPayment;
        private boolean requirePaymentConfirmation;
        private boolean autoPrintReceiptAfterPayment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxServiceSettings {
        private boolean serviceChargeEnabled;
        private double serviceChargePercent;
        private boolean taxEnabled;
        private double taxPercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderSettings {
        private boolean autoOrderNumber;
        private String orderNumberPrefix;
        private boolean allowOrderEditing;
        private boolean allowItemCancellation;
        private boolean allowQuantityEditing;
        private boolean requireCancellationReason;
        private boolean requireManagerApproval;
        private boolean autoSendToKitchen;
        private boolean allowSplitBill;
        private boolean allowMergeOrders;
        private boolean allowReopenOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KitchenSettings {
        private boolean autoPrintKitchenOrder;
        private boolean soundNotificationOnNewTicket;
        private boolean autoAcceptOrders;
        private String ticketFontSize;
        private boolean showWaiterOnTicket;
        private boolean showTableOnTicket;
        private boolean showNotesOnTicket;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationSettings {
        private boolean soundEnabled;
        private int soundVolume;
        private String newOrderSound;
        private String itemReadySound;
        private boolean lowStockAlert;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecuritySettings {
        private boolean requirePinForCashier;
        private int autoLogoutMinutes;
        private int sessionTimeoutMinutes;
        private int maxLoginAttempts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BackupSettings {
        private String backupLocation;
        private boolean autoBackupEnabled;
        private String backupFrequency;
        private String lastBackupTime;
        private String dbStatus;
        private String dbSize;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemInfoDto {
        private String appName;
        private String appVersion;
        private String backendStatus;
        private String databaseStatus;
        private String postgresVersion;
        private String serverTime;
        private String frontendVersion;
        private String webSocketStatus;
        private String printerServiceStatus;
        private long totalMemoryMb;
        private long freeMemoryMb;
        private int totalPrinters;
        private int onlinePrinters;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllSettingsResponse {
        private RestaurantSettings restaurant;
        private GeneralSettings general;
        private ReceiptSettings receipt;
        private PaymentSettings payments;
        private TaxServiceSettings taxService;
        private OrderSettings orders;
        private KitchenSettings kitchen;
        private NotificationSettings notifications;
        private SecuritySettings security;
        private BackupSettings backup;
        private Map<String, Object> rawSettings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditLogResponse {
        private UUID id;
        private String action;
        private String entityType;
        private UUID entityId;
        private String oldValue;
        private String newValue;
        private String notes;
        private String userName;
        private Instant createdAt;
    }
}
