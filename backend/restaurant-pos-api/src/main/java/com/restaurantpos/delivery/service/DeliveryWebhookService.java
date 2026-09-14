package com.restaurantpos.delivery.service;

import com.restaurantpos.common.exception.PosException;
import com.restaurantpos.delivery.config.DeliveryEncryptionService;
import com.restaurantpos.delivery.entity.DeliveryIntegrationLogEntity;
import com.restaurantpos.delivery.entity.DeliveryProviderEntity;
import com.restaurantpos.delivery.entity.DeliveryWebhookEventEntity;
import com.restaurantpos.delivery.provider.DeliveryProvider;
import com.restaurantpos.delivery.provider.DeliveryProviderRegistry;
import com.restaurantpos.delivery.provider.model.WebhookProcessingResult;
import com.restaurantpos.delivery.repository.DeliveryProviderRepository;
import com.restaurantpos.delivery.repository.DeliveryWebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryWebhookService {

    private final DeliveryProviderRepository providerRepository;
    private final DeliveryProviderRegistry providerRegistry;
    private final DeliveryWebhookEventRepository webhookEventRepository;
    private final DeliveryEncryptionService encryptionService;
    private final DeliveryOrderService deliveryOrderService;
    private final DeliveryLogService logService;

    @Transactional
    public Map<String, Object> processWebhook(String providerCode, String payload, Map<String, String> headers) {
        long start = System.currentTimeMillis();

        DeliveryProviderEntity providerConfig = providerRepository.findByCodeAndDeletedAtIsNull(providerCode.toUpperCase())
                .orElseThrow(() -> PosException.notFound("Provayder topilmadi: " + providerCode));

        DeliveryProvider provider = providerRegistry.getProvider(providerConfig.getCode())
                .orElseThrow(() -> PosException.badRequest("Provayder adapteri topilmadi: " + providerConfig.getCode()));

        String plainWebhookSecret = encryptionService.decrypt(providerConfig.getWebhookSecret());

        WebhookProcessingResult result = provider.handleWebhook(providerConfig, plainWebhookSecret, payload, headers);

        if (!result.isValid()) {
            logService.logAction(providerConfig.getTenant(), providerConfig, "WEBHOOK_REJECTED", null,
                    DeliveryIntegrationLogEntity.LogStatus.FAILED, System.currentTimeMillis() - start,
                    result.getErrorMessage(), "Webhook rad etildi");
            throw PosException.badRequest(result.getErrorMessage() != null ? result.getErrorMessage() : "Yaroqsiz webhook");
        }

        // Idempotency: Check if webhook event with eventId was already processed
        if (result.getEventId() != null) {
            Optional<DeliveryWebhookEventEntity> existingEvent = webhookEventRepository
                    .findByProviderIdAndExternalEventId(providerConfig.getId(), result.getEventId());

            if (existingEvent.isPresent()) {
                log.info("Duplicate webhook event ignored: [Provider: {}, EventId: {}]", providerConfig.getCode(), result.getEventId());
                return Map.of("success", true, "message", "Duplicate event already processed");
            }

            DeliveryWebhookEventEntity event = new DeliveryWebhookEventEntity();
            event.setTenant(providerConfig.getTenant());
            event.setProvider(providerConfig);
            event.setExternalEventId(result.getEventId());
            event.setEventType(result.getEventType() != null ? result.getEventType() : "GENERIC");
            event.setProcessed(true);
            event.setPayload(payload);
            webhookEventRepository.save(event);
        }

        // Process order if present in webhook
        if (result.getOrder() != null) {
            deliveryOrderService.ingestExternalOrder(result.getOrder(), providerConfig);
        }

        logService.logAction(providerConfig.getTenant(), providerConfig, "WEBHOOK_PROCESSED", result.getEventId(),
                DeliveryIntegrationLogEntity.LogStatus.SUCCESS, System.currentTimeMillis() - start, null,
                "Webhook muvaffaqiyatli qayta ishlandi (Voqea: " + result.getEventType() + ")");

        return Map.of("success", true, "message", "Webhook qabul qilindi", "eventId", result.getEventId() != null ? result.getEventId() : "N/A");
    }
}
