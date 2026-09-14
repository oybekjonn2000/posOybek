package com.restaurantpos.delivery.controller;

import com.restaurantpos.auth.security.UserPrincipal;
import com.restaurantpos.common.config.PaginationUtils;
import com.restaurantpos.common.response.ApiResponse;
import com.restaurantpos.delivery.dto.DeliveryDashboardDto;
import com.restaurantpos.delivery.dto.DeliveryLogDto;
import com.restaurantpos.delivery.entity.DeliveryIntegrationLogEntity;
import com.restaurantpos.delivery.repository.DeliveryIntegrationLogRepository;
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
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
@Tag(name = "Delivery Analytics & Logs", description = "Delivery Dashboard and Integration Logs API")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class DeliveryDashboardController {

    private final DeliveryOrderService orderService;
    private final DeliveryIntegrationLogRepository logRepository;

    @GetMapping("/dashboard")
    @Operation(summary = "Get delivery analytics and summary metrics")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<DeliveryDashboardDto.Summary>> getDashboard(
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getDashboardSummary(user.getTenantId())));
    }

    @GetMapping("/logs")
    @Operation(summary = "Get delivery integration logs with pagination")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<DeliveryLogDto.Response>>> getLogs(
            @RequestParam(required = false) UUID providerId,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "25") Integer size,
            @AuthenticationPrincipal UserPrincipal user) {
        Pageable pageable = PaginationUtils.safePageable(page, size);
        Page<DeliveryIntegrationLogEntity> logPage = providerId != null
                ? logRepository.findByTenantIdAndProviderIdOrderByCreatedAtDesc(user.getTenantId(), providerId, pageable)
                : logRepository.findByTenantIdOrderByCreatedAtDesc(user.getTenantId(), pageable);

        List<DeliveryLogDto.Response> responses = logPage.getContent().stream()
                .map(l -> DeliveryLogDto.Response.builder()
                        .id(l.getId())
                        .providerId(l.getProvider() != null ? l.getProvider().getId() : null)
                        .providerName(l.getProvider() != null ? l.getProvider().getName() : "Umumiy")
                        .action(l.getAction())
                        .externalId(l.getExternalId())
                        .status(l.getStatus().name())
                        .requestTimeMs(l.getRequestTimeMs())
                        .errorMessage(l.getErrorMessage())
                        .details(l.getDetails())
                        .createdAt(l.getCreatedAt())
                        .build())
                .toList();

        return ResponseEntity.ok(ApiResponse.success(responses, ApiResponse.PageMeta.of(logPage)));
    }
}
