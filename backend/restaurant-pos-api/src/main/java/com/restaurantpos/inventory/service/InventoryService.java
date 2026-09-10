package com.restaurantpos.inventory.service;

import com.restaurantpos.common.exception.PosException;
import com.restaurantpos.inventory.dto.InventoryDto;
import com.restaurantpos.inventory.entity.InventoryItem;
import com.restaurantpos.inventory.entity.InventoryTransaction;
import com.restaurantpos.inventory.repository.InventoryItemRepository;
import com.restaurantpos.inventory.repository.InventoryTransactionRepository;
import com.restaurantpos.tenants.entity.Tenant;
import com.restaurantpos.tenants.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository itemRepository;
    private final InventoryTransactionRepository txRepository;
    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public List<InventoryDto.Response> getAllItems(UUID tenantId) {
        return itemRepository.findByTenantIdAndDeletedAtIsNullOrderByNameAsc(tenantId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventoryDto.Response> getLowStockItems(UUID tenantId) {
        return itemRepository.findLowStockItems(tenantId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public InventoryDto.Response createItem(UUID tenantId, InventoryDto.CreateRequest req) {
        if (req.getSku() != null && itemRepository.existsByTenantIdAndSkuAndDeletedAtIsNull(tenantId, req.getSku())) {
            throw PosException.badRequest("SKU already exists: " + req.getSku());
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> PosException.notFound("Tenant not found"));

        InventoryItem item = new InventoryItem();
        item.setTenant(tenant);
        item.setName(req.getName());
        item.setSku(req.getSku());
        item.setUnit(req.getUnit() != null ? req.getUnit() : "pcs");
        item.setQuantity(req.getQuantity() != null ? req.getQuantity() : BigDecimal.ZERO);
        item.setMinQuantity(req.getMinQuantity() != null ? req.getMinQuantity() : BigDecimal.ZERO);
        item.setCostPrice(req.getCostPrice());
        item.setCategory(req.getCategory());
        item.setNotes(req.getNotes());

        return toResponse(itemRepository.save(item));
    }

    @Transactional
    public InventoryDto.Response adjustStock(UUID tenantId, UUID itemId, InventoryDto.AdjustRequest req, UUID userId) {
        InventoryItem item = itemRepository.findByIdAndTenantIdAndDeletedAtIsNull(itemId, tenantId)
                .orElseThrow(() -> PosException.notFound("Inventory item not found: " + itemId));

        BigDecimal before = item.getQuantity();
        BigDecimal after = before.add(req.getQuantity());

        if (after.compareTo(BigDecimal.ZERO) < 0) {
            throw PosException.badRequest("Insufficient stock. Current: " + before + ", Requested: " + req.getQuantity());
        }

        item.setQuantity(after);
        item.setUpdatedAt(Instant.now());
        itemRepository.save(item);

        // Transaction log
        InventoryTransaction tx = new InventoryTransaction();
        tx.setTenant(item.getTenant());
        tx.setItem(item);
        tx.setType(InventoryTransaction.TransactionType.valueOf(req.getType() != null ? req.getType().toUpperCase() : "ADJUSTMENT"));
        tx.setQuantity(req.getQuantity());
        tx.setQuantityBefore(before);
        tx.setQuantityAfter(after);
        tx.setNotes(req.getNotes());
        txRepository.save(tx);

        log.info("Inventory adjusted: item={} before={} after={} type={}", itemId, before, after, req.getType());

        if (item.isLowStock()) {
            log.warn("LOW STOCK ALERT: item={} name={} qty={} min={}", itemId, item.getName(), after, item.getMinQuantity());
        }

        return toResponse(item);
    }

    /** Buyurtma to'langanida ingredientlarni avtomatik ayirish */
    @Transactional
    public void deductStockForOrder(UUID tenantId, UUID orderId, List<StockDeduction> deductions) {
        for (StockDeduction d : deductions) {
            itemRepository.findByIdAndTenantIdAndDeletedAtIsNull(d.itemId(), tenantId).ifPresent(item -> {
                BigDecimal before = item.getQuantity();
                BigDecimal after = before.subtract(d.quantity());
                item.setQuantity(after.max(BigDecimal.ZERO)); // manfiy bo'lmasin
                item.setUpdatedAt(Instant.now());
                itemRepository.save(item);

                InventoryTransaction tx = new InventoryTransaction();
                tx.setTenant(item.getTenant());
                tx.setItem(item);
                tx.setType(InventoryTransaction.TransactionType.SALE);
                tx.setQuantity(d.quantity().negate());
                tx.setQuantityBefore(before);
                tx.setQuantityAfter(item.getQuantity());
                tx.setReferenceType("ORDER");
                tx.setReferenceId(orderId);
                txRepository.save(tx);
            });
        }
    }

    public record StockDeduction(UUID itemId, BigDecimal quantity) {}

    @Transactional(readOnly = true)
    public List<InventoryDto.TransactionResponse> getTransactions(UUID itemId, int limit) {
        return txRepository.findByItemIdOrderByCreatedAtDesc(itemId, PageRequest.of(0, limit))
                .stream().map(this::toTxResponse).collect(Collectors.toList());
    }

    @Transactional
    public void deleteItem(UUID tenantId, UUID itemId) {
        InventoryItem item = itemRepository.findByIdAndTenantIdAndDeletedAtIsNull(itemId, tenantId)
                .orElseThrow(() -> PosException.notFound("Inventory item not found: " + itemId));
        item.setDeletedAt(Instant.now());
        itemRepository.save(item);
    }

    private InventoryDto.Response toResponse(InventoryItem item) {
        return InventoryDto.Response.builder()
                .id(item.getId())
                .name(item.getName())
                .sku(item.getSku())
                .unit(item.getUnit())
                .quantity(item.getQuantity())
                .minQuantity(item.getMinQuantity())
                .costPrice(item.getCostPrice())
                .category(item.getCategory())
                .lowStock(item.isLowStock())
                .active(item.isActive())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private InventoryDto.TransactionResponse toTxResponse(InventoryTransaction tx) {
        return InventoryDto.TransactionResponse.builder()
                .id(tx.getId())
                .itemId(tx.getItem().getId())
                .itemName(tx.getItem().getName())
                .type(tx.getType().name())
                .quantity(tx.getQuantity())
                .quantityBefore(tx.getQuantityBefore())
                .quantityAfter(tx.getQuantityAfter())
                .notes(tx.getNotes())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
