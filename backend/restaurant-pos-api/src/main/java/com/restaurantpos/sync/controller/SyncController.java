package com.restaurantpos.sync.controller;

import com.restaurantpos.auth.security.UserPrincipal;
import com.restaurantpos.common.response.ApiResponse;
import com.restaurantpos.sync.dto.SyncDto;
import com.restaurantpos.sync.service.SyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
@Tag(name = "Sync", description = "Offline-first Sync Engine Status API")
public class SyncController {

    private final SyncService syncService;

    @GetMapping("/status")
    @Operation(summary = "Get offline sync status and queue metrics")
    public ResponseEntity<ApiResponse<SyncDto.StatusResponse>> getStatus(
            @AuthenticationPrincipal UserPrincipal user) {
        SyncDto.StatusResponse status = syncService.getStatus(user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @PostMapping("/trigger")
    @Operation(summary = "Manually trigger sync to cloud")
    public ResponseEntity<ApiResponse<Integer>> triggerSync(
            @AuthenticationPrincipal UserPrincipal user) {
        int count = syncService.triggerManualSync(user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(count, "Sync completed. " + count + " events processed."));
    }
}
