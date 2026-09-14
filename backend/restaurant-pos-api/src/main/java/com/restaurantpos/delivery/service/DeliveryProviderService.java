package com.restaurantpos.delivery.service;

import com.restaurantpos.common.exception.PosException;
import com.restaurantpos.delivery.config.DeliveryEncryptionService;
import com.restaurantpos.delivery.dto.DeliveryProviderDto;
import com.restaurantpos.delivery.entity.DeliveryIntegrationLogEntity;
import com.restaurantpos.delivery.entity.DeliveryProviderEntity;
import com.restaurantpos.delivery.provider.DeliveryProvider;
import com.restaurantpos.delivery.provider.DeliveryProviderRegistry;
import com.restaurantpos.delivery.provider.model.ConnectionTestResult;
import com.restaurantpos.delivery.repository.DeliveryOrderRepository;
import com.restaurantpos.delivery.repository.DeliveryProviderRepository;
import com.restaurantpos.tenants.entity.Tenant;
import com.restaurantpos.tenants.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryProviderService {

    private final DeliveryProviderRepository providerRepository;
    private final DeliveryOrderRepository orderRepository;
    private final TenantRepository tenantRepository;
    private final DeliveryProviderRegistry providerRegistry;
    private final DeliveryEncryptionService encryptionService;
    private final DeliveryLogService logService;

    @Transactional(readOnly = true)
    public List<DeliveryProviderDto.Response> getProviders(UUID tenantId) {
        return providerRepository.findByTenantIdAndDeletedAtIsNull(tenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DeliveryProviderDto.Response getProvider(UUID tenantId, UUID id) {
        DeliveryProviderEntity entity = providerRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Delivery provayder topilmadi: " + id));
        return toResponse(entity);
    }

    @Transactional
    public DeliveryProviderDto.Response createProvider(UUID tenantId, DeliveryProviderDto.CreateRequest req) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> PosException.notFound("Tenant topilmadi"));

        if (providerRepository.existsByTenantIdAndCodeAndDeletedAtIsNull(tenantId, req.getCode())) {
            throw PosException.conflict("Ushbu kodli provayder allaqachon mavjud: " + req.getCode(), "PROVIDER_EXISTS");
        }

        DeliveryProviderEntity entity = new DeliveryProviderEntity();
        entity.setTenant(tenant);
        entity.setName(req.getName());
        entity.setCode(req.getCode().toUpperCase());
        entity.setProviderType(DeliveryProviderEntity.ProviderType.valueOf(req.getProviderType().toUpperCase()));
        entity.setStatus(DeliveryProviderEntity.ProviderStatus.DISCONNECTED);
        entity.setApiBaseUrl(req.getApiBaseUrl());
        entity.setRestaurantId(req.getRestaurantId());
        entity.setWebhookUrl(req.getWebhookUrl());

        if (req.getApiKey() != null && !req.getApiKey().isBlank()) {
            entity.setEncryptedApiKey(encryptionService.encrypt(req.getApiKey()));
        }
        if (req.getClientId() != null && !req.getClientId().isBlank()) {
            entity.setEncryptedClientId(encryptionService.encrypt(req.getClientId()));
        }
        if (req.getSecret() != null && !req.getSecret().isBlank()) {
            entity.setEncryptedSecret(encryptionService.encrypt(req.getSecret()));
        }
        if (req.getWebhookSecret() != null && !req.getWebhookSecret().isBlank()) {
            entity.setWebhookSecret(encryptionService.encrypt(req.getWebhookSecret()));
        }

        if (req.getAutoAccept() != null) entity.setAutoAccept(req.getAutoAccept());
        if (req.getAutoPrintKitchen() != null) entity.setAutoPrintKitchen(req.getAutoPrintKitchen());
        if (req.getAutoPrintReceipt() != null) entity.setAutoPrintReceipt(req.getAutoPrintReceipt());
        if (req.getSoundNotification() != null) entity.setSoundNotification(req.getSoundNotification());
        if (req.getAutoSync() != null) entity.setAutoSync(req.getAutoSync());
        if (req.getSyncIntervalSeconds() != null) entity.setSyncIntervalSeconds(req.getSyncIntervalSeconds());
        if (req.getCommissionType() != null) entity.setCommissionType(DeliveryProviderEntity.CommissionType.valueOf(req.getCommissionType()));
        if (req.getCommissionValue() != null) entity.setCommissionValue(req.getCommissionValue());
        if (req.getDefaultPaymentType() != null) entity.setDefaultPaymentType(req.getDefaultPaymentType());

        DeliveryProviderEntity saved = providerRepository.save(entity);
        logService.logAction(tenant, saved, "CREATE_PROVIDER", saved.getCode(), DeliveryIntegrationLogEntity.LogStatus.SUCCESS, 0L, null, "Yangi provayder qo'shildi");
        return toResponse(saved);
    }

    @Transactional
    public DeliveryProviderDto.Response updateProvider(UUID tenantId, UUID id, DeliveryProviderDto.UpdateRequest req) {
        DeliveryProviderEntity entity = providerRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Delivery provayder topilmadi: " + id));

        if (req.getName() != null) entity.setName(req.getName());
        if (req.getApiBaseUrl() != null) entity.setApiBaseUrl(req.getApiBaseUrl());
        if (req.getRestaurantId() != null) entity.setRestaurantId(req.getRestaurantId());
        if (req.getWebhookUrl() != null) entity.setWebhookUrl(req.getWebhookUrl());

        if (req.getApiKey() != null && !req.getApiKey().isBlank()) {
            entity.setEncryptedApiKey(encryptionService.encrypt(req.getApiKey()));
        }
        if (req.getClientId() != null && !req.getClientId().isBlank()) {
            entity.setEncryptedClientId(encryptionService.encrypt(req.getClientId()));
        }
        if (req.getSecret() != null && !req.getSecret().isBlank()) {
            entity.setEncryptedSecret(encryptionService.encrypt(req.getSecret()));
        }
        if (req.getWebhookSecret() != null && !req.getWebhookSecret().isBlank()) {
            entity.setWebhookSecret(encryptionService.encrypt(req.getWebhookSecret()));
        }

        if (req.getAutoAccept() != null) entity.setAutoAccept(req.getAutoAccept());
        if (req.getAutoPrintKitchen() != null) entity.setAutoPrintKitchen(req.getAutoPrintKitchen());
        if (req.getAutoPrintReceipt() != null) entity.setAutoPrintReceipt(req.getAutoPrintReceipt());
        if (req.getSoundNotification() != null) entity.setSoundNotification(req.getSoundNotification());
        if (req.getAutoSync() != null) entity.setAutoSync(req.getAutoSync());
        if (req.getSyncIntervalSeconds() != null) entity.setSyncIntervalSeconds(req.getSyncIntervalSeconds());
        if (req.getCommissionType() != null) entity.setCommissionType(DeliveryProviderEntity.CommissionType.valueOf(req.getCommissionType()));
        if (req.getCommissionValue() != null) entity.setCommissionValue(req.getCommissionValue());
        if (req.getDefaultPaymentType() != null) entity.setDefaultPaymentType(req.getDefaultPaymentType());

        DeliveryProviderEntity saved = providerRepository.save(entity);
        logService.logAction(entity.getTenant(), saved, "UPDATE_PROVIDER", saved.getCode(), DeliveryIntegrationLogEntity.LogStatus.SUCCESS, 0L, null, "Provayder sozlamalari yangilandi");
        return toResponse(saved);
    }

    @Transactional
    public DeliveryProviderDto.TestConnectionResponse testConnection(UUID tenantId, UUID id) {
        DeliveryProviderEntity entity = providerRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Delivery provayder topilmadi: " + id));

        DeliveryProvider provider = providerRegistry.getProvider(entity.getCode())
                .orElseThrow(() -> PosException.badRequest("Provayder adapteri topilmadi: " + entity.getCode()));

        String plainApiKey = encryptionService.decrypt(entity.getEncryptedApiKey());
        String plainSecret = encryptionService.decrypt(entity.getEncryptedSecret());

        ConnectionTestResult result = provider.testConnection(entity, plainApiKey, plainSecret);

        entity.setLastConnectionTestAt(Instant.now());
        entity.setLastConnectionStatus(result.isSuccess() ? "SUCCESS" : "FAIL");
        entity.setLastConnectionError(result.isSuccess() ? null : result.getMessage());
        providerRepository.save(entity);

        logService.logAction(entity.getTenant(), entity, "TEST_CONNECTION", entity.getCode(),
                result.isSuccess() ? DeliveryIntegrationLogEntity.LogStatus.SUCCESS : DeliveryIntegrationLogEntity.LogStatus.FAILED,
                result.getResponseTimeMs(), result.getMessage(), null);

        return DeliveryProviderDto.TestConnectionResponse.builder()
                .success(result.isSuccess())
                .message(result.getMessage())
                .errorCode(result.getErrorCode())
                .responseTimeMs(result.getResponseTimeMs())
                .build();
    }

    @Transactional
    public DeliveryProviderDto.Response connectProvider(UUID tenantId, UUID id) {
        DeliveryProviderEntity entity = providerRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Delivery provayder topilmadi: " + id));

        entity.setStatus(DeliveryProviderEntity.ProviderStatus.CONNECTED);
        entity.setLastSyncAt(Instant.now());
        DeliveryProviderEntity saved = providerRepository.save(entity);

        logService.logAction(entity.getTenant(), entity, "CONNECT_PROVIDER", entity.getCode(),
                DeliveryIntegrationLogEntity.LogStatus.SUCCESS, 0L, null, "Provayder faollashtirildi (CONNECTED)");

        return toResponse(saved);
    }

    @Transactional
    public DeliveryProviderDto.Response disconnectProvider(UUID tenantId, UUID id) {
        DeliveryProviderEntity entity = providerRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Delivery provayder topilmadi: " + id));

        entity.setStatus(DeliveryProviderEntity.ProviderStatus.DISCONNECTED);
        DeliveryProviderEntity saved = providerRepository.save(entity);

        logService.logAction(entity.getTenant(), entity, "DISCONNECT_PROVIDER", entity.getCode(),
                DeliveryIntegrationLogEntity.LogStatus.SUCCESS, 0L, null, "Provayder uzildi (DISCONNECTED)");

        return toResponse(saved);
    }

    @Transactional
    public void deleteProvider(UUID tenantId, UUID id) {
        DeliveryProviderEntity entity = providerRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Delivery provayder topilmadi: " + id));

        entity.setStatus(DeliveryProviderEntity.ProviderStatus.DISABLED);
        entity.setDeletedAt(Instant.now());
        providerRepository.save(entity);

        logService.logAction(entity.getTenant(), entity, "DELETE_PROVIDER", entity.getCode(),
                DeliveryIntegrationLogEntity.LogStatus.SUCCESS, 0L, null, "Provayder o'chirildi/disable qilindi");
    }

    public DeliveryProviderDto.Response toResponse(DeliveryProviderEntity e) {
        String decryptedKey = encryptionService.decrypt(e.getEncryptedApiKey());
        String maskedKey = encryptionService.mask(decryptedKey);

        return DeliveryProviderDto.Response.builder()
                .id(e.getId())
                .name(e.getName())
                .code(e.getCode())
                .providerType(e.getProviderType() != null ? e.getProviderType().name() : null)
                .status(e.getStatus() != null ? e.getStatus().name() : null)
                .apiBaseUrl(e.getApiBaseUrl())
                .hasApiKey(e.getEncryptedApiKey() != null && !e.getEncryptedApiKey().isBlank())
                .hasSecret(e.getEncryptedSecret() != null && !e.getEncryptedSecret().isBlank())
                .maskedApiKey(maskedKey)
                .restaurantId(e.getRestaurantId())
                .webhookUrl(e.getWebhookUrl())
                .hasWebhookSecret(e.getWebhookSecret() != null && !e.getWebhookSecret().isBlank())
                .autoAccept(e.isAutoAccept())
                .autoPrintKitchen(e.isAutoPrintKitchen())
                .autoPrintReceipt(e.isAutoPrintReceipt())
                .soundNotification(e.isSoundNotification())
                .autoSync(e.isAutoSync())
                .syncIntervalSeconds(e.getSyncIntervalSeconds())
                .commissionType(e.getCommissionType() != null ? e.getCommissionType().name() : "PERCENTAGE")
                .commissionValue(e.getCommissionValue())
                .defaultOrderSource(e.getDefaultOrderSource())
                .defaultPaymentType(e.getDefaultPaymentType())
                .lastSyncAt(e.getLastSyncAt())
                .lastConnectionTestAt(e.getLastConnectionTestAt())
                .lastConnectionStatus(e.getLastConnectionStatus())
                .lastConnectionError(e.getLastConnectionError())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
