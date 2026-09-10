package com.restaurantpos.payments.controller;

import com.restaurantpos.auth.security.UserPrincipal;
import com.restaurantpos.common.response.ApiResponse;
import com.restaurantpos.payments.dto.PaymentDto;
import com.restaurantpos.payments.service.PaymentService;
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
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payments Processing & Cash Management API")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAuthority('PROCESS_PAYMENT')")
    @Operation(summary = "Process order payment (cash, card, mixed)")
    public ResponseEntity<ApiResponse<PaymentDto.Response>> processPayment(
            @Valid @RequestBody PaymentDto.ProcessRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        PaymentDto.Response payment = paymentService.processPayment(user.getTenantId(), user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(payment, "Payment completed successfully"));
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAuthority('REFUND')")
    @Operation(summary = "Process payment refund")
    public ResponseEntity<ApiResponse<PaymentDto.Response>> refundPayment(
            @PathVariable UUID id,
            @Valid @RequestBody PaymentDto.RefundRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        PaymentDto.Response refund = paymentService.refundPayment(id, user.getTenantId(), user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(refund, "Refund processed successfully"));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get all payments for an order")
    public ResponseEntity<ApiResponse<List<PaymentDto.Response>>> getPaymentsByOrder(
            @PathVariable UUID orderId) {
        List<PaymentDto.Response> payments = paymentService.getPaymentsByOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }
}
