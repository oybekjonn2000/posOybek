package com.restaurantpos.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class AuthDto {

    @Getter
    @Setter
    public static class LoginRequest {
        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Password is required")
        @Size(min = 4, max = 100)
        private String password;

        private String deviceId;
    }

    @Getter
    @Setter
    public static class PinLoginRequest {
        @NotBlank(message = "PIN is required")
        @Size(min = 4, max = 8)
        private String pin;

        private String deviceId;
    }

    @Getter
    @Setter
    public static class RefreshTokenRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    @Getter
    @Setter
    public static class ChangePasswordRequest {
        @NotBlank
        private String currentPassword;

        @NotBlank
        @Size(min = 6, max = 100)
        private String newPassword;
    }

    @Getter
    public static class TokenResponse {
        private final String accessToken;
        private final String refreshToken;
        private final String tokenType = "Bearer";
        private final long expiresIn;
        private final UserInfo user;

        public TokenResponse(String accessToken, String refreshToken, long expiresIn, UserInfo user) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
            this.user = user;
        }
    }

    @Getter
    public static class UserInfo {
        private final String id;
        private final String username;
        private final String fullName;
        private final String tenantId;
        private final String role;
        private final String kitchenId;
        private final java.util.List<String> kitchenIds;
        private final java.util.Set<String> permissions;

        public UserInfo(String id, String username, String fullName, String tenantId,
                        String role, String kitchenId, java.util.Set<String> permissions) {
            this(id, username, fullName, tenantId, role, kitchenId, 
                 kitchenId != null ? java.util.List.of(kitchenId) : java.util.List.of(), permissions);
        }

        public UserInfo(String id, String username, String fullName, String tenantId,
                        String role, String kitchenId, java.util.List<String> kitchenIds, java.util.Set<String> permissions) {
            this.id = id;
            this.username = username;
            this.fullName = fullName;
            this.tenantId = tenantId;
            this.role = role;
            this.kitchenId = kitchenId;
            this.kitchenIds = kitchenIds != null ? kitchenIds : java.util.List.of();
            this.permissions = permissions;
        }
    }
}
