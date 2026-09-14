package com.restaurantpos.delivery.provider.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryCourierInfo {
    private String courierId;
    private String name;
    private String phone;
    private String vehicle; // CAR, BIKE, FOOT, SCOOTER
    private String status;  // ASSIGNED, ARRIVED_AT_RESTAURANT, PICKED_UP, DELIVERED
}
