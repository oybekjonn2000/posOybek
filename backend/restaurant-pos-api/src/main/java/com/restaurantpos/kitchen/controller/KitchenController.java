package com.restaurantpos.kitchen.controller;

import com.restaurantpos.auth.security.UserPrincipal;
import com.restaurantpos.common.response.ApiResponse;
import com.restaurantpos.kitchen.dto.KitchenDto;
import com.restaurantpos.kitchen.service.KitchenService;
import com.restaurantpos.orders.dto.OrderDto;
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
@RequiredArgsConstructor
@Tag(name = "Kitchen", description = "Kitchen Display System (KDS) & Multi-Kitchen API")
public class KitchenController {

    private final KitchenService kitchenService;

    @GetMapping({"/api/kitchens", "/api/kitchen/stations"})
    @Operation(summary = "Get all active kitchen stations (Palovchi, Somsapaz, Bar, Pitsaxona, Asosiy oshxona)")
    public ResponseEntity<ApiResponse<List<KitchenDto.Response>>> getKitchens(
            @AuthenticationPrincipal UserPrincipal user) {
        List<KitchenDto.Response> kitchens = kitchenService.getKitchens(user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(kitchens));
    }

    @PostMapping("/api/kitchens")
    @PreAuthorize("hasAuthority('MANAGE_SETTINGS') or hasAuthority('MANAGE_PRODUCTS')")
    @Operation(summary = "Create new kitchen station")
    public ResponseEntity<ApiResponse<KitchenDto.Response>> createKitchen(
            @Valid @RequestBody KitchenDto.CreateRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        KitchenDto.Response created = kitchenService.createKitchen(user.getTenantId(), request);
        return ResponseEntity.ok(ApiResponse.success(created, "Oshxona muvaffaqiyatli qo'shildi"));
    }

    @GetMapping("/api/kitchen/orders")
    @PreAuthorize("hasAuthority('KITCHEN_VIEW') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Get active kitchen orders, optionally filtered by kitchenId")
    public ResponseEntity<ApiResponse<List<OrderDto.Response>>> getKitchenOrders(
            @RequestParam(required = false) UUID kitchenId,
            @AuthenticationPrincipal UserPrincipal user) {
        List<OrderDto.Response> orders = kitchenService.getActiveKitchenOrders(user.getTenantId(), kitchenId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PutMapping("/api/kitchen/items/{itemId}/status")
    @PreAuthorize("hasAuthority('KITCHEN_UPDATE') or hasAuthority('EDIT_ORDER')")
    @Operation(summary = "Update kitchen item preparation status (NEW, ACCEPTED, COOKING, READY, SERVED)")
    public ResponseEntity<ApiResponse<Void>> updateItemStatus(
            @PathVariable UUID itemId,
            @RequestParam String status) {
        kitchenService.updateItemKitchenStatus(itemId, status);
        return ResponseEntity.ok(ApiResponse.success(null, "Kitchen item status updated"));
    }
}
