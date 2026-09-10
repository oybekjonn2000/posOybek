package com.restaurantpos.shifts.controller;

import com.restaurantpos.auth.security.UserPrincipal;
import com.restaurantpos.common.response.ApiResponse;
import com.restaurantpos.shifts.dto.ShiftDto;
import com.restaurantpos.shifts.service.ShiftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
@Tag(name = "Shifts", description = "Cashier Shifts & Day Reconciliation API")
public class ShiftController {

    private final ShiftService shiftService;

    @GetMapping("/current")
    @Operation(summary = "Get currently active shift")
    public ResponseEntity<ApiResponse<ShiftDto.Response>> getCurrentShift(
            @AuthenticationPrincipal UserPrincipal user) {
        ShiftDto.Response current = shiftService.getCurrentShift(user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(current));
    }

    @PostMapping("/open")
    @PreAuthorize("hasAuthority('MANAGE_SHIFTS')")
    @Operation(summary = "Open new shift with initial cash amount")
    public ResponseEntity<ApiResponse<ShiftDto.Response>> openShift(
            @Valid @RequestBody ShiftDto.OpenRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        ShiftDto.Response shift = shiftService.openShift(user.getTenantId(), user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(shift, "Shift opened successfully"));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('MANAGE_SHIFTS')")
    @Operation(summary = "Close shift and calculate cash differences")
    public ResponseEntity<ApiResponse<ShiftDto.Response>> closeShift(
            @PathVariable UUID id,
            @Valid @RequestBody ShiftDto.CloseRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        ShiftDto.Response shift = shiftService.closeShift(user.getTenantId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(shift, "Shift closed successfully"));
    }
}
