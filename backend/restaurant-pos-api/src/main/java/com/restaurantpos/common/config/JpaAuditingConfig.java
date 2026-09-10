package com.restaurantpos.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA Auditing configuration — tracks who created/modified entities.
 */
@Configuration
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() ||
                    authentication.getPrincipal() == null) {
                return Optional.empty();
            }
            try {
                if (authentication.getPrincipal() instanceof com.restaurantpos.auth.security.UserPrincipal userPrincipal) {
                    return Optional.of(userPrincipal.getUserId());
                }
            } catch (Exception ignored) {}
            return Optional.empty();
        };
    }
}
