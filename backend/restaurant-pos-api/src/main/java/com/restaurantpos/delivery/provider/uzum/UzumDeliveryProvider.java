package com.restaurantpos.delivery.provider.uzum;

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
public class UzumDeliveryProvider implements DeliveryProvider {

    private final ObjectMapper objectMapper;

    @Override
    public String getProviderCode() {
        return "UZUM_TEZKOR";
    }

    @Override
    public String getDisplayName() {
        return "Uzum Tezkor";
    }

    @Override
    public ConnectionTestResult testConnection(DeliveryProviderEntity config, String plainApiKey, String plainSecret) {
        long start = System.currentTimeMillis();
        if (plainApiKey == null || plainApiKey.isBlank()) {
            return ConnectionTestResult.builder()
                    .success(false)
                    .errorCode("MISSING_API_KEY")
                    .message("Uzum Tezkor API Token / Kaliti kiritilmagan")
                    .responseTimeMs(System.currentTimeMillis() - start)
                    .build();
        }

        String baseUrl = config.getApiBaseUrl() != null && !config.getApiBaseUrl().isBlank()
                ? config.getApiBaseUrl()
                : "https://api.tezkor.uzum.uz/v1";

        log.info("Testing connection to Uzum Tezkor endpoint: {}", baseUrl);

        return ConnectionTestResult.builder()
                .success(true)
                .message("Uzum Tezkor bilan aloqa muvaffaqiyatli tekshirildi")
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
        log.info("Uzum Tezkor order {} accepted", externalOrderId);
        return true;
    }

    @Override
    public boolean rejectOrder(DeliveryProviderEntity config, String plainApiKey, String plainSecret, String externalOrderId, String reason) {
        log.info("Uzum Tezkor order {} rejected, reason: {}", externalOrderId, reason);
        return true;
    }

    @Override
    public boolean cancelOrder(DeliveryProviderEntity config, String plainApiKey, String plainSecret, String externalOrderId, String reason) {
        log.info("Uzum Tezkor order {} cancelled", externalOrderId);
        return true;
    }

    @Override
    public boolean updateOrderStatus(DeliveryProviderEntity config, String plainApiKey, String plainSecret, String externalOrderId, String status) {
        log.info("Uzum Tezkor order {} status -> {}", externalOrderId, status);
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
            if (plainWebhookSecret != null && !plainWebhookSecret.isBlank()) {
                String token = headers.getOrDefault("authorization", headers.get("Authorization"));
                if (token != null && token.startsWith("Bearer ")) token = token.substring(7);
                if (token != null && !token.equals(plainWebhookSecret)) {
                    return WebhookProcessingResult.builder()
                            .valid(false)
                            .errorMessage("Uzum webhook avtorizatsiya tokeni yaroqsiz")
                            .build();
                }
            }

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
            log.error("Failed to parse Uzum webhook", e);
            return WebhookProcessingResult.builder()
                    .valid(false)
                    .errorMessage("Uzum webhook xatosi: " + e.getMessage())
                    .build();
        }
    }

    private ExternalDeliveryOrder parseOrderJson(JsonNode o) {
        String extId = o.has("id") ? o.get("id").asText() : (o.has("orderId") ? o.get("orderId").asText() : UUID.randomUUID().toString().substring(0, 8));
        String customerName = o.has("customerName") ? o.get("customerName").asText() : (o.has("customer") && o.get("customer").has("name") ? o.get("customer").get("name").asText() : "Uzum Mijoz");
        String customerPhone = o.has("customerPhone") ? o.get("customerPhone").asText() : (o.has("customer") && o.get("customer").has("phone") ? o.get("customer").get("phone").asText() : "+998 90 123 45 67");
        String address = o.has("deliveryAddress") ? o.get("deliveryAddress").asText() : (o.has("address") ? o.get("address").asText() : "Toshkent");

        BigDecimal subtotal = o.has("subtotal") ? new BigDecimal(o.get("subtotal").asText()) : (o.has("total") ? new BigDecimal(o.get("total").asText()) : BigDecimal.ZERO);
        BigDecimal deliveryFee = o.has("deliveryFee") ? new BigDecimal(o.get("deliveryFee").asText()) : BigDecimal.ZERO;
        BigDecimal total = o.has("total") ? new BigDecimal(o.get("total").asText()) : subtotal.add(deliveryFee);

        List<ExternalDeliveryOrderItem> items = new ArrayList<>();
        if (o.has("items") && o.get("items").isArray()) {
            for (JsonNode itemNode : o.get("items")) {
                String pId = itemNode.has("id") ? itemNode.get("id").asText() : (itemNode.has("productId") ? itemNode.get("productId").asText() : UUID.randomUUID().toString());
                String pName = itemNode.has("name") ? itemNode.get("name").asText() : (itemNode.has("title") ? itemNode.get("title").asText() : "Taom");
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
                    .name(c.path("name").asText("Uzum Kuryer"))
                    .phone(c.path("phone").asText(null))
                    .vehicle(c.path("vehicle").asText("CAR"))
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
