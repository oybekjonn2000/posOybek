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
    @Operation(summary = "Get kitchen stations with filtering, search, and optional pagination")
    public ResponseEntity<ApiResponse<List<KitchenDto.Response>>> getKitchens(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        List<KitchenDto.Response> kitchens = kitchenService.getKitchensFiltered(user.getTenantId(), search, status);
        if (user != null && user.isKitchen() && user.getKitchenIds() != null && !user.getKitchenIds().isEmpty()) {
            kitchens = kitchens.stream()
                    .filter(k -> user.getKitchenIds().contains(k.getId()))
                    .toList();
        }
        if (page != null) {
            org.springframework.data.domain.Pageable pageable = com.restaurantpos.common.config.PaginationUtils.safePageable(page, size);
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), kitchens.size());
            List<KitchenDto.Response> content = (start <= end && start < kitchens.size())
                    ? kitchens.subList(start, end)
                    : java.util.Collections.emptyList();
            org.springframework.data.domain.Page<KitchenDto.Response> pageResult =
                    new org.springframework.data.domain.PageImpl<>(content, pageable, kitchens.size());
            return ResponseEntity.ok(ApiResponse.success(pageResult.getContent(), ApiResponse.PageMeta.of(pageResult)));
        }
        return ResponseEntity.ok(ApiResponse.success(kitchens));
    }

    @GetMapping("/api/kitchens/active")
    @Operation(summary = "Get active kitchen stations")
    public ResponseEntity<ApiResponse<List<KitchenDto.Response>>> getActiveKitchens(
            @AuthenticationPrincipal UserPrincipal user) {
        List<KitchenDto.Response> active = kitchenService.getActiveKitchens(user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(active));
    }

    @GetMapping("/api/kitchens/{id}")
    @Operation(summary = "Get single kitchen station by ID")
    public ResponseEntity<ApiResponse<KitchenDto.Response>> getKitchen(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        if (user != null && user.isKitchen() && !user.hasKitchenAccess(id)) {
            throw com.restaurantpos.common.exception.PosException.forbidden("Sizda boshqa oshxona ma'lumotlarini ko'rish huquqi yo'q!");
        }
        KitchenDto.Response kitchen = kitchenService.getKitchenById(user.getTenantId(), id);
        return ResponseEntity.ok(ApiResponse.success(kitchen));
    }

    @PostMapping("/api/kitchens")
    @PreAuthorize("hasAuthority('MANAGE_SETTINGS') or hasAuthority('MANAGE_PRODUCTS') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Create new kitchen station")
    public ResponseEntity<ApiResponse<KitchenDto.Response>> createKitchen(
            @Valid @RequestBody KitchenDto.CreateRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        KitchenDto.Response created = kitchenService.createKitchen(user.getTenantId(), request);
        return ResponseEntity.ok(ApiResponse.success(created, "Oshxona muvaffaqiyatli qo'shildi"));
    }

    @PutMapping("/api/kitchens/{id}")
    @PreAuthorize("hasAuthority('MANAGE_SETTINGS') or hasAuthority('MANAGE_PRODUCTS') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Update kitchen station configuration and printer")
    public ResponseEntity<ApiResponse<KitchenDto.Response>> updateKitchen(
            @PathVariable UUID id,
            @Valid @RequestBody KitchenDto.UpdateRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        KitchenDto.Response updated = kitchenService.updateKitchen(user.getTenantId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Oshxona muvaffaqiyatli yangilandi"));
    }

    @PatchMapping("/api/kitchens/{id}/status")
    @PreAuthorize("hasAuthority('MANAGE_SETTINGS') or hasAuthority('MANAGE_PRODUCTS') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Activate or deactivate kitchen station")
    public ResponseEntity<ApiResponse<KitchenDto.Response>> toggleKitchenStatus(
            @PathVariable UUID id,
            @RequestParam(required = false) Boolean active,
            @RequestBody(required = false) java.util.Map<String, Boolean> body,
            @AuthenticationPrincipal UserPrincipal user) {
        boolean targetActive;
        if (active != null) {
            targetActive = active;
        } else if (body != null && body.containsKey("active")) {
            targetActive = Boolean.TRUE.equals(body.get("active"));
        } else {
            throw com.restaurantpos.common.exception.PosException.badRequest("Active status is required");
        }
        KitchenDto.Response updated = kitchenService.toggleKitchenStatus(user.getTenantId(), id, targetActive);
        return ResponseEntity.ok(ApiResponse.success(updated, "Oshxona holati yangilandi"));
    }

    @GetMapping("/api/kitchens/{id}/employees")
    @PreAuthorize("hasAuthority('MANAGE_SETTINGS') or hasAuthority('MANAGE_USERS') or hasAuthority('MANAGE_PRODUCTS') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Get employees assigned to kitchen station")
    public ResponseEntity<ApiResponse<List<com.restaurantpos.users.dto.UserDto.Response>>> getKitchenEmployees(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        List<com.restaurantpos.users.dto.UserDto.Response> employees = kitchenService.getKitchenEmployees(user.getTenantId(), id);
        return ResponseEntity.ok(ApiResponse.success(employees));
    }

    @PostMapping({"/api/kitchens/{id}/employees", "/api/kitchens/{id}/assign-employees"})
    @PreAuthorize("hasAuthority('MANAGE_SETTINGS') or hasAuthority('MANAGE_USERS') or hasAuthority('MANAGE_PRODUCTS') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Assign employees to kitchen station")
    public ResponseEntity<ApiResponse<Void>> assignKitchenEmployees(
            @PathVariable UUID id,
            @RequestBody KitchenDto.AssignEmployeesRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        kitchenService.assignKitchenEmployees(user.getTenantId(), id, request != null ? request.getEmployeeIds() : null);
        return ResponseEntity.ok(ApiResponse.success(null, "Xodimlar oshxonaga muvaffaqiyatli biriktirildi"));
    }

    @GetMapping("/api/kitchens/{id}/categories")
    @Operation(summary = "Get categories assigned to kitchen station")
    public ResponseEntity<ApiResponse<List<com.restaurantpos.products.dto.CategoryDto.Response>>> getKitchenCategories(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        List<com.restaurantpos.products.dto.CategoryDto.Response> categories = kitchenService.getKitchenCategories(user.getTenantId(), id);
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @PostMapping("/api/kitchens/{id}/categories")
    @PreAuthorize("hasAuthority('MANAGE_SETTINGS') or hasAuthority('MANAGE_PRODUCTS') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Assign multiple categories to a kitchen station (1 Category → 1 Kitchen rule enforced)")
    public ResponseEntity<ApiResponse<KitchenDto.AssignCategoriesResponse>> assignKitchenCategories(
            @PathVariable UUID id,
            @RequestBody KitchenDto.AssignCategoriesRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        KitchenDto.AssignCategoriesResponse result = kitchenService.assignCategoriesToKitchen(user.getTenantId(), id, request);
        String msg = result.getMessage();
        return ResponseEntity.ok(ApiResponse.success(result, msg));
    }

    @DeleteMapping("/api/kitchens/{id}")
    @PreAuthorize("hasAuthority('MANAGE_SETTINGS') or hasAuthority('MANAGE_PRODUCTS') or hasRole('ADMIN') or hasRole('MANAGER')")
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
        if (user != null && user.isKitchen()) {
            java.util.Set<UUID> userKitchenIds = user.getKitchenIds();
            if (userKitchenIds == null || userKitchenIds.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success(List.of()));
            }
            if (kitchenId != null) {
                if (!userKitchenIds.contains(kitchenId)) {
                    throw com.restaurantpos.common.exception.PosException.forbidden("Sizda boshqa oshxona ma'lumotlarini ko'rish huquqi yo'q!");
                }
                List<OrderDto.Response> orders = kitchenService.getActiveKitchenOrders(user.getTenantId(), kitchenId);
                return ResponseEntity.ok(ApiResponse.success(orders));
            } else {
                List<OrderDto.Response> orders = kitchenService.getActiveKitchenOrdersForKitchens(user.getTenantId(), userKitchenIds);
                return ResponseEntity.ok(ApiResponse.success(orders));
            }
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
        if (user != null && user.isKitchen()) {
            OrderDto.Response order = kitchenService.getKitchenOrderByIdForKitchens(id, user.getTenantId(), user.getKitchenIds());
            return ResponseEntity.ok(ApiResponse.success(order));
        }
        OrderDto.Response order = kitchenService.getKitchenOrderById(id, user.getTenantId(), null);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/api/kitchen/tickets/{id}")
    @PreAuthorize("hasAuthority('KITCHEN_VIEW') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Get single kitchen ticket by ID with station authorization guard")
    public ResponseEntity<ApiResponse<com.restaurantpos.kitchen.entity.KitchenTicket>> getKitchenTicket(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        com.restaurantpos.kitchen.entity.KitchenTicket ticket = kitchenService.getKitchenTicketById(id, user.getTenantId(), null);
        if (user != null && user.isKitchen() && ticket.getKitchen() != null) {
            if (!user.hasKitchenAccess(ticket.getKitchen().getId())) {
                throw com.restaurantpos.common.exception.PosException.forbidden("Sizda boshqa oshxona ma'lumotlarini ko'rish huquqi yo'q!");
            }
        }
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
        java.util.Set<UUID> allowedKitchenIds = (user != null && user.isKitchen()) ? user.getKitchenIds() : null;
        kitchenService.updateKitchenOrderStatusForKitchens(id, user.getTenantId(), targetStatus, allowedKitchenIds);
        return ResponseEntity.ok(ApiResponse.success(null, "Kitchen order status updated"));
    }

    @PutMapping("/api/kitchen/items/{itemId}/status")
    @PreAuthorize("hasAuthority('KITCHEN_UPDATE')")
    @Operation(summary = "Update kitchen item preparation status (NEW, ACCEPTED, COOKING, READY, SERVED)")
    public ResponseEntity<ApiResponse<Void>> updateItemStatus(
            @PathVariable UUID itemId,
            @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal user) {
        java.util.Set<UUID> allowedKitchenIds = (user != null && user.isKitchen()) ? user.getKitchenIds() : null;
        kitchenService.updateItemKitchenStatusForKitchens(itemId, status, allowedKitchenIds);
        return ResponseEntity.ok(ApiResponse.success(null, "Kitchen item status updated"));
    }
}
