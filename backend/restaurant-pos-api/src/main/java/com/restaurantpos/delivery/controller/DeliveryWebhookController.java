package com.restaurantpos.delivery.controller;

import com.restaurantpos.delivery.service.DeliveryWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/delivery/webhook")
@RequiredArgsConstructor
@Tag(name = "Delivery Webhooks", description = "Public Webhook Receiver for External Delivery Providers")
public class DeliveryWebhookController {

    private final DeliveryWebhookService webhookService;

    @PostMapping("/{providerCode}")
    @Operation(summary = "Ingest webhook event from external delivery service (Yandex, Uzum, Glovo, Custom)")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @PathVariable String providerCode,
            @RequestBody String rawPayload,
            @RequestHeader Map<String, String> headers) {

        log.info("Received delivery webhook for provider [{}] with {} bytes payload", providerCode, rawPayload.length());
        Map<String, Object> result = webhookService.processWebhook(providerCode, rawPayload, headers);
        return ResponseEntity.ok(result);
    }
}
