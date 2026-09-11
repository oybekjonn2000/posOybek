package com.restaurantpos.inventory.service;

import com.restaurantpos.common.exception.PosException;
import com.restaurantpos.inventory.dto.InventoryDto;
import com.restaurantpos.inventory.entity.*;
import com.restaurantpos.inventory.repository.*;
import com.restaurantpos.orders.entity.Order;
import com.restaurantpos.orders.entity.OrderItem;
import com.restaurantpos.products.entity.Product;
import com.restaurantpos.products.repository.ProductRepository;
import com.restaurantpos.tenants.entity.Tenant;
import com.restaurantpos.tenants.repository.TenantRepository;
import com.restaurantpos.users.entity.User;
import com.restaurantpos.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository itemRepository;
    private final InventoryTransactionRepository txRepository;
    private final WarehouseRepository warehouseRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final ProductIngredientRepository productIngredientRepository;
    private final InventoryAuditRepository auditRepository;
    private final InventoryAuditItemRepository auditItemRepository;
    private final ProductRepository productRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    // ==========================================
    // 1. DASHBOARD STATS
    // ==========================================
    @Transactional(readOnly = true)
    public InventoryDto.DashboardStats getDashboardStats(UUID tenantId) {
        List<InventoryItem> allItems = itemRepository.findByTenantIdAndDeletedAtIsNullOrderByNameAsc(tenantId);

        long totalProducts = allItems.size();
        BigDecimal totalStockQuantity = BigDecimal.ZERO;
        long lowStockCount = 0;
        long outOfStockCount = 0;
        BigDecimal warehouseValuation = BigDecimal.ZERO;

        for (InventoryItem item : allItems) {
            BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
            totalStockQuantity = totalStockQuantity.add(qty);

            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                outOfStockCount++;
            } else if (item.getMinQuantity() != null && qty.compareTo(item.getMinQuantity()) <= 0) {
                lowStockCount++;
            }

            if (item.getCostPrice() != null && qty.compareTo(BigDecimal.ZERO) > 0) {
                warehouseValuation = warehouseValuation.add(qty.multiply(item.getCostPrice()));
            }
        }

        // Bugungi harakatlar statistikasi (Asia/Tashkent)
        ZoneId zone = ZoneId.of("Asia/Tashkent");
        Instant startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant();
        Instant endOfDay = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant();

        List<InventoryTransaction> todayTxs = txRepository.findByTenantAndDateRange(tenantId, startOfDay, endOfDay);

        BigDecimal todayIncomingAmount = BigDecimal.ZERO;
        BigDecimal todayOutgoingAmount = BigDecimal.ZERO;
        BigDecimal todaySalesConsumptionAmount = BigDecimal.ZERO;

        for (InventoryTransaction tx : todayTxs) {
            BigDecimal amount = tx.getTotalCost() != null ? tx.getTotalCost() :
                    (tx.getUnitCost() != null ? tx.getUnitCost().multiply(tx.getQuantity().abs()) : BigDecimal.ZERO);

            if (tx.getType() == InventoryTransaction.TransactionType.PURCHASE || tx.getType() == InventoryTransaction.TransactionType.IN) {
                todayIncomingAmount = todayIncomingAmount.add(amount);
            } else if (tx.getType() == InventoryTransaction.TransactionType.OUT || tx.getType() == InventoryTransaction.TransactionType.WASTE) {
                todayOutgoingAmount = todayOutgoingAmount.add(amount);
            } else if (tx.getType() == InventoryTransaction.TransactionType.SALE) {
                todaySalesConsumptionAmount = todaySalesConsumptionAmount.add(amount);
            }
        }

        // Oxirgi yakunlangan inventarizatsiya farqi
        List<InventoryAudit> audits = auditRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        BigDecimal recentDiscrepancyAmount = audits.stream()
                .filter(a -> a.getStatus() == InventoryAudit.AuditStatus.COMPLETED && a.getTotalDifferenceCost() != null)
                .findFirst()
                .map(InventoryAudit::getTotalDifferenceCost)
                .orElse(BigDecimal.ZERO);

        return InventoryDto.DashboardStats.builder()
                .totalProducts(totalProducts)
                .totalStockQuantity(totalStockQuantity)
                .lowStockCount(lowStockCount)
                .outOfStockCount(outOfStockCount)
                .todayIncomingAmount(todayIncomingAmount)
                .todayOutgoingAmount(todayOutgoingAmount)
                .todaySalesConsumptionAmount(todaySalesConsumptionAmount)
                .warehouseValuation(warehouseValuation)
                .recentDiscrepancyAmount(recentDiscrepancyAmount)
                .build();
    }

    // ==========================================
    // 2. ITEMS CRUD & FILTERING
    // ==========================================
    @Transactional(readOnly = true)
    public List<InventoryDto.Response> getItemsFiltered(UUID tenantId, UUID warehouseId, String category, Boolean lowStock) {
        return itemRepository.findFiltered(tenantId, warehouseId, category, lowStock)
                .stream().map(this::toItemResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventoryDto.Response> getAllItems(UUID tenantId) {
        return itemRepository.findByTenantIdAndDeletedAtIsNullOrderByNameAsc(tenantId)
                .stream().map(this::toItemResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventoryDto.Response> getLowStockItems(UUID tenantId) {
        return itemRepository.findLowStockItems(tenantId)
                .stream().map(this::toItemResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InventoryDto.Response getItem(UUID tenantId, UUID itemId) {
        InventoryItem item = itemRepository.findByIdAndTenantIdAndDeletedAtIsNull(itemId, tenantId)
                .orElseThrow(() -> PosException.notFound("Item not found: " + itemId));
        return toItemResponse(item);
    }

    @Transactional
    public InventoryDto.Response createItem(UUID tenantId, InventoryDto.CreateRequest req) {
        if (req.getSku() != null && !req.getSku().isBlank() &&
                itemRepository.existsByTenantIdAndSkuAndDeletedAtIsNull(tenantId, req.getSku())) {
            throw PosException.badRequest("SKU already exists: " + req.getSku());
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> PosException.notFound("Tenant not found"));

        Warehouse warehouse = null;
        if (req.getWarehouseId() != null) {
            warehouse = warehouseRepository.findByIdAndTenantIdAndDeletedAtIsNull(req.getWarehouseId(), tenantId)
                    .orElse(null);
        }
        if (warehouse == null) {
            warehouse = warehouseRepository.findByTenantIdAndIsDefaultTrueAndDeletedAtIsNull(tenantId)
                    .orElse(null);
        }

        InventoryItem item = new InventoryItem();
        item.setTenant(tenant);
        item.setName(req.getName());
        item.setSku(req.getSku());
        item.setUnit(req.getUnit() != null ? req.getUnit() : "dona");
        item.setQuantity(req.getQuantity() != null ? req.getQuantity() : BigDecimal.ZERO);
        item.setMinQuantity(req.getMinQuantity() != null ? req.getMinQuantity() : BigDecimal.ZERO);
        item.setMaxQuantity(req.getMaxQuantity());
        item.setCostPrice(req.getCostPrice());
        item.setSellingPrice(req.getSellingPrice());
        item.setCategory(req.getCategory());
        item.setNotes(req.getNotes());
        item.setWarehouse(warehouse);

        InventoryItem saved = itemRepository.save(item);

        // Boshlang'ich qoldiq bo'lsa, transaction yaratish
        if (saved.getQuantity() != null && saved.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            InventoryTransaction tx = new InventoryTransaction();
            tx.setTenant(tenant);
            tx.setItem(saved);
            tx.setWarehouse(warehouse);
            tx.setType(InventoryTransaction.TransactionType.IN);
            tx.setQuantity(saved.getQuantity());
            tx.setQuantityBefore(BigDecimal.ZERO);
            tx.setQuantityAfter(saved.getQuantity());
            tx.setUnitCost(saved.getCostPrice());
            tx.setTotalCost(saved.getCostPrice() != null ? saved.getCostPrice().multiply(saved.getQuantity()) : BigDecimal.ZERO);
            tx.setNotes("Boshlang'ich qoldiq kiritildi");
            txRepository.save(tx);
        }

        return toItemResponse(saved);
    }

    @Transactional
    public InventoryDto.Response updateItem(UUID tenantId, UUID itemId, InventoryDto.UpdateRequest req) {
        InventoryItem item = itemRepository.findByIdAndTenantIdAndDeletedAtIsNull(itemId, tenantId)
                .orElseThrow(() -> PosException.notFound("Item not found: " + itemId));

        item.setName(req.getName());
        item.setSku(req.getSku());
        if (req.getUnit() != null) item.setUnit(req.getUnit());
        item.setMinQuantity(req.getMinQuantity());
        item.setMaxQuantity(req.getMaxQuantity());
        item.setCostPrice(req.getCostPrice());
        item.setSellingPrice(req.getSellingPrice());
        item.setCategory(req.getCategory());
        item.setNotes(req.getNotes());

        if (req.getWarehouseId() != null) {
            Warehouse wh = warehouseRepository.findByIdAndTenantIdAndDeletedAtIsNull(req.getWarehouseId(), tenantId)
                    .orElse(null);
            if (wh != null) item.setWarehouse(wh);
        }

        item.setUpdatedAt(Instant.now());
        return toItemResponse(itemRepository.save(item));
    }

    @Transactional
    public void deleteItem(UUID tenantId, UUID itemId) {
        InventoryItem item = itemRepository.findByIdAndTenantIdAndDeletedAtIsNull(itemId, tenantId)
                .orElseThrow(() -> PosException.notFound("Item not found: " + itemId));
        item.setDeletedAt(Instant.now());
        itemRepository.save(item);
    }

    // ==========================================
    // 3. ADJUSTMENT & OUTBOUND (CHIQIM)
    // ==========================================
    @Transactional
    public InventoryDto.Response adjustStock(UUID tenantId, UUID itemId, InventoryDto.AdjustRequest req, UUID userId) {
        InventoryItem item = itemRepository.findByIdAndTenantIdAndDeletedAtIsNull(itemId, tenantId)
                .orElseThrow(() -> PosException.notFound("Item not found: " + itemId));

        BigDecimal before = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
        BigDecimal after = before.add(req.getQuantity());

        item.setQuantity(after);
        item.setUpdatedAt(Instant.now());
        itemRepository.save(item);

        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        InventoryTransaction tx = new InventoryTransaction();
        tx.setTenant(item.getTenant());
        tx.setItem(item);
        tx.setWarehouse(item.getWarehouse());
        tx.setType(InventoryTransaction.TransactionType.valueOf(req.getType() != null ? req.getType().toUpperCase() : "ADJUSTMENT"));
        tx.setQuantity(req.getQuantity());
        tx.setQuantityBefore(before);
        tx.setQuantityAfter(after);
        tx.setUnitCost(req.getUnitCost() != null ? req.getUnitCost() : item.getCostPrice());
        tx.setTotalCost(tx.getUnitCost() != null ? tx.getUnitCost().multiply(req.getQuantity().abs()) : BigDecimal.ZERO);
        tx.setNotes(req.getNotes());
        tx.setUser(user);
        txRepository.save(tx);

        log.info("Stock adjusted: itemId={} before={} after={} type={}", itemId, before, after, req.getType());
        return toItemResponse(item);
    }

    @Transactional
    public InventoryDto.Response recordOutbound(UUID tenantId, InventoryDto.OutboundRequest req, UUID userId) {
        InventoryItem item = itemRepository.findByIdAndTenantIdAndDeletedAtIsNull(req.getItemId(), tenantId)
                .orElseThrow(() -> PosException.notFound("Item not found: " + req.getItemId()));

        BigDecimal qty = req.getQuantity().abs();
        BigDecimal before = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
        BigDecimal after = before.subtract(qty);

        item.setQuantity(after);
        item.setUpdatedAt(Instant.now());
        itemRepository.save(item);

        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        Warehouse wh = req.getWarehouseId() != null
                ? warehouseRepository.findByIdAndTenantIdAndDeletedAtIsNull(req.getWarehouseId(), tenantId).orElse(item.getWarehouse())
                : item.getWarehouse();

        InventoryTransaction.TransactionType type = "WASTE".equalsIgnoreCase(req.getType())
                ? InventoryTransaction.TransactionType.WASTE
                : InventoryTransaction.TransactionType.OUT;

        InventoryTransaction tx = new InventoryTransaction();
        tx.setTenant(item.getTenant());
        tx.setItem(item);
        tx.setWarehouse(wh);
        tx.setType(type);
        tx.setQuantity(qty.negate());
        tx.setQuantityBefore(before);
        tx.setQuantityAfter(after);
        tx.setUnitCost(item.getCostPrice());
        tx.setTotalCost(item.getCostPrice() != null ? item.getCostPrice().multiply(qty) : BigDecimal.ZERO);
        tx.setNotes((req.getReason() != null ? req.getReason() : "Chiqim") + (req.getNotes() != null ? " - " + req.getNotes() : ""));
        tx.setUser(user);
        txRepository.save(tx);

        log.info("Outbound recorded: itemId={} qty={} reason={}", req.getItemId(), qty, req.getReason());
        return toItemResponse(item);
    }

    // ==========================================
    // 4. LEDGER / MOVEMENTS
    // ==========================================
    @Transactional(readOnly = true)
    public List<InventoryDto.TransactionResponse> getTransactions(UUID tenantId, UUID itemId, String typeStr, UUID warehouseId, Instant from, Instant to, int page, int size) {
        InventoryTransaction.TransactionType type = null;
        if (typeStr != null && !typeStr.isBlank() && !"ALL".equalsIgnoreCase(typeStr)) {
            try {
                type = InventoryTransaction.TransactionType.valueOf(typeStr.toUpperCase());
            } catch (Exception ignored) {}
        }

        int pageSize = size > 0 ? size : 50;
        int pageIndex = Math.max(page, 0);

        return txRepository.findFiltered(tenantId, itemId, type, warehouseId, from, to, PageRequest.of(pageIndex, pageSize))
                .stream().map(this::toTxResponse).collect(Collectors.toList());
    }

    // ==========================================
    // 5. KIRIM / PURCHASES
    // ==========================================
    @Transactional
    public InventoryDto.PurchaseResponse createPurchase(UUID tenantId, InventoryDto.PurchaseCreateRequest req, UUID userId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> PosException.notFound("Tenant not found"));

        Supplier supplier = supplierRepository.findByIdAndTenantIdAndDeletedAtIsNull(req.getSupplierId(), tenantId)
                .orElseThrow(() -> PosException.notFound("Supplier not found: " + req.getSupplierId()));

        Warehouse warehouse = warehouseRepository.findByIdAndTenantIdAndDeletedAtIsNull(req.getWarehouseId(), tenantId)
                .orElse(warehouseRepository.findByTenantIdAndIsDefaultTrueAndDeletedAtIsNull(tenantId).orElse(null));

        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        Purchase purchase = new Purchase();
        purchase.setTenant(tenant);
        purchase.setSupplier(supplier);
        purchase.setWarehouse(warehouse);
        purchase.setUser(user);
        purchase.setInvoiceNumber(req.getInvoiceNumber());
        purchase.setNotes(req.getNotes());
        purchase.setStatus(Purchase.PurchaseStatus.CONFIRMED);

        LocalDate pDate = req.getPurchaseDate() != null ? req.getPurchaseDate() : LocalDate.now();
        purchase.setPurchaseDate(pDate.atStartOfDay(ZoneId.of("Asia/Tashkent")).toInstant());

        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String pNumber = "PUR-" + dateStr + "-" + String.format("%04d", (int)(Math.random() * 9000 + 1000));
        purchase.setPurchaseNumber(pNumber);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<PurchaseItem> purchaseItems = new ArrayList<>();

        for (InventoryDto.PurchaseItemRequest itemReq : req.getItems()) {
            InventoryItem item = itemRepository.findByIdAndTenantIdAndDeletedAtIsNull(itemReq.getItemId(), tenantId)
                    .orElseThrow(() -> PosException.notFound("Inventory item not found: " + itemReq.getItemId()));

            BigDecimal qty = itemReq.getQuantity();
            BigDecimal unitCost = itemReq.getUnitCost() != null ? itemReq.getUnitCost() :
                    (item.getCostPrice() != null ? item.getCostPrice() : BigDecimal.ZERO);
            BigDecimal itemTotal = unitCost.multiply(qty);
            totalAmount = totalAmount.add(itemTotal);

            PurchaseItem pItem = new PurchaseItem();
            pItem.setPurchase(purchase);
            pItem.setInventoryItem(item);
            pItem.setItemName(item.getName());
            pItem.setUnit(item.getUnit());
            pItem.setQuantity(qty);
            pItem.setUnitPrice(unitCost);
            pItem.setTotal(itemTotal);
            purchaseItems.add(pItem);

            // Ombor qoldig'ini oshirish
            BigDecimal before = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
            BigDecimal after = before.add(qty);
            item.setQuantity(after);
            item.setCostPrice(unitCost);
            item.setUpdatedAt(Instant.now());
            itemRepository.save(item);

            // Movement yozish
            InventoryTransaction tx = new InventoryTransaction();
            tx.setTenant(tenant);
            tx.setItem(item);
            tx.setWarehouse(warehouse);
            tx.setType(InventoryTransaction.TransactionType.PURCHASE);
            tx.setQuantity(qty);
            tx.setQuantityBefore(before);
            tx.setQuantityAfter(after);
            tx.setUnitCost(unitCost);
            tx.setTotalCost(itemTotal);
            tx.setReferenceType("PURCHASE");
            tx.setReferenceId(purchase.getId());
            tx.setReferenceNumber(pNumber);
            tx.setNotes("Kirim: " + supplier.getName() + " (" + (req.getInvoiceNumber() != null ? req.getInvoiceNumber() : "") + ")");
            tx.setUser(user);
            txRepository.save(tx);
        }

        BigDecimal paid = req.getPaidAmount() != null ? req.getPaidAmount() : BigDecimal.ZERO;
        purchase.setSubtotal(totalAmount);
        purchase.setTotal(totalAmount);
        purchase.setPaidAmount(paid);
        BigDecimal debt = totalAmount.subtract(paid).max(BigDecimal.ZERO);
        purchase.setDebtAmount(debt);
        purchase.setItems(purchaseItems);

        Purchase savedPurchase = purchaseRepository.save(purchase);

        // Supplier debt yangilash
        supplier.setTotalDebt((supplier.getTotalDebt() != null ? supplier.getTotalDebt() : BigDecimal.ZERO).add(debt));
        supplierRepository.save(supplier);

        return toPurchaseResponse(savedPurchase);
    }

    @Transactional(readOnly = true)
    public List<InventoryDto.PurchaseResponse> getAllPurchases(UUID tenantId) {
        return purchaseRepository.findByTenantIdAndDeletedAtIsNullOrderByPurchaseDateDesc(tenantId)
                .stream().map(this::toPurchaseResponse).collect(Collectors.toList());
    }

    // ==========================================
    // 6. INVENTARIZATSIYA / AUDITS
    // ==========================================
    @Transactional
    public InventoryDto.AuditResponse startAudit(UUID tenantId, InventoryDto.AuditStartRequest req, UUID userId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> PosException.notFound("Tenant not found"));

        Warehouse warehouse = req.getWarehouseId() != null
                ? warehouseRepository.findByIdAndTenantIdAndDeletedAtIsNull(req.getWarehouseId(), tenantId).orElse(null)
                : warehouseRepository.findByTenantIdAndIsDefaultTrueAndDeletedAtIsNull(tenantId).orElse(null);

        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        InventoryAudit audit = new InventoryAudit();
        audit.setTenant(tenant);
        audit.setWarehouse(warehouse);
        audit.setStatus(InventoryAudit.AuditStatus.IN_PROGRESS);
        audit.setNotes((req.getTitle() != null ? req.getTitle() + " - " : "") + (req.getNotes() != null ? req.getNotes() : ""));
        audit.setCreatedBy(user);
        audit.setCreatedAt(Instant.now());

        InventoryAudit savedAudit = auditRepository.save(audit);

        // Hozirgi mahsulotlarni tizim qoldig'i bilan avtomatik qo'shish
        List<InventoryItem> items = warehouse != null
                ? itemRepository.findByTenantIdAndWarehouseIdAndDeletedAtIsNullOrderByNameAsc(tenantId, warehouse.getId())
                : itemRepository.findByTenantIdAndDeletedAtIsNullOrderByNameAsc(tenantId);

        List<InventoryAuditItem> auditItems = new ArrayList<>();
        for (InventoryItem item : items) {
            InventoryAuditItem aItem = new InventoryAuditItem();
            aItem.setAudit(savedAudit);
            aItem.setInventoryItem(item);
            aItem.setSystemQuantity(item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO);
            aItem.setActualQuantity(item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO); // default current
            aItem.setDifferenceQuantity(BigDecimal.ZERO);
            aItem.setCostPrice(item.getCostPrice() != null ? item.getCostPrice() : BigDecimal.ZERO);
            aItem.setDifferenceCost(BigDecimal.ZERO);
            auditItems.add(aItem);
        }

        auditItemRepository.saveAll(auditItems);
        savedAudit.setTotalItemsCounted(auditItems.size());
        return toAuditResponse(savedAudit);
    }

    @Transactional
    public InventoryDto.AuditResponse submitAudit(UUID tenantId, UUID auditId, InventoryDto.AuditSubmitRequest req, UUID userId) {
        InventoryAudit audit = auditRepository.findByIdAndTenantId(auditId, tenantId)
                .orElseThrow(() -> PosException.notFound("Audit not found: " + auditId));

        if (audit.getStatus() == InventoryAudit.AuditStatus.COMPLETED) {
            throw PosException.badRequest("Audit is already completed");
        }

        User user = userId != null ? userRepository.findById(userId).orElse(null) : audit.getCreatedBy();
        Map<UUID, BigDecimal> actualMap = new HashMap<>();
        Map<UUID, String> notesMap = new HashMap<>();

        if (req.getItems() != null) {
            for (InventoryDto.AuditItemCountRequest c : req.getItems()) {
                actualMap.put(c.getItemId(), c.getActualQuantity());
                if (c.getNotes() != null) notesMap.put(c.getItemId(), c.getNotes());
            }
        }

        List<InventoryAuditItem> auditItems = auditItemRepository.findByAuditId(auditId);
        BigDecimal totalDiscrepancyCost = BigDecimal.ZERO;

        for (InventoryAuditItem aItem : auditItems) {
            UUID itemId = aItem.getInventoryItem().getId();
            BigDecimal actual = actualMap.getOrDefault(itemId, aItem.getSystemQuantity());
            BigDecimal sys = aItem.getSystemQuantity() != null ? aItem.getSystemQuantity() : BigDecimal.ZERO;
            BigDecimal diff = actual.subtract(sys);
            BigDecimal unitCost = aItem.getCostPrice() != null ? aItem.getCostPrice() : BigDecimal.ZERO;
            BigDecimal diffCost = diff.multiply(unitCost);

            aItem.setActualQuantity(actual);
            aItem.setDifferenceQuantity(diff);
            aItem.setDifferenceCost(diffCost);
            if (notesMap.containsKey(itemId)) {
                aItem.setNotes(notesMap.get(itemId));
            }
            auditItemRepository.save(aItem);

            totalDiscrepancyCost = totalDiscrepancyCost.add(diffCost);

            // Agar farq bo'lsa, tizim qoldig'ini yangilash va ADJUSTMENT transaction yozish
            if (diff.compareTo(BigDecimal.ZERO) != 0) {
                InventoryItem item = aItem.getInventoryItem();
                item.setQuantity(actual);
                item.setUpdatedAt(Instant.now());
                itemRepository.save(item);

                InventoryTransaction tx = new InventoryTransaction();
                tx.setTenant(audit.getTenant());
                tx.setItem(item);
                tx.setWarehouse(audit.getWarehouse());
                tx.setType(InventoryTransaction.TransactionType.ADJUSTMENT);
                tx.setQuantity(diff);
                tx.setQuantityBefore(sys);
                tx.setQuantityAfter(actual);
                tx.setUnitCost(unitCost);
                tx.setTotalCost(diffCost.abs());
                tx.setReferenceType("AUDIT");
                tx.setReferenceId(audit.getId());
                tx.setNotes("Inventarizatsiya tuzatishi: " + (diff.compareTo(BigDecimal.ZERO) > 0 ? "+" : "") + diff + " " + item.getUnit());
                tx.setUser(user);
                txRepository.save(tx);
            }
        }

        audit.setStatus(InventoryAudit.AuditStatus.COMPLETED);
        audit.setCompletedAt(Instant.now());
        audit.setCompletedBy(user);
        audit.setTotalDifferenceCost(totalDiscrepancyCost);
        if (req.getNotes() != null) audit.setNotes(req.getNotes());

        return toAuditResponse(auditRepository.save(audit));
    }

    @Transactional(readOnly = true)
    public List<InventoryDto.AuditResponse> getAllAudits(UUID tenantId) {
        return auditRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream().map(this::toAuditResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InventoryDto.AuditResponse getAuditDetails(UUID tenantId, UUID auditId) {
        InventoryAudit audit = auditRepository.findByIdAndTenantId(auditId, tenantId)
                .orElseThrow(() -> PosException.notFound("Audit not found: " + auditId));
        return toAuditResponse(audit);
    }

    // ==========================================
    // 7. RECIPES / RETSEPTLAR
    // ==========================================
    @Transactional(readOnly = true)
    public List<InventoryDto.ProductRecipeResponse> getAllRecipes(UUID tenantId) {
        List<Product> products = productRepository.findByTenantIdAndDeletedAtIsNullOrderBySortOrderAscNameAsc(tenantId);
        List<InventoryDto.ProductRecipeResponse> result = new ArrayList<>();

        for (Product product : products) {
            List<ProductIngredient> ingredients = productIngredientRepository.findByProductId(product.getId());
            BigDecimal totalCost = BigDecimal.ZERO;
            List<InventoryDto.RecipeItemResponse> itemDtos = new ArrayList<>();

            for (ProductIngredient ing : ingredients) {
                InventoryItem item = ing.getInventoryItem();
                BigDecimal cost = item != null && item.getCostPrice() != null
                        ? item.getCostPrice().multiply(ing.getQuantity())
                        : BigDecimal.ZERO;
                totalCost = totalCost.add(cost);

                itemDtos.add(InventoryDto.RecipeItemResponse.builder()
                        .id(ing.getId())
                        .inventoryItemId(item != null ? item.getId() : null)
                        .inventoryItemName(item != null ? item.getName() : "Unknown")
                        .quantity(ing.getQuantity())
                        .unit(ing.getUnit())
                        .costPrice(item != null ? item.getCostPrice() : BigDecimal.ZERO)
                        .totalCost(cost)
                        .build());
            }

            result.add(InventoryDto.ProductRecipeResponse.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .categoryName(product.getCategory() != null ? product.getCategory().getName() : "")
                    .price(product.getSalePrice())
                    .recipeItems(itemDtos)
                    .totalCostPrice(totalCost)
                    .build());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public InventoryDto.ProductRecipeResponse getProductRecipe(UUID tenantId, UUID productId) {
        Product product = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(productId, tenantId)
                .orElseThrow(() -> PosException.notFound("Product not found: " + productId));

        List<ProductIngredient> ingredients = productIngredientRepository.findByProductId(productId);
        BigDecimal totalCost = BigDecimal.ZERO;
        List<InventoryDto.RecipeItemResponse> itemDtos = new ArrayList<>();

        for (ProductIngredient ing : ingredients) {
            InventoryItem item = ing.getInventoryItem();
            BigDecimal cost = item != null && item.getCostPrice() != null
                    ? item.getCostPrice().multiply(ing.getQuantity())
                    : BigDecimal.ZERO;
            totalCost = totalCost.add(cost);

            itemDtos.add(InventoryDto.RecipeItemResponse.builder()
                    .id(ing.getId())
                    .inventoryItemId(item != null ? item.getId() : null)
                    .inventoryItemName(item != null ? item.getName() : "Unknown")
                    .quantity(ing.getQuantity())
                    .unit(ing.getUnit())
                    .costPrice(item != null ? item.getCostPrice() : BigDecimal.ZERO)
                    .totalCost(cost)
                    .build());
        }

        return InventoryDto.ProductRecipeResponse.builder()
                .productId(product.getId())
                .productName(product.getName())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : "")
                .price(product.getSalePrice())
                .recipeItems(itemDtos)
                .totalCostPrice(totalCost)
                .build();
    }

    @Transactional
    public InventoryDto.ProductRecipeResponse saveProductRecipe(UUID tenantId, InventoryDto.SaveRecipeRequest req) {
        Product product = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(req.getProductId(), tenantId)
                .orElseThrow(() -> PosException.notFound("Product not found: " + req.getProductId()));

        // Oldingi ingredientlarni tozalash
        List<ProductIngredient> existing = productIngredientRepository.findByProductId(product.getId());
        if (!existing.isEmpty()) {
            productIngredientRepository.deleteAll(existing);
            productIngredientRepository.flush();
        }

        if (req.getItems() != null && !req.getItems().isEmpty()) {
            List<ProductIngredient> newIngredients = new ArrayList<>();
            for (InventoryDto.RecipeItemRequest itemReq : req.getItems()) {
                InventoryItem item = itemRepository.findByIdAndTenantIdAndDeletedAtIsNull(itemReq.getInventoryItemId(), tenantId)
                        .orElseThrow(() -> PosException.notFound("Inventory item not found: " + itemReq.getInventoryItemId()));

                ProductIngredient ing = new ProductIngredient();
                ing.setProduct(product);
                ing.setInventoryItem(item);
                ing.setQuantity(itemReq.getQuantity());
                ing.setUnit(itemReq.getUnit() != null ? itemReq.getUnit() : item.getUnit());
                newIngredients.add(ing);
            }
            productIngredientRepository.saveAll(newIngredients);
        }

        return getProductRecipe(tenantId, product.getId());
    }

    // ==========================================
    // 8. DEDUCT STOCK FOR PAID ORDER (ATOMIC & IDEMPOTENT)
    // ==========================================
    @Transactional
    public void deductStockForPaidOrder(Order order) {
        if (order == null || order.getId() == null) return;

        // Idempotency: Agar ushbu order uchun SALE transaction allaqachon mavjud bo'lsa, duplicate deduction qilmaslik
        boolean alreadyDeducted = txRepository.existsByReferenceTypeAndReferenceIdAndType(
                "ORDER", order.getId(), InventoryTransaction.TransactionType.SALE);
        if (alreadyDeducted) {
            log.info("Stock deduction already processed for order: {}", order.getId());
            return;
        }

        if (order.getItems() == null || order.getItems().isEmpty()) return;

        for (OrderItem orderItem : order.getItems()) {
            if (orderItem.isVoided()) continue;
            if (orderItem.getProduct() == null) continue;

            BigDecimal activeQty = orderItem.getQuantity() != null ? orderItem.getQuantity() : BigDecimal.ZERO;
            if (orderItem.getCancelledQuantity() != null) {
                activeQty = activeQty.subtract(orderItem.getCancelledQuantity());
            }
            if (activeQty.compareTo(BigDecimal.ZERO) <= 0) continue;

            List<ProductIngredient> ingredients = productIngredientRepository.findByProductId(orderItem.getProduct().getId());
            for (ProductIngredient ing : ingredients) {
                InventoryItem invItem = ing.getInventoryItem();
                if (invItem == null) continue;

                BigDecimal reqQty = ing.getQuantity().multiply(activeQty);
                BigDecimal before = invItem.getQuantity() != null ? invItem.getQuantity() : BigDecimal.ZERO;
                BigDecimal after = before.subtract(reqQty);

                invItem.setQuantity(after);
                invItem.setUpdatedAt(Instant.now());
                itemRepository.save(invItem);

                InventoryTransaction tx = new InventoryTransaction();
                tx.setTenant(order.getTenant());
                tx.setItem(invItem);
                tx.setWarehouse(invItem.getWarehouse());
                tx.setType(InventoryTransaction.TransactionType.SALE);
                tx.setQuantity(reqQty.negate());
                tx.setQuantityBefore(before);
                tx.setQuantityAfter(after);
                tx.setUnitCost(invItem.getCostPrice());
                tx.setTotalCost(invItem.getCostPrice() != null ? invItem.getCostPrice().multiply(reqQty) : BigDecimal.ZERO);
                tx.setReferenceType("ORDER");
                tx.setReferenceId(order.getId());
                tx.setReferenceNumber(order.getOrderNumber());
                tx.setNotes("Sotuv: " + orderItem.getProductName() + " x" + activeQty + " (" + invItem.getName() + " sarfi)");
                txRepository.save(tx);

                log.info("Recipe stock deducted: item={} reqQty={} after={}", invItem.getName(), reqQty, after);
            }
        }
    }

    // ==========================================
    // 9. WAREHOUSES & SUPPLIERS
    // ==========================================
    @Transactional(readOnly = true)
    public List<InventoryDto.WarehouseResponse> getAllWarehouses(UUID tenantId) {
        return warehouseRepository.findByTenantIdAndDeletedAtIsNullOrderByNameAsc(tenantId)
                .stream().map(this::toWarehouseResponse).collect(Collectors.toList());
    }

    @Transactional
    public InventoryDto.WarehouseResponse createWarehouse(UUID tenantId, InventoryDto.WarehouseCreateRequest req) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> PosException.notFound("Tenant not found"));

        Warehouse wh = new Warehouse();
        wh.setTenant(tenant);
        wh.setName(req.getName());
        wh.setDescription(req.getDescription());
        wh.setActive(true);

        return toWarehouseResponse(warehouseRepository.save(wh));
    }

    @Transactional(readOnly = true)
    public List<InventoryDto.SupplierResponse> getAllSuppliers(UUID tenantId) {
        return supplierRepository.findByTenantIdAndDeletedAtIsNullOrderByNameAsc(tenantId)
                .stream().map(this::toSupplierResponse).collect(Collectors.toList());
    }

    @Transactional
    public InventoryDto.SupplierResponse createSupplier(UUID tenantId, InventoryDto.SupplierCreateRequest req) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> PosException.notFound("Tenant not found"));

        Supplier s = new Supplier();
        s.setTenant(tenant);
        s.setName(req.getName());
        s.setContactPerson(req.getContactPerson());
        s.setPhone(req.getPhone());
        s.setEmail(req.getEmail());
        s.setAddress(req.getAddress());
        s.setActive(true);
        s.setTotalDebt(BigDecimal.ZERO);

        return toSupplierResponse(supplierRepository.save(s));
    }

    // ==========================================
    // MAPPERS
    // ==========================================
    private InventoryDto.Response toItemResponse(InventoryItem item) {
        BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
        BigDecimal min = item.getMinQuantity() != null ? item.getMinQuantity() : BigDecimal.ZERO;

        return InventoryDto.Response.builder()
                .id(item.getId())
                .name(item.getName())
                .sku(item.getSku())
                .unit(item.getUnit())
                .quantity(qty)
                .minQuantity(min)
                .maxQuantity(item.getMaxQuantity())
                .costPrice(item.getCostPrice())
                .sellingPrice(item.getSellingPrice())
                .category(item.getCategory())
                .lowStock(qty.compareTo(BigDecimal.ZERO) > 0 && qty.compareTo(min) <= 0)
                .outOfStock(qty.compareTo(BigDecimal.ZERO) <= 0)
                .warehouseId(item.getWarehouse() != null ? item.getWarehouse().getId() : null)
                .warehouseName(item.getWarehouse() != null ? item.getWarehouse().getName() : "Asosiy ombor")
                .active(item.isActive())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private InventoryDto.TransactionResponse toTxResponse(InventoryTransaction tx) {
        return InventoryDto.TransactionResponse.builder()
                .id(tx.getId())
                .itemId(tx.getItem() != null ? tx.getItem().getId() : null)
                .itemName(tx.getItem() != null ? tx.getItem().getName() : "N/A")
                .unit(tx.getItem() != null ? tx.getItem().getUnit() : "")
                .warehouseId(tx.getWarehouse() != null ? tx.getWarehouse().getId() : null)
                .warehouseName(tx.getWarehouse() != null ? tx.getWarehouse().getName() : null)
                .type(tx.getType().name())
                .quantity(tx.getQuantity())
                .quantityBefore(tx.getQuantityBefore())
                .quantityAfter(tx.getQuantityAfter())
                .unitCost(tx.getUnitCost())
                .totalCost(tx.getTotalCost())
                .referenceType(tx.getReferenceType())
                .referenceId(tx.getReferenceId())
                .referenceNumber(tx.getReferenceNumber())
                .notes(tx.getNotes())
                .userName(tx.getUser() != null ? tx.getUser().getFirstName() + " " + (tx.getUser().getLastName() != null ? tx.getUser().getLastName() : "") : null)
                .createdAt(tx.getCreatedAt())
                .build();
    }

    private InventoryDto.PurchaseResponse toPurchaseResponse(Purchase p) {
        List<InventoryDto.PurchaseItemResponse> itemDtos = (p.getItems() != null ? p.getItems() : Collections.<PurchaseItem>emptyList())
                .stream().map(i -> InventoryDto.PurchaseItemResponse.builder()
                        .id(i.getId())
                        .itemId(i.getInventoryItem() != null ? i.getInventoryItem().getId() : null)
                        .itemName(i.getInventoryItem() != null ? i.getInventoryItem().getName() : "N/A")
                        .itemSku(i.getInventoryItem() != null ? i.getInventoryItem().getSku() : null)
                        .unit(i.getInventoryItem() != null ? i.getInventoryItem().getUnit() : null)
                        .quantity(i.getQuantity())
                        .unitCost(i.getUnitPrice())
                        .totalCost(i.getTotal())
                        .notes("")
                        .build()).collect(Collectors.toList());

        return InventoryDto.PurchaseResponse.builder()
                .id(p.getId())
                .purchaseNumber(p.getPurchaseNumber())
                .invoiceNumber(p.getInvoiceNumber())
                .supplierId(p.getSupplier() != null ? p.getSupplier().getId() : null)
                .supplierName(p.getSupplier() != null ? p.getSupplier().getName() : "N/A")
                .warehouseId(p.getWarehouse() != null ? p.getWarehouse().getId() : null)
                .warehouseName(p.getWarehouse() != null ? p.getWarehouse().getName() : null)
                .purchaseDate(p.getPurchaseDate() != null ? LocalDate.ofInstant(p.getPurchaseDate(), ZoneId.of("Asia/Tashkent")) : null)
                .status(p.getStatus() != null ? p.getStatus().name() : null)
                .totalAmount(p.getTotal())
                .paidAmount(p.getPaidAmount())
                .balanceDue(p.getDebtAmount())
                .notes(p.getNotes())
                .items(itemDtos)
                .createdAt(p.getCreatedAt())
                .build();
    }

    private InventoryDto.AuditResponse toAuditResponse(InventoryAudit a) {
        List<InventoryAuditItem> rawItems = a.getItems() != null ? a.getItems() : auditItemRepository.findByAuditId(a.getId());
        List<InventoryDto.AuditItemResponse> itemDtos = rawItems.stream().map(i -> InventoryDto.AuditItemResponse.builder()
                .id(i.getId())
                .itemId(i.getInventoryItem() != null ? i.getInventoryItem().getId() : null)
                .itemName(i.getInventoryItem() != null ? i.getInventoryItem().getName() : "N/A")
                .sku(i.getInventoryItem() != null ? i.getInventoryItem().getSku() : null)
                .unit(i.getInventoryItem() != null ? i.getInventoryItem().getUnit() : null)
                .systemQuantity(i.getSystemQuantity())
                .actualQuantity(i.getActualQuantity())
                .difference(i.getDifferenceQuantity())
                .unitCost(i.getCostPrice())
                .totalDifferenceCost(i.getDifferenceCost())
                .notes(i.getNotes())
                .build()).collect(Collectors.toList());

        return InventoryDto.AuditResponse.builder()
                .id(a.getId())
                .auditNumber("AUD-" + (a.getId() != null ? a.getId().toString().substring(0, 8) : ""))
                .warehouseId(a.getWarehouse() != null ? a.getWarehouse().getId() : null)
                .warehouseName(a.getWarehouse() != null ? a.getWarehouse().getName() : null)
                .title(a.getNotes() != null ? a.getNotes() : "Inventarizatsiya")
                .status(a.getStatus() != null ? a.getStatus().name() : null)
                .totalDiscrepancyCost(a.getTotalDifferenceCost())
                .conductedByName(a.getCreatedBy() != null ? a.getCreatedBy().getFirstName() + " " + (a.getCreatedBy().getLastName() != null ? a.getCreatedBy().getLastName() : "") : null)
                .startedAt(a.getCreatedAt())
                .completedAt(a.getCompletedAt())
                .notes(a.getNotes())
                .items(itemDtos)
                .build();
    }

    private InventoryDto.WarehouseResponse toWarehouseResponse(Warehouse w) {
        return InventoryDto.WarehouseResponse.builder()
                .id(w.getId())
                .name(w.getName())
                .description(w.getDescription())
                .address(w.getDescription())
                .active(w.isActive())
                .createdAt(w.getCreatedAt())
                .build();
    }

    private InventoryDto.SupplierResponse toSupplierResponse(Supplier s) {
        return InventoryDto.SupplierResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .contactPerson(s.getContactPerson())
                .phone(s.getPhone())
                .email(s.getEmail())
                .address(s.getAddress())
                .totalPurchases(BigDecimal.ZERO)
                .totalPaid(BigDecimal.ZERO)
                .balanceDue(s.getTotalDebt())
                .active(s.isActive())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
