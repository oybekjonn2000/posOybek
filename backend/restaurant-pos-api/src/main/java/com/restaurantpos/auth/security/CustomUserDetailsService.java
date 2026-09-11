package com.restaurantpos.auth.security;

import com.restaurantpos.users.entity.User;
import com.restaurantpos.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Custom UserDetailsService loading user with their permissions.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return buildPrincipal(user);
    }

    @Transactional(readOnly = true)
    public UserPrincipal loadUserById(UUID userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));
        return buildPrincipal(user);
    }

    private UserPrincipal buildPrincipal(User user) {
        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getCode())
                .collect(Collectors.toSet());

        String role = user.getRoles().isEmpty() ? "STAFF" : user.getRoles().iterator().next().getName();

        Set<UUID> kitchenIds = user.getKitchens().stream()
                .map(com.restaurantpos.kitchen.entity.Kitchen::getId)
                .collect(Collectors.toSet());
        UUID primaryKitchenId = kitchenIds.isEmpty() ? null : kitchenIds.iterator().next();

        return UserPrincipal.builder()
                .userId(user.getId())
                .tenantId(user.getTenant().getId())
                .kitchenId(primaryKitchenId)
                .kitchenIds(kitchenIds)
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(role)
                .permissions(permissions)
                .active(user.isActive())
                .build();
    }
}
