package com.restaurantpos.users.entity;

import com.restaurantpos.common.entity.BaseEntity;
import com.restaurantpos.tenants.entity.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * System user entity — supports all staff roles (admin, cashier, waiter, etc.)
 */
@Entity
@Table(name = "users")
@Getter
@Setter
public class User extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kitchen_id")
    private com.restaurantpos.kitchen.entity.Kitchen kitchen;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<EmployeeKitchen> employeeKitchens = new HashSet<>();

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "pin_hash", length = 255)
    private String pinHash;

    @Column(name = "language", length = 10)
    private String language = "uz";

    @Column(name = "theme", length = 20)
    private String theme = "dark";

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "failed_login_attempts")
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    public String getFullName() {
        return firstName + (lastName != null ? " " + lastName : "");
    }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.lockedUntil = Instant.now().plusSeconds(900); // Lock 15 minutes
        }
    }

    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    public com.restaurantpos.kitchen.entity.Kitchen getKitchen() {
        if (employeeKitchens != null && !employeeKitchens.isEmpty()) {
            return employeeKitchens.iterator().next().getKitchen();
        }
        return this.kitchen;
    }

    public Set<com.restaurantpos.kitchen.entity.Kitchen> getKitchens() {
        if (employeeKitchens != null && !employeeKitchens.isEmpty()) {
            return employeeKitchens.stream()
                    .map(EmployeeKitchen::getKitchen)
                    .collect(Collectors.toSet());
        }
        return this.kitchen != null ? Set.of(this.kitchen) : Collections.emptySet();
    }

    public List<UUID> getKitchenIds() {
        if (employeeKitchens != null && !employeeKitchens.isEmpty()) {
            return employeeKitchens.stream()
                    .map(ek -> ek.getKitchen().getId())
                    .collect(Collectors.toList());
        }
        return this.kitchen != null ? List.of(this.kitchen.getId()) : Collections.emptyList();
    }
}
