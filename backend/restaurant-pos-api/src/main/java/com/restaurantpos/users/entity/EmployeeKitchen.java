package com.restaurantpos.users.entity;

import com.restaurantpos.common.entity.BaseEntity;
import com.restaurantpos.kitchen.entity.Kitchen;
import com.restaurantpos.tenants.entity.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "employee_kitchens",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_employee_kitchens", columnNames = {"employee_id", "kitchen_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class EmployeeKitchen extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kitchen_id", nullable = false)
    private Kitchen kitchen;

    public EmployeeKitchen(Tenant tenant, User employee, Kitchen kitchen) {
        this.tenant = tenant;
        this.employee = employee;
        this.kitchen = kitchen;
    }
}
