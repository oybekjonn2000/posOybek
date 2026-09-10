package com.restaurantpos.inventory.controller;

import com.restaurantpos.auth.security.UserPrincipal;
import com.restaurantpos.common.response.ApiResponse;
import com.restaurantpos.inventory.dto.InventoryDto;
import com.restaurantpos.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Ombor boshqaruvi API")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_VIEW') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Barcha ombor mahsulotlari ro'yxati")
    public ResponseEntity<ApiResponse<List<InventoryDto.Response>>> getAll(
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAllItems(user.getTenantId())));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Kam qolgan mahsulotlar (min_quantity dan past)")
    public ResponseEntity<ApiResponse<List<InventoryDto.Response>>> getLowStock(
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getLowStockItems(user.getTenantId())));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE') or hasAuthority('MANAGE_PRODUCTS')")
    @Operation(summary = "Yangi ombor mahsuloti qo'shish")
    public ResponseEntity<ApiResponse<InventoryDto.Response>> create(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody InventoryDto.CreateRequest request) {
        InventoryDto.Response response = inventoryService.createItem(user.getTenantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/adjust")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE') or hasAuthority('MANAGE_PRODUCTS')")
    @Operation(summary = "Ombor miqdorini o'zgartirish (qo'shish/ayirish)")
    public ResponseEntity<ApiResponse<InventoryDto.Response>> adjust(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id,
            @RequestBody InventoryDto.AdjustRequest request) {
        InventoryDto.Response response = inventoryService.adjustStock(
                user.getTenantId(), id, request, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/transactions")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Mahsulot harakatlari tarixi")
    public ResponseEntity<ApiResponse<List<InventoryDto.TransactionResponse>>> getTransactions(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getTransactions(id, limit)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE') or hasAuthority('MANAGE_PRODUCTS')")
    @Operation(summary = "Ombor mahsulotini o'chirish (soft delete)")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id) {
        inventoryService.deleteItem(user.getTenantId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Inventory item deleted"));
    }
}
