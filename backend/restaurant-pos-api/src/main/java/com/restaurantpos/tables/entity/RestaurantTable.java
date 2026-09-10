package com.restaurantpos.tables.entity;

import com.restaurantpos.common.entity.BaseEntity;
import com.restaurantpos.tenants.entity.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Restaurant table entity — physical tables in the dining area.
 */
@Entity
@Table(name = "restaurant_tables")
@Getter
@Setter
public class RestaurantTable extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private TableZone zone;

    @Column(name = "table_number", nullable = false, length = 20)
    private String tableNumber;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "capacity", nullable = false)
    private int capacity = 4;

    @Column(name = "shape", length = 20)
    private String shape = "rectangle";

    @Column(name = "pos_x")
    private int posX = 0;

    @Column(name = "pos_y")
    private int posY = 0;

    @Column(name = "width")
    private int width = 100;

    @Column(name = "height")
    private int height = 80;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TableStatus status = TableStatus.FREE;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "current_order_id")
    private UUID currentOrderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waiter_id")
    private com.restaurantpos.users.entity.User waiter;

    public enum TableStatus {
        FREE, OCCUPIED, RESERVED, BILL_REQUESTED, CLEANING
    }
}
