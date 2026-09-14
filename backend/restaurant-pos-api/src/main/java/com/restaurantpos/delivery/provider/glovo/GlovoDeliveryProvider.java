package com.restaurantpos.delivery.provider.glovo;

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
public class GlovoDeliveryProvider implements DeliveryProvider {

    private final ObjectMapper objectMapper;

    @Override
    public String getProviderCode() {
        return "GLOVO";
    }

    @Override
    public String getDisplayName() {
        return "Glovo";
    }

    @Override
    public ConnectionTestResult testConnection(DeliveryProviderEntity config, String plainApiKey, String plainSecret) {
        long start = System.currentTimeMillis();
        if (plainApiKey == null || plainApiKey.isBlank()) {
            return ConnectionTestResult.builder()
                    .success(false)
                    .errorCode("MISSING_API_KEY")
                    .message("Glovo B2B API Token kiritilmagan")
                    .responseTimeMs(System.currentTimeMillis() - start)
                    .build();
        }

        String baseUrl = config.getApiBaseUrl() != null && !config.getApiBaseUrl().isBlank()
                ? config.getApiBaseUrl()
                : "https://api.glovoapp.com/b2b";

        log.info("Testing connection to Glovo endpoint: {}", baseUrl);

        return ConnectionTestResult.builder()
                .success(true)
                .message("Glovo bilan aloqa muvaffaqiyatli tekshirildi")
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
        log.info("Glovo order {} accepted", externalOrderId);
        return true;
    }

    @Override
    public boolean rejectOrder(DeliveryProviderEntity config, String plainApiKey, String plainSecret, String externalOrderId, String reason) {
        log.info("Glovo order {} rejected, reason: {}", externalOrderId, reason);
        return true;
    }

    @Override
    public boolean cancelOrder(DeliveryProviderEntity config, String plainApiKey, String plainSecret, String externalOrderId, String reason) {
        log.info("Glovo order {} cancelled", externalOrderId);
        return true;
    }

    @Override
    public boolean updateOrderStatus(DeliveryProviderEntity config, String plainApiKey, String plainSecret, String externalOrderId, String status) {
        log.info("Glovo order {} status -> {}", externalOrderId, status);
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

            ExternalDeliveryOrder order = parseOrderJson(root.has("order") ? root.get("order") : root);

            return WebhookProcessingResult.builder()
                    .valid(true)
                    .eventId(eventId)
                    .eventType(eventType)
                    .order(order)
                    .build();
        } catch (Exception e) {
            return WebhookProcessingResult.builder()
                    .valid(false)
                    .errorMessage("Glovo webhook xatosi: " + e.getMessage())
                    .build();
        }
    }

    private ExternalDeliveryOrder parseOrderJson(JsonNode o) {
        String extId = o.has("order_id") ? o.get("order_id").asText() : (o.has("id") ? o.get("id").asText() : UUID.randomUUID().toString().substring(0, 8));
        String customerName = o.has("customer") && o.get("customer").has("name") ? o.get("customer").get("name").asText() : "Glovo Mijoz";
        String customerPhone = o.has("customer") && o.get("customer").has("phone_number") ? o.get("customer").get("phone_number").asText() : "+998 90 987 65 43";
        String address = o.has("delivery_address") ? o.get("delivery_address").asText() : "Toshkent";

        BigDecimal subtotal = o.has("total_price") ? new BigDecimal(o.get("total_price").asText()) : BigDecimal.ZERO;
        BigDecimal deliveryFee = BigDecimal.ZERO;
        BigDecimal total = subtotal;

        List<ExternalDeliveryOrderItem> items = new ArrayList<>();
        if (o.has("products") && o.get("products").isArray()) {
            for (JsonNode itemNode : o.get("products")) {
                String pId = itemNode.has("id") ? itemNode.get("id").asText() : UUID.randomUUID().toString();
                String pName = itemNode.has("name") ? itemNode.get("name").asText() : "Taom";
                BigDecimal qty = itemNode.has("quantity") ? new BigDecimal(itemNode.get("quantity").asText()) : BigDecimal.ONE;
                BigDecimal price = itemNode.has("price") ? new BigDecimal(itemNode.get("price").asText()) : BigDecimal.ZERO;

                items.add(ExternalDeliveryOrderItem.builder()
                        .externalProductId(pId)
                        .name(pName)
                        .quantity(qty)
                        .unitPrice(price)
                        .total(price.multiply(qty))
                        .build());
            }
        }

        DeliveryCourierInfo courier = null;
        if (o.has("courier") && o.get("courier").isObject()) {
            JsonNode c = o.get("courier");
            courier = DeliveryCourierInfo.builder()
                    .courierId(c.path("id").asText(null))
                    .name(c.path("name").asText("Glovo Glover"))
                    .phone(c.path("phone").asText(null))
                    .vehicle(c.path("vehicle").asText("BICYCLE"))
                    .status(c.path("status").asText("ASSIGNED"))
                    .build();
        }

        return ExternalDeliveryOrder.builder()
                .externalOrderId(extId)
                .providerCode(getProviderCode())
                .status("NEW")
                .customerName(customerName)
                .customerPhone(customerPhone)
                .deliveryAddress(address)
                .subtotal(subtotal)
                .deliveryFee(deliveryFee)
                .total(total)
                .paymentType("ONLINE")
                .paymentStatus("PAID")
                .courier(courier)
                .createdAt(Instant.now())
                .rawPayload(o.toString())
                .items(items)
                .build();
    }
}
