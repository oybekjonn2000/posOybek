package com.restaurantpos.delivery.provider.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionTestResult {
    private boolean success;
    private String message;
    private String errorCode;
    private long responseTimeMs;
}
