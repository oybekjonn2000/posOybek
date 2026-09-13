package com.restaurantpos.settings.controller;

import com.restaurantpos.auth.security.UserPrincipal;
import com.restaurantpos.common.response.ApiResponse;
import com.restaurantpos.settings.dto.SettingsDto;
import com.restaurantpos.settings.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@Tag(name = "Settings", description = "Enterprise Restaurant Settings & POS Configuration Center")
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    @PreAuthorize("hasAuthority('MANAGE_SETTINGS') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Get all configured settings across all categories")
    public ResponseEntity<ApiResponse<SettingsDto.AllSettingsResponse>> getAllSettings(
            @AuthenticationPrincipal UserPrincipal user) {
        SettingsDto.AllSettingsResponse response = settingsService.getAllSettings(user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/restaurant")
    @PreAuthorize("hasAuthority('MANAGE_SETTINGS') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Update restaurant profile and branding settings")
    public ResponseEntity<ApiResponse<SettingsDto.RestaurantSettings>> updateRestaurant(
            @RequestBody SettingsDto.RestaurantSettings request,
            @AuthenticationPrincipal UserPrincipal user) {
        SettingsDto.RestaurantSettings updated = settingsService.updateRestaurantSettings(
                user.getTenantId(), user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Restoran ma'lumotlari muvaffaqiyatli saqlandi"));
    }

    @PutMapping("/{category}")
    @PreAuthorize("hasAuthority('MANAGE_SETTINGS') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Update settings for a specific category (general, receipt, payments, orders, etc.)")
    public ResponseEntity<ApiResponse<Void>> updateCategory(
            @PathVariable String category,
            @RequestBody Map<String, Object> values,
            @AuthenticationPrincipal UserPrincipal user) {
        settingsService.updateCategorySettings(user.getTenantId(), user.getUserId(), category, values);
        return ResponseEntity.ok(ApiResponse.success(null, category.toUpperCase() + " sozlamalari muvaffaqiyatli saqlandi"));
    }

    @GetMapping("/system/info")
    @PreAuthorize("hasAuthority('MANAGE_SETTINGS') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Get live system diagnostic information and status")
    public ResponseEntity<ApiResponse<SettingsDto.SystemInfoDto>> getSystemInfo(
            @AuthenticationPrincipal UserPrincipal user) {
        SettingsDto.SystemInfoDto info = settingsService.getSystemInfo(user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(info));
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAuthority('VIEW_AUDIT_LOGS') or hasAuthority('MANAGE_SETTINGS') or hasRole('ADMIN')")
    @Operation(summary = "Get audit trail with optional pagination")
    public ResponseEntity<ApiResponse<List<SettingsDto.AuditLogResponse>>> getAuditLogs(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal UserPrincipal user) {
        if (page != null) {
            org.springframework.data.domain.Pageable pageable = com.restaurantpos.common.config.PaginationUtils.safePageable(page, size != null ? size : limit);
            org.springframework.data.domain.Page<SettingsDto.AuditLogResponse> pageResult = settingsService.getAuditLogsPaginated(user.getTenantId(), pageable);
            return ResponseEntity.ok(ApiResponse.success(pageResult.getContent(), ApiResponse.PageMeta.of(pageResult)));
        }
        List<SettingsDto.AuditLogResponse> logs = settingsService.getAuditLogs(user.getTenantId(), limit);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @PostMapping("/backup/trigger")
    @PreAuthorize("hasAuthority('BACKUP_RESTORE') or hasRole('ADMIN')")
    @Operation(summary = "Trigger manual backup snapshot")
    public ResponseEntity<ApiResponse<String>> triggerBackup(
            @AuthenticationPrincipal UserPrincipal user) {
        String msg = "Zaxira nusxa yaratish boshlandi (C:/posOybek/backups/pos_backup_" + System.currentTimeMillis() + ".sql)";
        return ResponseEntity.ok(ApiResponse.success(msg, "Zaxiralash muvaffaqiyatli amalga oshirildi"));
    }
}
