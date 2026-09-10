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
        if (user != null && user.getKitchenId() != null) {
            kitchens = kitchens.stream()
                    .filter(k -> k.getId().equals(user.getKitchenId()))
                    .toList();
        }
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

    @PutMapping("/api/kitchens/{id}")
    @PreAuthorize("hasAuthority('MANAGE_SETTINGS') or hasAuthority('MANAGE_PRODUCTS')")
    @Operation(summary = "Update kitchen station configuration and printer")
    public ResponseEntity<ApiResponse<KitchenDto.Response>> updateKitchen(
            @PathVariable UUID id,
            @RequestBody KitchenDto.UpdateRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        KitchenDto.Response updated = kitchenService.updateKitchen(user.getTenantId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Oshxona muvaffaqiyatli yangilandi"));
    }

    @DeleteMapping("/api/kitchens/{id}")
    @PreAuthorize("hasAuthority('MANAGE_SETTINGS') or hasAuthority('MANAGE_PRODUCTS')")
    @Operation(summary = "Delete kitchen station with category and product count guard")
    public ResponseEntity<ApiResponse<Void>> deleteKitchen(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        kitchenService.deleteKitchen(user.getTenantId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Oshxona muvaffaqiyatli o'chirildi"));
    }

    @GetMapping("/api/kitchen/orders")
    @PreAuthorize("hasAuthority('KITCHEN_VIEW') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Get active kitchen orders, optionally filtered by kitchenId")
    public ResponseEntity<ApiResponse<List<OrderDto.Response>>> getKitchenOrders(
            @RequestParam(required = false) UUID kitchenId,
            @AuthenticationPrincipal UserPrincipal user) {
        if (user != null && user.getKitchenId() != null) {
            if (kitchenId != null && !kitchenId.equals(user.getKitchenId())) {
                throw com.restaurantpos.common.exception.PosException.forbidden("Sizda boshqa oshxona ma'lumotlarini ko'rish huquqi yo'q!");
            }
            kitchenId = user.getKitchenId();
        }
        List<OrderDto.Response> orders = kitchenService.getActiveKitchenOrders(user.getTenantId(), kitchenId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/api/kitchen/orders/{id}")
    @PreAuthorize("hasAuthority('KITCHEN_VIEW') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Get single kitchen order by ID, filtered for user kitchen station")
    public ResponseEntity<ApiResponse<OrderDto.Response>> getKitchenOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        OrderDto.Response order = kitchenService.getKitchenOrderById(id, user.getTenantId(), user != null ? user.getKitchenId() : null);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/api/kitchen/tickets/{id}")
    @PreAuthorize("hasAuthority('KITCHEN_VIEW') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Get single kitchen ticket by ID with station authorization guard")
    public ResponseEntity<ApiResponse<com.restaurantpos.kitchen.entity.KitchenTicket>> getKitchenTicket(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        com.restaurantpos.kitchen.entity.KitchenTicket ticket = kitchenService.getKitchenTicketById(id, user.getTenantId(), user != null ? user.getKitchenId() : null);
        return ResponseEntity.ok(ApiResponse.success(ticket));
    }

    @PutMapping("/api/kitchen/orders/{id}/status")
    @PreAuthorize("hasAuthority('KITCHEN_UPDATE')")
    @Operation(summary = "Update all items in order for user's kitchen station")
    public ResponseEntity<ApiResponse<Void>> updateKitchenOrderStatus(
            @PathVariable UUID id,
            @RequestParam(required = false) String status,
            @RequestBody(required = false) java.util.Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal user) {
        String targetStatus = status;
        if ((targetStatus == null || targetStatus.isBlank()) && body != null) {
            targetStatus = body.get("status");
        }
        if (targetStatus == null || targetStatus.isBlank()) {
            throw com.restaurantpos.common.exception.PosException.badRequest("Status is required");
        }
        kitchenService.updateKitchenOrderStatus(id, user.getTenantId(), targetStatus, user != null ? user.getKitchenId() : null);
        return ResponseEntity.ok(ApiResponse.success(null, "Kitchen order status updated"));
    }

    @PutMapping("/api/kitchen/items/{itemId}/status")
    @PreAuthorize("hasAuthority('KITCHEN_UPDATE')")
    @Operation(summary = "Update kitchen item preparation status (NEW, ACCEPTED, COOKING, READY, SERVED)")
    public ResponseEntity<ApiResponse<Void>> updateItemStatus(
            @PathVariable UUID itemId,
            @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal user) {
        kitchenService.updateItemKitchenStatus(itemId, status, user != null ? user.getKitchenId() : null);
        return ResponseEntity.ok(ApiResponse.success(null, "Kitchen item status updated"));
    }
}
