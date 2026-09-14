package com.restaurantpos.delivery.provider;

import com.restaurantpos.delivery.entity.DeliveryProviderEntity;
import com.restaurantpos.delivery.provider.model.*;

import java.util.List;
import java.util.Map;

/**
 * Common Provider Adapter Interface for all Delivery Services.
 * Implementations exist for Yandex, Uzum, Glovo, and Custom REST APIs.
 */
public interface DeliveryProvider {

    String getProviderCode();

    String getDisplayName();

    ConnectionTestResult testConnection(DeliveryProviderEntity config, String plainApiKey, String plainSecret);

    List<ExternalDeliveryOrder> fetchActiveOrders(DeliveryProviderEntity config, String plainApiKey, String plainSecret);

    ExternalDeliveryOrder getOrder(DeliveryProviderEntity config, String plainApiKey, String plainSecret, String externalOrderId);

    boolean acceptOrder(DeliveryProviderEntity config, String plainApiKey, String plainSecret, String externalOrderId, int prepMinutes);

    boolean rejectOrder(DeliveryProviderEntity config, String plainApiKey, String plainSecret, String externalOrderId, String reason);

    boolean cancelOrder(DeliveryProviderEntity config, String plainApiKey, String plainSecret, String externalOrderId, String reason);

    boolean updateOrderStatus(DeliveryProviderEntity config, String plainApiKey, String plainSecret, String externalOrderId, String status);

    DeliveryCourierInfo getCourierInfo(DeliveryProviderEntity config, String plainApiKey, String plainSecret, String externalOrderId);

    List<ExternalDeliveryProduct> fetchProducts(DeliveryProviderEntity config, String plainApiKey, String plainSecret);

    WebhookProcessingResult handleWebhook(DeliveryProviderEntity config, String plainWebhookSecret, String payload, Map<String, String> headers);
}
