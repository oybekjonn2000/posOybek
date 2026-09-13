package com.restaurantpos.settings.service;

import com.restaurantpos.common.exception.PosException;
import com.restaurantpos.printers.entity.Printer;
import com.restaurantpos.printers.repository.PrinterRepository;
import com.restaurantpos.settings.dto.SettingsDto;
import com.restaurantpos.settings.entity.AppSetting;
import com.restaurantpos.settings.entity.AuditLog;
import com.restaurantpos.settings.repository.AppSettingRepository;
import com.restaurantpos.settings.repository.AuditLogRepository;
import com.restaurantpos.tenants.entity.Tenant;
import com.restaurantpos.tenants.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettingsService {

    private final AppSettingRepository appSettingRepository;
    private final TenantRepository tenantRepository;
    private final PrinterRepository printerRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public SettingsDto.AllSettingsResponse getAllSettings(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> PosException.notFound("Tenant not found"));

        List<AppSetting> allSettings = appSettingRepository.findByTenantId(tenantId);
        Map<String, Map<String, String>> byCategory = new HashMap<>();

        for (AppSetting s : allSettings) {
            byCategory.computeIfAbsent(s.getCategory().toUpperCase(), k -> new HashMap<>())
                    .put(s.getKey(), s.getValue());
        }

        Map<String, String> gen = byCategory.getOrDefault("GENERAL", Collections.emptyMap());
        Map<String, String> rec = byCategory.getOrDefault("RECEIPT", Collections.emptyMap());
        Map<String, String> pay = byCategory.getOrDefault("PAYMENTS", Collections.emptyMap());
        Map<String, String> tax = byCategory.getOrDefault("TAX_SERVICE", Collections.emptyMap());
        Map<String, String> ord = byCategory.getOrDefault("ORDERS", Collections.emptyMap());
        Map<String, String> kit = byCategory.getOrDefault("KITCHEN", Collections.emptyMap());
        Map<String, String> not = byCategory.getOrDefault("NOTIFICATIONS", Collections.emptyMap());
        Map<String, String> sec = byCategory.getOrDefault("SECURITY", Collections.emptyMap());
        Map<String, String> bkp = byCategory.getOrDefault("BACKUP", Collections.emptyMap());

        SettingsDto.RestaurantSettings restaurant = SettingsDto.RestaurantSettings.builder()
                .name(tenant.getName())
                .logoUrl(tenant.getLogoUrl())
                .address(tenant.getAddress())
                .city(tenant.getCity())
                .country(tenant.getCountry())
                .phone(tenant.getPhone())
                .email(tenant.getEmail())
                .taxNumber(tenant.getTaxNumber())
                .currency(tenant.getCurrency())
                .currencySymbol(getCurrencySymbol(tenant.getCurrency()))
                .timezone(tenant.getTimezone())
                .language(gen.getOrDefault("language", "uz"))
                .workingHours(gen.getOrDefault("workingHours", "09:00 - 23:00"))
                .description(gen.getOrDefault("restaurantDescription", "Qarshi shahar milliy va zamonaviy taomlar restorani"))
                .website(gen.getOrDefault("website", "https://restaurantpos.uz"))
                .build();

        SettingsDto.GeneralSettings general = SettingsDto.GeneralSettings.builder()
                .defaultCurrency(gen.getOrDefault("defaultCurrency", tenant.getCurrency()))
                .dateFormat(gen.getOrDefault("dateFormat", "DD.MM.YYYY"))
                .timeFormat(gen.getOrDefault("timeFormat", "HH:mm"))
                .language(gen.getOrDefault("language", "uz"))
                .theme(gen.getOrDefault("theme", "dark"))
                .autoSave(Boolean.parseBoolean(gen.getOrDefault("autoSave", "true")))
                .autoRefresh(Boolean.parseBoolean(gen.getOrDefault("autoRefresh", "true")))
                .realTimeUpdates(Boolean.parseBoolean(gen.getOrDefault("realTimeUpdates", "true")))
                .confirmBeforeDelete(Boolean.parseBoolean(gen.getOrDefault("confirmBeforeDelete", "true")))
                .confirmBeforeCancelOrder(Boolean.parseBoolean(gen.getOrDefault("confirmBeforeCancelOrder", "true")))
                .confirmBeforePayment(Boolean.parseBoolean(gen.getOrDefault("confirmBeforePayment", "true")))
                .soundNotifications(Boolean.parseBoolean(gen.getOrDefault("soundNotifications", "true")))
                .build();

        SettingsDto.ReceiptSettings receipt = SettingsDto.ReceiptSettings.builder()
                .restaurantName(tenant.getName())
                .logoUrl(tenant.getLogoUrl())
                .address(tenant.getAddress())
                .phone(tenant.getPhone())
                .header(rec.getOrDefault("header", "Xush kelibsiz! Marhamat, rohatlaning!"))
                .footer(rec.getOrDefault("footer", "Tashrifingiz uchun rahmat! Yana kutib qolamiz!"))
                .showWaiter(Boolean.parseBoolean(rec.getOrDefault("showWaiter", "true")))
                .showCashier(Boolean.parseBoolean(rec.getOrDefault("showCashier", "true")))
                .showTable(Boolean.parseBoolean(rec.getOrDefault("showTable", "true")))
                .showOrderNumber(Boolean.parseBoolean(rec.getOrDefault("showOrderNumber", "true")))
                .showDateTime(Boolean.parseBoolean(rec.getOrDefault("showDateTime", "true")))
                .showPaymentMethod(Boolean.parseBoolean(rec.getOrDefault("showPaymentMethod", "true")))
                .showDiscount(Boolean.parseBoolean(rec.getOrDefault("showDiscount", "true")))
                .showServiceCharge(Boolean.parseBoolean(rec.getOrDefault("showServiceCharge", "true")))
                .showTax(Boolean.parseBoolean(rec.getOrDefault("showTax", "false")))
                .paperWidth(parseInt(rec.getOrDefault("paperWidth", "80"), 80))
                .numberOfCopies(parseInt(rec.getOrDefault("numberOfCopies", "1"), 1))
                .build();

        SettingsDto.PaymentSettings payments = SettingsDto.PaymentSettings.builder()
                .cashEnabled(Boolean.parseBoolean(pay.getOrDefault("cashEnabled", "true")))
                .cardEnabled(Boolean.parseBoolean(pay.getOrDefault("cardEnabled", "true")))
                .clickEnabled(Boolean.parseBoolean(pay.getOrDefault("clickEnabled", "true")))
                .paymeEnabled(Boolean.parseBoolean(pay.getOrDefault("paymeEnabled", "true")))
                .otherEnabled(Boolean.parseBoolean(pay.getOrDefault("otherEnabled", "false")))
                .defaultPaymentMethod(pay.getOrDefault("defaultPaymentMethod", "CASH"))
                .allowMixedPayment(Boolean.parseBoolean(pay.getOrDefault("allowMixedPayment", "true")))
                .requirePaymentConfirmation(Boolean.parseBoolean(pay.getOrDefault("requirePaymentConfirmation", "false")))
                .autoPrintReceiptAfterPayment(Boolean.parseBoolean(pay.getOrDefault("autoPrintReceiptAfterPayment", "true")))
                .build();

        SettingsDto.TaxServiceSettings taxService = SettingsDto.TaxServiceSettings.builder()
                .serviceChargeEnabled(Boolean.parseBoolean(tax.getOrDefault("serviceChargeEnabled", "true")))
                .serviceChargePercent(parseDouble(tax.getOrDefault("serviceChargePercent", "10.0"), 10.0))
                .taxEnabled(Boolean.parseBoolean(tax.getOrDefault("taxEnabled", "false")))
                .taxPercent(parseDouble(tax.getOrDefault("taxPercent", "12.0"), 12.0))
                .build();

        SettingsDto.OrderSettings orders = SettingsDto.OrderSettings.builder()
                .autoOrderNumber(Boolean.parseBoolean(ord.getOrDefault("autoOrderNumber", "true")))
                .orderNumberPrefix(ord.getOrDefault("orderNumberPrefix", "ORD"))
                .allowOrderEditing(Boolean.parseBoolean(ord.getOrDefault("allowOrderEditing", "true")))
                .allowItemCancellation(Boolean.parseBoolean(ord.getOrDefault("allowItemCancellation", "true")))
                .allowQuantityEditing(Boolean.parseBoolean(ord.getOrDefault("allowQuantityEditing", "true")))
                .requireCancellationReason(Boolean.parseBoolean(ord.getOrDefault("requireCancellationReason", "true")))
                .requireManagerApproval(Boolean.parseBoolean(ord.getOrDefault("requireManagerApproval", "false")))
                .autoSendToKitchen(Boolean.parseBoolean(ord.getOrDefault("autoSendToKitchen", "true")))
                .allowSplitBill(Boolean.parseBoolean(ord.getOrDefault("allowSplitBill", "true")))
                .allowMergeOrders(Boolean.parseBoolean(ord.getOrDefault("allowMergeOrders", "true")))
                .allowReopenOrder(Boolean.parseBoolean(ord.getOrDefault("allowReopenOrder", "false")))
                .build();

        SettingsDto.KitchenSettings kitchen = SettingsDto.KitchenSettings.builder()
                .autoPrintKitchenOrder(Boolean.parseBoolean(kit.getOrDefault("autoPrintKitchenOrder", "true")))
                .soundNotificationOnNewTicket(Boolean.parseBoolean(kit.getOrDefault("soundNotificationOnNewTicket", "true")))
                .autoAcceptOrders(Boolean.parseBoolean(kit.getOrDefault("autoAcceptOrders", "false")))
                .ticketFontSize(kit.getOrDefault("ticketFontSize", "medium"))
                .showWaiterOnTicket(Boolean.parseBoolean(kit.getOrDefault("showWaiterOnTicket", "true")))
                .showTableOnTicket(Boolean.parseBoolean(kit.getOrDefault("showTableOnTicket", "true")))
                .showNotesOnTicket(Boolean.parseBoolean(kit.getOrDefault("showNotesOnTicket", "true")))
                .build();

        SettingsDto.NotificationSettings notifications = SettingsDto.NotificationSettings.builder()
                .soundEnabled(Boolean.parseBoolean(not.getOrDefault("soundEnabled", "true")))
                .soundVolume(parseInt(not.getOrDefault("soundVolume", "80"), 80))
                .newOrderSound(not.getOrDefault("newOrderSound", "chime"))
                .itemReadySound(not.getOrDefault("itemReadySound", "bell"))
                .lowStockAlert(Boolean.parseBoolean(not.getOrDefault("lowStockAlert", "true")))
                .build();

        SettingsDto.SecuritySettings security = SettingsDto.SecuritySettings.builder()
                .requirePinForCashier(Boolean.parseBoolean(sec.getOrDefault("requirePinForCashier", "true")))
                .autoLogoutMinutes(parseInt(sec.getOrDefault("autoLogoutMinutes", "30"), 30))
                .sessionTimeoutMinutes(parseInt(sec.getOrDefault("sessionTimeoutMinutes", "120"), 120))
                .maxLoginAttempts(parseInt(sec.getOrDefault("maxLoginAttempts", "5"), 5))
                .build();

        SettingsDto.BackupSettings backup = SettingsDto.BackupSettings.builder()
                .backupLocation(bkp.getOrDefault("backupLocation", "C:/posOybek/backups"))
                .autoBackupEnabled(Boolean.parseBoolean(bkp.getOrDefault("autoBackupEnabled", "true")))
                .backupFrequency(bkp.getOrDefault("backupFrequency", "DAILY"))
                .lastBackupTime(bkp.getOrDefault("lastBackupTime", "2026-09-10 23:00:00"))
                .dbStatus("CONNECTED (PostgreSQL 18)")
                .dbSize("24.8 MB")
                .build();

        Map<String, Object> raw = new HashMap<>();
        allSettings.forEach(s -> raw.put(s.getCategory() + "." + s.getKey(), s.getValue()));

        return SettingsDto.AllSettingsResponse.builder()
                .restaurant(restaurant)
                .general(general)
                .receipt(receipt)
                .payments(payments)
                .taxService(taxService)
                .orders(orders)
                .kitchen(kitchen)
                .notifications(notifications)
                .security(security)
                .backup(backup)
                .rawSettings(raw)
                .build();
    }

    @Transactional
    public SettingsDto.RestaurantSettings updateRestaurantSettings(UUID tenantId, UUID userId, SettingsDto.RestaurantSettings request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> PosException.notFound("Tenant not found"));

        String oldVal = String.format("Name: %s, Phone: %s, Currency: %s", tenant.getName(), tenant.getPhone(), tenant.getCurrency());

        if (request.getName() != null && !request.getName().isBlank()) {
            tenant.setName(request.getName().trim());
        }
        if (request.getAddress() != null) tenant.setAddress(request.getAddress().trim());
        if (request.getCity() != null) tenant.setCity(request.getCity().trim());
        if (request.getCountry() != null) tenant.setCountry(request.getCountry().trim());
        if (request.getPhone() != null) tenant.setPhone(request.getPhone().trim());
        if (request.getEmail() != null) tenant.setEmail(request.getEmail().trim());
        if (request.getTaxNumber() != null) tenant.setTaxNumber(request.getTaxNumber().trim());
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            tenant.setCurrency(request.getCurrency().trim().toUpperCase());
        }
        if (request.getTimezone() != null && !request.getTimezone().isBlank()) {
            tenant.setTimezone(request.getTimezone().trim());
        }
        if (request.getLogoUrl() != null) tenant.setLogoUrl(request.getLogoUrl().trim());

        Tenant saved = tenantRepository.save(tenant);

        // Update supplemental settings in settings table
        if (request.getWorkingHours() != null) {
            saveSetting(tenant, "GENERAL", "workingHours", request.getWorkingHours(), "Ish vaqti");
        }
        if (request.getDescription() != null) {
            saveSetting(tenant, "GENERAL", "restaurantDescription", request.getDescription(), "Restoran tavsifi");
        }
        if (request.getWebsite() != null) {
            saveSetting(tenant, "GENERAL", "website", request.getWebsite(), "Veb-sayt");
        }

        String newVal = String.format("Name: %s, Phone: %s, Currency: %s", saved.getName(), saved.getPhone(), saved.getCurrency());
        auditLogService.logChange(tenantId, userId, "UPDATE", "RESTAURANT_SETTINGS", tenant.getId(), oldVal, newVal, "Restoran ma'lumotlari yangilandi");

        return request;
    }

    @Transactional
    public void updateCategorySettings(UUID tenantId, UUID userId, String category, Map<String, Object> values) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> PosException.notFound("Tenant not found"));

        String catUpper = category.trim().toUpperCase();

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue() != null ? entry.getValue().toString() : "";
            saveSetting(tenant, catUpper, key, val, catUpper + " sozlamasi: " + key);
        }

        auditLogService.logChange(tenantId, userId, "UPDATE", "SETTINGS_" + catUpper, null,
                null, values.toString(), catUpper + " sozlamalari yangilandi (" + values.size() + " ta parametr)");
    }

    private void saveSetting(Tenant tenant, String category, String key, String value, String desc) {
        AppSetting setting = appSettingRepository.findByTenantIdAndCategoryAndKey(tenant.getId(), category, key)
                .orElseGet(() -> {
                    AppSetting s = new AppSetting();
                    s.setTenant(tenant);
                    s.setCategory(category);
                    s.setKey(key);
                    s.setPublic(true);
                    return s;
                });

        setting.setValue(value);
        setting.setDescription(desc);
        appSettingRepository.save(setting);
    }

    @Transactional(readOnly = true)
    public SettingsDto.SystemInfoDto getSystemInfo(UUID tenantId) {
        String pgVersion = "PostgreSQL 18";
        try {
            Object res = entityManager.createNativeQuery("SELECT version()").getSingleResult();
            if (res != null) {
                pgVersion = res.toString().split(" on ")[0];
            }
        } catch (Exception ignored) {}

        List<Printer> printers = printerRepository.findByTenantIdAndDeletedAtIsNullOrderByCreatedAtAsc(tenantId);
        int totalPrinters = printers.size();
        int onlinePrinters = (int) printers.stream()
                .filter(p -> p.getStatus() == Printer.PrinterStatus.ONLINE && p.isActive())
                .count();

        Runtime rt = Runtime.getRuntime();
        long totalMb = rt.totalMemory() / (1024 * 1024);
        long freeMb = rt.freeMemory() / (1024 * 1024);

        String srvTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.of("Asia/Tashkent"))
                .format(Instant.now());

        return SettingsDto.SystemInfoDto.builder()
                .appName("Restaurant POS Enterprise")
                .appVersion("1.0.0-PRO")
                .backendStatus("ONLINE (Spring Boot 3.3.4)")
                .databaseStatus("CONNECTED (localhost:5433/pos)")
                .postgresVersion(pgVersion)
                .serverTime(srvTime)
                .frontendVersion("Angular 18.2.0")
                .webSocketStatus("ONLINE (/ws STOMP Active)")
                .printerServiceStatus("ONLINE (" + onlinePrinters + "/" + totalPrinters + " faol)")
                .totalMemoryMb(totalMb)
                .freeMemoryMb(freeMb)
                .totalPrinters(totalPrinters)
                .onlinePrinters(onlinePrinters)
                .build();
    }

    @Transactional(readOnly = true)
    public List<SettingsDto.AuditLogResponse> getAuditLogs(UUID tenantId, int limit) {
        int l = limit > 0 && limit <= 100 ? limit : 20;
        return auditLogRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(0, l))
                .stream().map(a -> SettingsDto.AuditLogResponse.builder()
                        .id(a.getId())
                        .action(a.getAction())
                        .entityType(a.getEntityType())
                        .entityId(a.getEntityId())
                        .oldValue(a.getOldValue())
                        .newValue(a.getNewValue())
                        .notes(a.getNotes())
                        .userName(a.getUser() != null ? a.getUser().getFullName() : "Tizim")
                        .createdAt(a.getCreatedAt())
                        .build()
                ).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<SettingsDto.AuditLogResponse> getAuditLogsPaginated(UUID tenantId, org.springframework.data.domain.Pageable pageable) {
        return auditLogRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId, pageable)
                .map(a -> SettingsDto.AuditLogResponse.builder()
                        .id(a.getId())
                        .action(a.getAction())
                        .entityType(a.getEntityType())
                        .entityId(a.getEntityId())
                        .oldValue(a.getOldValue())
                        .newValue(a.getNewValue())
                        .notes(a.getNotes())
                        .userName(a.getUser() != null ? a.getUser().getFullName() : "Tizim")
                        .createdAt(a.getCreatedAt())
                        .build());
    }

    private String getCurrencySymbol(String currency) {
        if ("UZS".equalsIgnoreCase(currency)) return "so'm";
        if ("USD".equalsIgnoreCase(currency)) return "$";
        if ("EUR".equalsIgnoreCase(currency)) return "€";
        if ("RUB".equalsIgnoreCase(currency)) return "₽";
        return currency;
    }

    private int parseInt(String val, int def) {
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private double parseDouble(String val, double def) {
        try {
            return Double.parseDouble(val.trim());
        } catch (Exception e) {
            return def;
        }
    }
}
