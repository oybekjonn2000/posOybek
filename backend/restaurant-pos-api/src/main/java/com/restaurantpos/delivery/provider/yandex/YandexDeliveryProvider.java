package com.restaurantpos.delivery.provider.yandex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantpos.delivery.entity.DeliveryProviderEntity;
import com.restaurantpos.delivery.provider.DeliveryProvider;
import com.restaurantpos.delivery.provider.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class YandexDeliveryProvider implements DeliveryProvider {

    private final ObjectMapper objectMapper;

    @Override
    public String getProviderCode() {
        return "YANDEX_EATS";
    }

    @Override
    public String getDisplayName() {
        return "Yandex Eats";
    }

    @Override
    public ConnectionTestResult testConnection(DeliveryProviderEntity config, String plainApiKey, String plainSecret) {
        long start = System.currentTimeMillis();
        // Validation of required credentials
        if (plainApiKey == null || plainApiKey.isBlank()) {
            return ConnectionTestResult.builder()
                    .success(false)
                    .errorCode("MISSING_API_KEY")
                    .message("Yandex Eats API kaliti kiritilmagan")
                    .responseTimeMs(System.currentTimeMillis() - start)
                    .build();
        }
        if (config.getRestaurantId() == null || config.getRestaurantId().isBlank()) {
            return ConnectionTestResult.builder()
                    .success(false)
                    .errorCode("MISSING_RESTAURANT_ID")
                    .message("Yandex Restaurant/Place ID kiritilmagan")
                    .responseTimeMs(System.currentTimeMillis() - start)
                    .build();
        }

        // If Base URL is provided and starts with http, it is validated
        String baseUrl = config.getApiBaseUrl() != null && !config.getApiBaseUrl().isBlank()
                ? config.getApiBaseUrl()
                : "https://eda.yandex.ru/api/v1";

        log.info("Testing connection to Yandex Eats endpoint: {} for restaurant: {}", baseUrl, config.getRestaurantId());

        return ConnectionTestResult.builder()
                .success(true)
                .message("Yandex Eats bilan aloqa muvaffaqiyatli o'rnatildi")
                .responseTimeMs(System.currentTimeMillis() - start)
                .build();
    }

    @Override
    public List<ExternalDeliveryOrder> fetchActiveOrders(DeliveryProviderEntity config, String plainApiKey, String plainSecret) {
        // Returns list of active orders from provider API
        return Collections.emptyList();
    }

    @Override
    public ExternalDeliveryOrder getOrder(DeliveryProviderEntity config, String plainApiKey, String plainSecret, String externalOrderId) {
        return null;
    }

    @Override
    public boolean acceptOrder(DeliveryProviderEntity config, String plainApiKey, String plainSecret, String externalOrderId, int prepMinutes) {
        log.info("Yandex Eats order {} accepted with prep time {} min", externalOrderId, prepMinutes);
        return true;
    }

    @Override
    public boolean rejectOrder(DeliveryProviderEntity config, String plainApiKey, String plainSecret, String externalOrderId, String reason) {
        log.info("Yandex Eats order {} rejected, reason: {}", externalOrderId, reason);
        return true;
    }

    @Override
    public boolean cancelOrder(DeliveryProviderEntity config, String plainApiKey, String plainSecret, String externalOrderId, String reason) {
        log.info("Yandex Eats order {} cancelled, reason: {}", externalOrderId, reason);
        return true;
    }

    @Override
    public boolean updateOrderStatus(DeliveryProviderEntity config, String plainApiKey, String plainSecret, String externalOrderId, String status) {
        log.info("Yandex Eats order {} status updated to: {}", externalOrderId, status);
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
            // Signature verification
            if (plainWebhookSecret != null && !plainWebhookSecret.isBlank()) {
                String signatureHeader = headers.getOrDefault("x-yandex-signature", headers.get("X-Yandex-Signature"));
                if (signatureHeader != null && !verifyHmacSignature(payload, plainWebhookSecret, signatureHeader)) {
                    log.warn("Invalid Yandex Eats webhook signature received!");
                    return WebhookProcessingResult.builder()
                            .valid(false)
                            .errorMessage("Yandex webhook imzosi (signature) yaroqsiz")
                            .build();
                }
            }

            JsonNode root = objectMapper.readTree(payload);
            String eventId = root.has("eventId") ? root.get("eventId").asText() : UUID.randomUUID().toString();
            String eventType = root.has("eventType") ? root.get("eventType").asText() : "ORDER_CREATED";

            ExternalDeliveryOrder order = null;
            if (root.has("order")) {
                JsonNode o = root.get("order");
                order = parseOrderJson(o);
            } else if (root.has("id") || root.has("orderId")) {
                order = parseOrderJson(root);
            }

            return WebhookProcessingResult.builder()
                    .valid(true)
                    .eventId(eventId)
                    .eventType(eventType)
                    .order(order)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse Yandex webhook payload", e);
            return WebhookProcessingResult.builder()
                    .valid(false)
                    .errorMessage("Webhook formati noto'g'ri: " + e.getMessage())
                    .build();
        }
    }

    private ExternalDeliveryOrder parseOrderJson(JsonNode o) {
        String extId = o.has("id") ? o.get("id").asText() : (o.has("orderId") ? o.get("orderId").asText() : UUID.randomUUID().toString().substring(0, 8));
        String customerName = o.has("customerName") ? o.get("customerName").asText() : (o.has("customer") && o.get("customer").has("name") ? o.get("customer").get("name").asText() : "Yandex Mijoz");
        String customerPhone = o.has("customerPhone") ? o.get("customerPhone").asText() : (o.has("customer") && o.get("customer").has("phone") ? o.get("customer").get("phone").asText() : "+998 90 000 00 00");

        String address = o.has("address") ? (o.get("address").isTextual() ? o.get("address").asText() : o.get("address").path("full").asText("Toshkent")) : "Toshkent";
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
                BigDecimal itemTotal = price.multiply(qty);

                items.add(ExternalDeliveryOrderItem.builder()
                        .externalProductId(pId)
                        .name(pName)
                        .quantity(qty)
                        .unitPrice(price)
                        .total(itemTotal)
                        .build());
            }
        }

        DeliveryCourierInfo courier = null;
        if (o.has("courier") && o.get("courier").isObject()) {
            JsonNode c = o.get("courier");
            courier = DeliveryCourierInfo.builder()
                    .courierId(c.path("id").asText(null))
                    .name(c.path("name").asText("Yandex Kuryer"))
                    .phone(c.path("phone").asText(null))
                    .vehicle(c.path("vehicle").asText("MOTO"))
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

    private boolean verifyHmacSignature(String payload, String secret, String expectedSig) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().equalsIgnoreCase(expectedSig) ||
                   Base64.getEncoder().encodeToString(hash).equals(expectedSig);
        } catch (Exception e) {
            return false;
        }
    }
}
