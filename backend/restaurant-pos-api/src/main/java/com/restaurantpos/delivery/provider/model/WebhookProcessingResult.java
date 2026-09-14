package com.restaurantpos.delivery.provider.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookProcessingResult {
    private boolean valid;
    private String eventId;
    private String eventType;
    private ExternalDeliveryOrder order;
    private String errorMessage;
}
