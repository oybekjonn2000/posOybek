package com.restaurantpos.orders.controller;

import com.restaurantpos.auth.security.UserPrincipal;
import com.restaurantpos.common.response.ApiResponse;
import com.restaurantpos.orders.dto.OrderDto;
import com.restaurantpos.orders.service.OrderService;
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
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "POS Orders Lifecycle API")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "Get active orders")
    public ResponseEntity<ApiResponse<List<OrderDto.Response>>> getActiveOrders(
            @AuthenticationPrincipal UserPrincipal user) {
        List<OrderDto.Response> orders = orderService.getActiveOrders(user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<OrderDto.Response>> getOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        OrderDto.Response order = orderService.getOrderById(id, user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_ORDER')")
    @Operation(summary = "Create new order")
    public ResponseEntity<ApiResponse<OrderDto.Response>> createOrder(
            @Valid @RequestBody OrderDto.CreateRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        OrderDto.Response order = orderService.createOrder(user.getTenantId(), user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(order, "Order created successfully"));
    }

    @PostMapping("/{id}/items")
    @PreAuthorize("hasAuthority('EDIT_ORDER')")
    @Operation(summary = "Add items to existing order")
    public ResponseEntity<ApiResponse<OrderDto.Response>> addItems(
            @PathVariable UUID id,
            @Valid @RequestBody OrderDto.AddItemsRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        OrderDto.Response order = orderService.addItemsToOrder(id, user.getTenantId(), request);
        return ResponseEntity.ok(ApiResponse.success(order, "Items added to order"));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasAuthority('EDIT_ORDER')")
    @Operation(summary = "Void an item from order with reason")
    public ResponseEntity<ApiResponse<OrderDto.Response>> voidItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @Valid @RequestBody OrderDto.VoidItemRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        OrderDto.Response order = orderService.voidOrderItem(id, itemId, user.getUserId(), user.getTenantId(), request);
        return ResponseEntity.ok(ApiResponse.success(order, "Item voided"));
    }

    @PutMapping("/{id}/discount")
    @PreAuthorize("hasAuthority('APPLY_DISCOUNT')")
    @Operation(summary = "Apply discount to order")
    public ResponseEntity<ApiResponse<OrderDto.Response>> applyDiscount(
            @PathVariable UUID id,
            @RequestBody OrderDto.ApplyDiscountRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        OrderDto.Response order = orderService.applyDiscount(id, user.getTenantId(), request);
        return ResponseEntity.ok(ApiResponse.success(order, "Discount applied"));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update order status")
    public ResponseEntity<ApiResponse<OrderDto.Response>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody OrderDto.UpdateStatusRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        OrderDto.Response order = orderService.updateOrderStatus(id, user.getTenantId(), request);
        return ResponseEntity.ok(ApiResponse.success(order, "Order status updated"));
    }

    @PostMapping("/{id}/items/{itemId}/cancel")
    @PreAuthorize("hasAnyAuthority('EDIT_ORDER', 'DELETE_ORDER', 'ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_WAITER')")
    @Operation(summary = "Cancel an order item (full or partial quantity) with mandatory reason")
    public ResponseEntity<ApiResponse<com.restaurantpos.orders.dto.CancellationReceiptDto.CancellationResult>> cancelItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @Valid @RequestBody com.restaurantpos.orders.dto.CancellationReceiptDto.CancelItemRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        com.restaurantpos.orders.dto.CancellationReceiptDto.CancellationResult result =
                orderService.cancelOrderItem(id, itemId, user.getUserId(), user.getTenantId(), request);
        return ResponseEntity.ok(ApiResponse.success(result, "Mahsulot bekor qilindi"));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('DELETE_ORDER', 'ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_WAITER')")
    @Operation(summary = "Cancel full order with mandatory reason")
    public ResponseEntity<ApiResponse<com.restaurantpos.orders.dto.CancellationReceiptDto.CancellationResult>> cancelOrder(
            @PathVariable UUID id,
            @Valid @RequestBody com.restaurantpos.orders.dto.CancellationReceiptDto.CancelOrderRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        com.restaurantpos.orders.dto.CancellationReceiptDto.CancellationResult result =
                orderService.cancelOrder(id, user.getUserId(), user.getTenantId(), request);
        return ResponseEntity.ok(ApiResponse.success(result, "Buyurtma to'liq bekor qilindi"));
    }

    @GetMapping("/{id}/cancellation-receipts")
    @Operation(summary = "Get cancellation receipts for order")
    public ResponseEntity<ApiResponse<List<com.restaurantpos.orders.dto.CancellationReceiptDto.Response>>> getCancellationReceipts(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        List<com.restaurantpos.orders.dto.CancellationReceiptDto.Response> receipts =
                orderService.getCancellationReceipts(id, user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(receipts));
    }
}
