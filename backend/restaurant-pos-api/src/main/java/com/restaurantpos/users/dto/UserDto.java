package com.restaurantpos.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class UserDto {

    @Getter
    @Setter
    public static class CreateRequest {
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        private String username;

        @NotBlank(message = "Password is required")
        @Size(min = 4, message = "Password must be at least 4 characters")
        private String password;

        @NotBlank(message = "First name is required")
        private String firstName;

        private String lastName;
        private String email;
        private String phone;
        private String pin;
        private String role;
        private UUID roleId;
        private List<UUID> kitchenIds;
    }

    @Getter
    @Setter
    public static class UpdateRequest {
        @NotBlank(message = "First name is required")
        private String firstName;

        private String lastName;
        private String email;
        private String phone;
        private String pin;
        private String role;
        private UUID roleId;
        private Boolean active;
        private List<UUID> kitchenIds;
    }

    @Getter
    @Setter
    public static class ChangePasswordRequest {
        @NotBlank(message = "New password is required")
        @Size(min = 4, message = "Password must be at least 4 characters")
        private String newPassword;
    }

    @Getter
    @Setter
    @Builder
    public static class Response {
        private UUID id;
        private String username;
        private String firstName;
        private String lastName;
        private String fullName;
        private String email;
        private String phone;
        private boolean active;
        private String role;
        private UUID roleId;
        private List<String> permissions;
        private List<UUID> kitchenIds;
        private List<KitchenSummary> kitchens;
        private Instant lastLoginAt;
        private Instant createdAt;
    }

    @Getter
    @Setter
    @Builder
    public static class RoleResponse {
        private UUID id;
        private String name;
        private String description;
    }

    @Getter
    @Setter
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    @Builder
    public static class KitchenSummary {
        private UUID id;
        private String name;
        private String code;
    }
}
