package com.restaurantpos.auth.security;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Spring Security UserDetails implementation.
 * Carries authenticated user's identity and permissions.
 */
@Getter
@Builder
public class UserPrincipal implements UserDetails {

    private final UUID userId;
    private final UUID tenantId;
    private final UUID kitchenId;
    private final Set<UUID> kitchenIds;
    private final UUID deviceId;
    private final String username;
    private final String password;
    private final String firstName;
    private final String lastName;
    private final String role;
    private final Set<String> permissions;
    private final boolean active;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> auths = permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
        if (role != null && !role.isBlank()) {
            auths.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return auths;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return active;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    public String getFullName() {
        return firstName + (lastName != null ? " " + lastName : "");
    }

    public boolean isWaiter() {
        return "WAITER".equalsIgnoreCase(role);
    }

    public boolean isKitchen() {
        return "KITCHEN".equalsIgnoreCase(role);
    }

    public boolean isAdminOrManager() {
        return "ADMIN".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role);
    }

    public boolean hasKitchenAccess(UUID kId) {
        if (isAdminOrManager()) return true;
        if (kId == null) return false;
        if (kitchenIds != null && kitchenIds.contains(kId)) return true;
        return kitchenId != null && kitchenId.equals(kId);
    }
}

