package com.restaurantpos.auth.service;

import com.restaurantpos.auth.dto.AuthDto;
import com.restaurantpos.auth.security.JwtTokenProvider;
import com.restaurantpos.auth.security.UserPrincipal;
import com.restaurantpos.common.exception.PosException;
import com.restaurantpos.users.entity.User;
import com.restaurantpos.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Authentication service handling login, token refresh, and PIN authentication.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthDto.TokenResponse login(AuthDto.LoginRequest request) {
        User user = userRepository.findByUsernameAndDeletedAtIsNull(request.getUsername())
                .orElseThrow(() -> PosException.unauthorized("Invalid credentials"));

        if (!user.isActive()) {
            throw PosException.unauthorized("Account is inactive");
        }

        if (user.isLocked()) {
            throw PosException.unauthorized("Account is temporarily locked. Try again later.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(), request.getPassword()));

            user.resetFailedAttempts();
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            return buildTokenResponse(principal);

        } catch (Exception e) {
            user.incrementFailedAttempts();
            userRepository.save(user);
            log.warn("Failed login attempt for user: {}", request.getUsername());
            throw PosException.unauthorized("Invalid credentials");
        }
    }

    @Transactional
    public AuthDto.TokenResponse refreshToken(AuthDto.RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        if (!jwtTokenProvider.validateToken(token)) {
            throw PosException.unauthorized("Invalid or expired refresh token");
        }

        var userId = jwtTokenProvider.getUserIdFromToken(token);
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> PosException.unauthorized("User not found"));

        if (!user.isActive()) {
            throw PosException.unauthorized("Account is inactive");
        }

        var permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(p -> p.getCode())
                .collect(Collectors.toSet());

        UserPrincipal principal = UserPrincipal.builder()
                .userId(user.getId())
                .tenantId(user.getTenant().getId())
                .kitchenId(user.getKitchen() != null ? user.getKitchen().getId() : null)
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .permissions(permissions)
                .active(user.isActive())
                .build();

        return buildTokenResponse(principal);
    }

    private AuthDto.TokenResponse buildTokenResponse(UserPrincipal principal) {
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                principal.getUserId(), principal.getTenantId());

        AuthDto.UserInfo userInfo = new AuthDto.UserInfo(
                principal.getUserId().toString(),
                principal.getUsername(),
                principal.getFullName(),
                principal.getTenantId().toString(),
                principal.getRole(),
                principal.getKitchenId() != null ? principal.getKitchenId().toString() : null,
                principal.getPermissions()
        );

        return new AuthDto.TokenResponse(accessToken, refreshToken, 900, userInfo);
    }
}
