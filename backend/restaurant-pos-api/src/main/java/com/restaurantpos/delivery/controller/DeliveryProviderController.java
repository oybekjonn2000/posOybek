package com.restaurantpos.delivery.controller;

import com.restaurantpos.auth.security.UserPrincipal;
import com.restaurantpos.common.response.ApiResponse;
import com.restaurantpos.delivery.dto.DeliveryProviderDto;
import com.restaurantpos.delivery.service.DeliveryProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/delivery/providers")
@RequiredArgsConstructor
@Tag(name = "Delivery Providers", description = "Delivery Services Management API")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class DeliveryProviderController {

    private final DeliveryProviderService providerService;

    @GetMapping
    @Operation(summary = "Get all configured delivery providers")
    public ResponseEntity<ApiResponse<List<DeliveryProviderDto.Response>>> getProviders(
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success(providerService.getProviders(user.getTenantId())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get delivery provider by ID")
    public ResponseEntity<ApiResponse<DeliveryProviderDto.Response>> getProvider(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success(providerService.getProvider(user.getTenantId(), id)));
    }

    @PostMapping
    @Operation(summary = "Create a new delivery provider configuration")
    public ResponseEntity<ApiResponse<DeliveryProviderDto.Response>> createProvider(
            @Valid @RequestBody DeliveryProviderDto.CreateRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success(
                providerService.createProvider(user.getTenantId(), request), "Yangi delivery xizmati muvaffaqiyatli qo'shildi"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update delivery provider configuration")
    public ResponseEntity<ApiResponse<DeliveryProviderDto.Response>> updateProvider(
            @PathVariable UUID id,
            @RequestBody DeliveryProviderDto.UpdateRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success(
                providerService.updateProvider(user.getTenantId(), id, request), "Sozlamalar saqlandi"));
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "Test connection to delivery service")
    public ResponseEntity<ApiResponse<DeliveryProviderDto.TestConnectionResponse>> testConnection(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success(
                providerService.testConnection(user.getTenantId(), id)));
    }

    @PostMapping("/{id}/connect")
    @Operation(summary = "Connect / Activate provider")
    public ResponseEntity<ApiResponse<DeliveryProviderDto.Response>> connectProvider(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success(
                providerService.connectProvider(user.getTenantId(), id), "Xizmat muvaffaqiyatli ulandi"));
    }

    @PostMapping("/{id}/disconnect")
    @Operation(summary = "Disconnect / Deactivate provider")
    public ResponseEntity<ApiResponse<DeliveryProviderDto.Response>> disconnectProvider(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success(
                providerService.disconnectProvider(user.getTenantId(), id), "Xizmat o'chirildi"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete / Disable provider")
    public ResponseEntity<ApiResponse<Void>> deleteProvider(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        providerService.deleteProvider(user.getTenantId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xizmat o'chirildi"));
    }
}
