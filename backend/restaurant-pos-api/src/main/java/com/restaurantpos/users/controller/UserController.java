package com.restaurantpos.users.controller;

import com.restaurantpos.auth.security.UserPrincipal;
import com.restaurantpos.common.response.ApiResponse;
import com.restaurantpos.users.dto.UserDto;
import com.restaurantpos.users.service.UserService;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Employee & User Management API")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('MANAGE_USERS') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Get all employees")
    public ResponseEntity<ApiResponse<List<UserDto.Response>>> getAllUsers(
            @AuthenticationPrincipal UserPrincipal user) {
        List<UserDto.Response> users = userService.getAllUsers(user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserDto.Response>> getUserById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        UserDto.Response userDto = userService.getUserById(id, user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(userDto));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @Operation(summary = "Create new employee")
    public ResponseEntity<ApiResponse<UserDto.Response>> createUser(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody UserDto.CreateRequest request) {
        UserDto.Response created = userService.createUser(user.getTenantId(), request);
        return ResponseEntity.ok(ApiResponse.success(created, "Employee created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @Operation(summary = "Update employee")
    public ResponseEntity<ApiResponse<UserDto.Response>> updateUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody UserDto.UpdateRequest request) {
        UserDto.Response updated = userService.updateUser(id, user.getTenantId(), request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Employee updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @Operation(summary = "Deactivate/Activate employee")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        userService.deactivateUser(id, user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(null, "Employee status toggled"));
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @Operation(summary = "Reset employee password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody UserDto.ChangePasswordRequest request) {
        userService.resetPassword(id, user.getTenantId(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successfully"));
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('MANAGE_USERS') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Get all roles")
    public ResponseEntity<ApiResponse<List<UserDto.RoleResponse>>> getRoles(
            @AuthenticationPrincipal UserPrincipal user) {
        List<UserDto.RoleResponse> roles = userService.getRoles(user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(roles));
    }
}
