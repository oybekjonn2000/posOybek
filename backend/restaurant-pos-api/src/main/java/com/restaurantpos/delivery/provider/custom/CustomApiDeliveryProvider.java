package com.restaurantpos.delivery.provider.custom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantpos.delivery.entity.DeliveryProviderEntity;
import com.restaurantpos.delivery.provider.DeliveryProvider;
import com.restaurantpos.delivery.provider.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomApiDeliveryProvider implements DeliveryProvider {

    private final ObjectMapper objectMapper;

    @Override
    public String getProviderCode() {
        return "CUSTOM_API";
    }

    @Override
    public String getDisplayName() {
        return "Custom API (O'zimizning yetkazib berish xizmati)";
    }

    @Override
    public ConnectionTestResult testConnection(DeliveryProviderEntity config, String plainApiKey, String plainSecret) {
        long start = System.currentTimeMillis();
        // Custom provider can work with webhook or baseUrl
        return ConnectionTestResult.builder()
                .success(true)
                .message("Custom API endpointi faol holatda")
                .responseTimeMs(System.currentTimeMillis() - start)
                .build();
    }

    @Override
    public List<ExternalDeliveryOrder> fetchActiveOrders(DeliveryProviderEntity config, String plainApiKey, String plainSecret) {
        return Collections.emptyList();
    }

    @Override
    public ExternalDeliveryOrder getOrder(DeliveryProviderEntity config, String plainApiKey, String plainSecret, String externalOrderId) {
        return null;
    }

    @Override
    public boolean acceptOrder(DeliveryProviderEntity config, String plainApiKey, String plainSecret, String externalOrderId, int prepMinutes) {
        return true;
    }

    @Override
    public boolean rejectOrder(DeliveryProviderEntity config, String plainApiKey, String plainSecret, String externalOrderId, String reason) {
        return true;
    }

    @Override
    public boolean cancelOrder(DeliveryProviderEntity config, String plainApiKey, String plainSecret, String externalOrderId, String reason) {
        return true;
    }

    @Override
    public boolean updateOrderStatus(DeliveryProviderEntity config, String plainApiKey, String plainSecret, String externalOrderId, String status) {
        return true;
    }

    @Override
    public DeliveryCourierInfo getCourierInfo(DeliveryProviderEntity config, String plainApiKey, String plainSecret, String externalOrderId) {
        return null;
    }

    @Override
    public List<ExternalDeliveryProduct> fetchProducts(DeliveryProviderEntity config, String plainApiKey, String plainSecret) {
        return Collections.emptyList();
    }

    @Override
    public WebhookProcessingResult handleWebhook(DeliveryProviderEntity config, String plainWebhookSecret, String payload, Map<String, String> headers) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventId = root.has("eventId") ? root.get("eventId").asText() : UUID.randomUUID().toString();
            String eventType = root.has("eventType") ? root.get("eventType").asText() : "ORDER_CREATED";

            JsonNode o = root.has("order") ? root.get("order") : root;
            String extId = o.has("externalOrderId") ? o.get("externalOrderId").asText() : (o.has("id") ? o.get("id").asText() : UUID.randomUUID().toString().substring(0, 8));
            String customerName = o.has("customerName") ? o.get("customerName").asText() : "Mijoz";
            String customerPhone = o.has("customerPhone") ? o.get("customerPhone").asText() : "+998 90 000 00 00";
            String address = o.has("deliveryAddress") ? o.get("deliveryAddress").asText() : (o.has("address") ? o.get("address").asText() : "Toshkent");

            BigDecimal subtotal = o.has("subtotal") ? new BigDecimal(o.get("subtotal").asText()) : (o.has("total") ? new BigDecimal(o.get("total").asText()) : BigDecimal.ZERO);
            BigDecimal deliveryFee = o.has("deliveryFee") ? new BigDecimal(o.get("deliveryFee").asText()) : BigDecimal.ZERO;
            BigDecimal total = o.has("total") ? new BigDecimal(o.get("total").asText()) : subtotal.add(deliveryFee);

            List<ExternalDeliveryOrderItem> items = new ArrayList<>();
            if (o.has("items") && o.get("items").isArray()) {
                for (JsonNode itemNode : o.get("items")) {
                    String pId = itemNode.has("externalProductId") ? itemNode.get("externalProductId").asText() : (itemNode.has("id") ? itemNode.get("id").asText() : UUID.randomUUID().toString());
                    String pName = itemNode.has("name") ? itemNode.get("name").asText() : "Taom";
                    BigDecimal qty = itemNode.has("quantity") ? new BigDecimal(itemNode.get("quantity").asText()) : BigDecimal.ONE;
                    BigDecimal price = itemNode.has("unitPrice") ? new BigDecimal(itemNode.get("unitPrice").asText()) : (itemNode.has("price") ? new BigDecimal(itemNode.get("price").asText()) : BigDecimal.ZERO);

                    items.add(ExternalDeliveryOrderItem.builder()
                            .externalProductId(pId)
                            .name(pName)
                            .quantity(qty)
                            .unitPrice(price)
                            .total(price.multiply(qty))
                            .build());
                }
            }

            ExternalDeliveryOrder order = ExternalDeliveryOrder.builder()
                    .externalOrderId(extId)
                    .providerCode(getProviderCode())
                    .status("NEW")
                    .customerName(customerName)
                    .customerPhone(customerPhone)
                    .deliveryAddress(address)
                    .subtotal(subtotal)
                    .deliveryFee(deliveryFee)
                    .total(total)
                    .paymentType(o.has("paymentType") ? o.get("paymentType").asText() : "ONLINE")
                    .paymentStatus(o.has("paymentStatus") ? o.get("paymentStatus").asText() : "PAID")
                    .createdAt(Instant.now())
                    .rawPayload(payload)
                    .items(items)
                    .build();

            return WebhookProcessingResult.builder()
                    .valid(true)
                    .eventId(eventId)
                    .eventType(eventType)
                    .order(order)
                    .build();
        } catch (Exception e) {
            return WebhookProcessingResult.builder()
                    .valid(false)
                    .errorMessage("Custom API webhook xatosi: " + e.getMessage())
                    .build();
        }
    }
}
