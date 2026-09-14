package com.restaurantpos.delivery.controller;

import com.restaurantpos.auth.security.UserPrincipal;
import com.restaurantpos.common.config.PaginationUtils;
import com.restaurantpos.common.response.ApiResponse;
import com.restaurantpos.delivery.dto.DeliveryOrderDto;
import com.restaurantpos.delivery.service.DeliveryOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/delivery/orders")
@RequiredArgsConstructor
@Tag(name = "Delivery Orders", description = "Delivery Orders Management API")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
public class DeliveryOrderController {

    private final DeliveryOrderService orderService;

    @GetMapping
    @Operation(summary = "Get delivery orders with filtering and pagination")
    public ResponseEntity<ApiResponse<List<DeliveryOrderDto.Response>>> getOrders(
            @RequestParam(required = false) UUID providerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentType,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "25") Integer size,
            @AuthenticationPrincipal UserPrincipal user) {

        Pageable pageable = PaginationUtils.safePageable(page, size);
        Page<DeliveryOrderDto.Response> result = orderService.getFilteredOrders(
                user.getTenantId(), providerId, status, paymentType, search, pageable);

        return ResponseEntity.ok(ApiResponse.success(result.getContent(), ApiResponse.PageMeta.of(result)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get delivery order details by ID")
    public ResponseEntity<ApiResponse<DeliveryOrderDto.Response>> getOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrder(user.getTenantId(), id)));
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "Accept delivery order and dispatch to kitchen")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<ApiResponse<DeliveryOrderDto.Response>> acceptOrder(
            @PathVariable UUID id,
            @RequestBody(required = false) DeliveryOrderDto.AcceptRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        Integer prep = request != null ? request.getPreparationMinutes() : 20;
        return ResponseEntity.ok(ApiResponse.success(
                orderService.acceptOrder(user.getTenantId(), id, prep), "Buyurtma qabul qilindi va oshxonaga yuborildi"));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject delivery order")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<ApiResponse<DeliveryOrderDto.Response>> rejectOrder(
            @PathVariable UUID id,
            @RequestBody(required = false) DeliveryOrderDto.RejectRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        String reason = request != null && request.getReason() != null ? request.getReason() : "Restoran band";
        return ResponseEntity.ok(ApiResponse.success(
                orderService.rejectOrder(user.getTenantId(), id, reason), "Buyurtma rad etildi"));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel delivery order")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<ApiResponse<DeliveryOrderDto.Response>> cancelOrder(
            @PathVariable UUID id,
            @RequestBody(required = false) DeliveryOrderDto.CancelRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        String reason = request != null && request.getReason() != null ? request.getReason() : "Bekor qilindi";
        return ResponseEntity.ok(ApiResponse.success(
                orderService.cancelOrder(user.getTenantId(), id, reason), "Buyurtma bekor qilindi"));
    }

    @PostMapping("/{id}/map-item")
    @Operation(summary = "Manually map unmapped delivery item to POS product and process order")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<DeliveryOrderDto.Response>> mapItem(
            @PathVariable UUID id,
            @RequestBody DeliveryOrderDto.ManualMapItemRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.manualMapItemAndReprocess(user.getTenantId(), id, request.getExternalProductId(), request.getPosProductId()),
                "Mahsulot muvaffaqiyatli bog'landi"));
    }
}
