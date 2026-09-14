package com.restaurantpos.delivery.service;

import com.restaurantpos.common.exception.PosException;
import com.restaurantpos.delivery.dto.DeliveryDashboardDto;
import com.restaurantpos.delivery.dto.DeliveryMappingDto;
import com.restaurantpos.delivery.dto.DeliveryOrderDto;
import com.restaurantpos.delivery.entity.*;
import com.restaurantpos.delivery.provider.DeliveryProvider;
import com.restaurantpos.delivery.provider.DeliveryProviderRegistry;
import com.restaurantpos.delivery.provider.model.ExternalDeliveryOrder;
import com.restaurantpos.delivery.provider.model.ExternalDeliveryOrderItem;
import com.restaurantpos.delivery.repository.DeliveryOrderItemRepository;
import com.restaurantpos.delivery.repository.DeliveryOrderRepository;
import com.restaurantpos.delivery.repository.DeliveryProviderRepository;
import com.restaurantpos.orders.entity.Order;
import com.restaurantpos.orders.entity.OrderItem;
import com.restaurantpos.orders.repository.OrderItemRepository;
import com.restaurantpos.orders.repository.OrderRepository;
import com.restaurantpos.orders.service.OrderService;
import com.restaurantpos.printers.service.PrintRoutingService;
import com.restaurantpos.products.entity.Product;
import com.restaurantpos.products.repository.ProductRepository;
import com.restaurantpos.tenants.entity.Tenant;
import com.restaurantpos.common.websocket.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryOrderService {

    private final DeliveryOrderRepository deliveryOrderRepository;
    private final DeliveryOrderItemRepository deliveryOrderItemRepository;
    private final DeliveryProviderRepository deliveryProviderRepository;
    private final DeliveryMappingService mappingService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final DeliveryProviderRegistry providerRegistry;
    private final DeliveryLogService logService;
    private final WebSocketNotificationService wsNotification;
    private final PrintRoutingService printRoutingService;

    @Transactional
    public DeliveryOrderEntity ingestExternalOrder(ExternalDeliveryOrder extOrder, DeliveryProviderEntity provider) {
        Tenant tenant = provider.getTenant();

        // 1. IDEMPOTENCY CHECK: Check if order with this externalOrderId already exists for this provider
        Optional<DeliveryOrderEntity> existingOpt = deliveryOrderRepository
                .findByProviderIdAndExternalOrderId(provider.getId(), extOrder.getExternalOrderId());

        if (existingOpt.isPresent()) {
            DeliveryOrderEntity existing = existingOpt.get();
            log.info("Idempotent Delivery Order detected: [Provider: {}, ExternalId: {}]. Existing POS Order ID: {}",
                    provider.getCode(), extOrder.getExternalOrderId(), existing.getPosOrder() != null ? existing.getPosOrder().getId() : "NONE");

            // Update courier or status if new info arrived
            if (extOrder.getCourier() != null) {
                existing.setCourierName(extOrder.getCourier().getName());
                existing.setCourierPhone(extOrder.getCourier().getPhone());
                existing.setCourierId(extOrder.getCourier().getCourierId());
                existing.setCourierVehicle(extOrder.getCourier().getVehicle());
                existing.setCourierStatus(extOrder.getCourier().getStatus());
                deliveryOrderRepository.save(existing);
            }
            return existing;
        }

        // Calculate commission based on provider settings
        BigDecimal subtotal = extOrder.getSubtotal() != null ? extOrder.getSubtotal() : BigDecimal.ZERO;
        BigDecimal deliveryFee = extOrder.getDeliveryFee() != null ? extOrder.getDeliveryFee() : BigDecimal.ZERO;
        BigDecimal commission = BigDecimal.ZERO;

        if (provider.getCommissionType() == DeliveryProviderEntity.CommissionType.PERCENTAGE) {
            commission = subtotal.multiply(provider.getCommissionValue()).divide(BigDecimal.valueOf(100));
        } else if (provider.getCommissionType() == DeliveryProviderEntity.CommissionType.FIXED) {
            commission = provider.getCommissionValue();
        }

        DeliveryOrderEntity deliveryOrder = new DeliveryOrderEntity();
        deliveryOrder.setTenant(tenant);
        deliveryOrder.setProvider(provider);
        deliveryOrder.setExternalOrderId(extOrder.getExternalOrderId());
        deliveryOrder.setCustomerName(extOrder.getCustomerName());
        deliveryOrder.setCustomerPhone(extOrder.getCustomerPhone());
        deliveryOrder.setDeliveryAddress(extOrder.getDeliveryAddress());
        deliveryOrder.setAddressApartment(extOrder.getAddressApartment());
        deliveryOrder.setAddressEntrance(extOrder.getAddressEntrance());
        deliveryOrder.setAddressFloor(extOrder.getAddressFloor());
        deliveryOrder.setAddressComment(extOrder.getAddressComment());
        deliveryOrder.setLatitude(extOrder.getLatitude());
        deliveryOrder.setLongitude(extOrder.getLongitude());
        deliveryOrder.setSubtotal(subtotal);
        deliveryOrder.setDeliveryFee(deliveryFee);
        deliveryOrder.setCommission(commission);
        deliveryOrder.setDiscount(extOrder.getDiscount() != null ? extOrder.getDiscount() : BigDecimal.ZERO);
        deliveryOrder.setTotal(extOrder.getTotal() != null ? extOrder.getTotal() : subtotal.add(deliveryFee));
        deliveryOrder.setPaymentType(extOrder.getPaymentType() != null ? extOrder.getPaymentType() : provider.getDefaultPaymentType());
        deliveryOrder.setPaymentStatus(extOrder.getPaymentStatus() != null ? extOrder.getPaymentStatus() : "PAID");
        deliveryOrder.setRawPayload(extOrder.getRawPayload());

        if (extOrder.getCourier() != null) {
            deliveryOrder.setCourierName(extOrder.getCourier().getName());
            deliveryOrder.setCourierPhone(extOrder.getCourier().getPhone());
            deliveryOrder.setCourierId(extOrder.getCourier().getCourierId());
            deliveryOrder.setCourierVehicle(extOrder.getCourier().getVehicle());
            deliveryOrder.setCourierStatus(extOrder.getCourier().getStatus());
        }

        // Save delivery order header first
        deliveryOrder = deliveryOrderRepository.save(deliveryOrder);

        // 2. PRODUCT MAPPING CHECK for each item
        boolean hasUnmappedItem = false;
        Map<DeliveryOrderItemEntity, Product> mappedProducts = new HashMap<>();

        if (extOrder.getItems() != null) {
            for (ExternalDeliveryOrderItem itemReq : extOrder.getItems()) {
                DeliveryOrderItemEntity itemEntity = new DeliveryOrderItemEntity();
                itemEntity.setDeliveryOrder(deliveryOrder);
                itemEntity.setExternalProductId(itemReq.getExternalProductId());
                itemEntity.setName(itemReq.getName());
                itemEntity.setQuantity(itemReq.getQuantity() != null ? itemReq.getQuantity() : BigDecimal.ONE);
                itemEntity.setUnitPrice(itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : BigDecimal.ZERO);
                itemEntity.setTotal(itemReq.getTotal() != null ? itemReq.getTotal() : itemEntity.getUnitPrice().multiply(itemEntity.getQuantity()));
                itemEntity.setNotes(itemReq.getNotes());

                Optional<Product> resolvedProduct = mappingService.resolveProduct(provider, itemReq.getExternalProductId(), itemReq.getName());
                if (resolvedProduct.isPresent()) {
                    itemEntity.setPosProduct(resolvedProduct.get());
                    itemEntity.setMappingStatus("MAPPED");
                    mappedProducts.put(itemEntity, resolvedProduct.get());
                } else {
                    itemEntity.setMappingStatus("UNMAPPED");
                    hasUnmappedItem = true;
                }
                deliveryOrderItemRepository.save(itemEntity);
                deliveryOrder.getItems().add(itemEntity);
            }
        }

        // 3. DECISION: If unmapped items exist -> MAPPING_REQUIRED
        if (hasUnmappedItem) {
            deliveryOrder.setStatus(DeliveryOrderEntity.DeliveryOrderStatus.MAPPING_REQUIRED);
            deliveryOrder.setErrorMessage("Ba'zi mahsulotlar POS mahsulotlariga bog'lanmagan (Mapping talab qilinadi)");
            deliveryOrderRepository.save(deliveryOrder);

            logService.logAction(tenant, provider, "ORDER_MAPPING_REQUIRED", extOrder.getExternalOrderId(),
                    DeliveryIntegrationLogEntity.LogStatus.WARNING, 0L, deliveryOrder.getErrorMessage(), "Buyurtma mapping kutmoqda");

            notifyDeliveryUpdate(tenant.getId(), deliveryOrder);
            return deliveryOrder;
        }

        // 4. ALL ITEMS MAPPED -> Create POS Order & Kitchen Routing
        Order posOrder = createPosOrderFromDelivery(deliveryOrder, provider, mappedProducts);
        deliveryOrder.setPosOrder(posOrder);

        if (provider.isAutoAccept()) {
            deliveryOrder.setStatus(DeliveryOrderEntity.DeliveryOrderStatus.ACCEPTED);
        } else {
            deliveryOrder.setStatus(DeliveryOrderEntity.DeliveryOrderStatus.NEW);
        }

        deliveryOrderRepository.save(deliveryOrder);

        logService.logAction(tenant, provider, "ORDER_INGESTED", extOrder.getExternalOrderId(),
                DeliveryIntegrationLogEntity.LogStatus.SUCCESS, 0L, null, "POS Buyurtma #" + posOrder.getOrderNumber() + " yaratildi");

        notifyDeliveryUpdate(tenant.getId(), deliveryOrder);
        return deliveryOrder;
    }

    private Order createPosOrderFromDelivery(DeliveryOrderEntity delOrder, DeliveryProviderEntity provider, Map<DeliveryOrderItemEntity, Product> mappedProducts) {
        Tenant tenant = delOrder.getTenant();

        Order posOrder = new Order();
        posOrder.setTenant(tenant);
        posOrder.setOrderNumber("DEL-" + provider.getCode().substring(0, Math.min(provider.getCode().length(), 4)) + "-" + delOrder.getExternalOrderId());
        posOrder.setOrderType(Order.OrderType.DELIVERY);
        posOrder.setStatus(provider.isAutoAccept() ? Order.OrderStatus.IN_PROGRESS : Order.OrderStatus.OPEN);
        posOrder.setDeliveryAddress(delOrder.getDeliveryAddress());
        posOrder.setDeliveryPhone(delOrder.getCustomerPhone());
        posOrder.setDeliveryFee(delOrder.getDeliveryFee());
        posOrder.setNotes("Yetkazib berish: " + provider.getName() + " | Mijoz: " + (delOrder.getCustomerName() != null ? delOrder.getCustomerName() : ""));
        posOrder.setKitchenNotes("🚚 DELIVERY ORDER #" + posOrder.getOrderNumber() + " (" + provider.getName() + ")");

        // Create OrderItems
        for (Map.Entry<DeliveryOrderItemEntity, Product> entry : mappedProducts.entrySet()) {
            DeliveryOrderItemEntity dItem = entry.getKey();
            Product product = entry.getValue();

            OrderItem posItem = new OrderItem();
            posItem.setOrder(posOrder);
            posItem.setProduct(product);
            posItem.setProductName(product.getName());
            posItem.setProductSku(product.getSku());
            posItem.setQuantity(dItem.getQuantity());
            posItem.setUnitPrice(dItem.getUnitPrice());
            posItem.setSubtotal(dItem.getTotal());
            posItem.setNotes(dItem.getNotes());
            posItem.setKitchenStatus(OrderItem.KitchenStatus.NEW);

            // Assign Kitchen from Product Category
            if (product.getCategory() != null && product.getCategory().getKitchen() != null) {
                posItem.setKitchen(product.getCategory().getKitchen());
            }

            posOrder.getItems().add(posItem);
        }

        posOrder.recalculateTotals();
        Order savedOrder = orderRepository.save(posOrder);

        // Kitchen Routing if auto-accepted
        if (provider.isAutoAccept()) {
            routeToKitchen(savedOrder, tenant, provider);
        }

        return savedOrder;
    }

    private void routeToKitchen(Order order, Tenant tenant, DeliveryProviderEntity provider) {
        List<OrderItem> kitchenItems = order.getItems().stream()
                .filter(i -> !i.isVoided() && i.getKitchen() != null)
                .collect(Collectors.toList());

        if (kitchenItems.isEmpty()) return;

        Instant now = Instant.now();
        order.setSentToKitchenAt(now);

        for (OrderItem item : kitchenItems) {
            item.setKitchenStatus(OrderItem.KitchenStatus.SENT_TO_KITCHEN);
            item.setSentToKitchenAt(now);
            item.setSentQuantity(item.getQuantity());
        }
        orderRepository.save(order);

        // Hardware Print Routing if enabled on provider
        if (provider.isAutoPrintKitchen()) {
            try {
                printRoutingService.routeAndPrintKitchenTickets(order, kitchenItems);
            } catch (Exception e) {
                log.warn("Delivery order kitchen ticket print warning: {}", e.getMessage());
            }
        }

        // Notify KDS screens via WebSockets
        try {
            wsNotification.notifyNewOrder(tenant.getId(), null);
        } catch (Exception ignored) {}
    }

    @Transactional
    public DeliveryOrderDto.Response manualMapItemAndReprocess(UUID tenantId, UUID deliveryOrderId, String externalProductId, UUID posProductId) {
        DeliveryOrderEntity deliveryOrder = deliveryOrderRepository.findByIdAndTenantId(deliveryOrderId, tenantId)
                .orElseThrow(() -> PosException.notFound("Delivery buyurtma topilmadi: " + deliveryOrderId));

        Product posProduct = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(posProductId, tenantId)
                .orElseThrow(() -> PosException.notFound("POS mahsuloti topilmadi: " + posProductId));

        // Create / update product mapping
        DeliveryMappingDto.MapProductRequest mapReq = new DeliveryMappingDto.MapProductRequest(
                deliveryOrder.getProvider().getId(), externalProductId, null, posProductId);
        mappingService.mapProduct(tenantId, mapReq);

        // Update items in this delivery order
        List<DeliveryOrderItemEntity> items = deliveryOrderItemRepository.findByDeliveryOrderId(deliveryOrderId);
        boolean stillHasUnmapped = false;
        Map<DeliveryOrderItemEntity, Product> mappedProducts = new HashMap<>();

        for (DeliveryOrderItemEntity item : items) {
            if (externalProductId.equals(item.getExternalProductId())) {
                item.setPosProduct(posProduct);
                item.setMappingStatus("MAPPED");
                deliveryOrderItemRepository.save(item);
                mappedProducts.put(item, posProduct);
            } else if ("MAPPED".equals(item.getMappingStatus()) && item.getPosProduct() != null) {
                mappedProducts.put(item, item.getPosProduct());
            } else {
                Optional<Product> p = mappingService.resolveProduct(deliveryOrder.getProvider(), item.getExternalProductId(), item.getName());
                if (p.isPresent()) {
                    item.setPosProduct(p.get());
                    item.setMappingStatus("MAPPED");
                    deliveryOrderItemRepository.save(item);
                    mappedProducts.put(item, p.get());
                } else {
                    stillHasUnmapped = true;
                }
            }
        }

        // If all items mapped now -> create POS Order & Route
        if (!stillHasUnmapped && deliveryOrder.getPosOrder() == null) {
            Order posOrder = createPosOrderFromDelivery(deliveryOrder, deliveryOrder.getProvider(), mappedProducts);
            deliveryOrder.setPosOrder(posOrder);
            deliveryOrder.setStatus(deliveryOrder.getProvider().isAutoAccept() ? DeliveryOrderEntity.DeliveryOrderStatus.ACCEPTED : DeliveryOrderEntity.DeliveryOrderStatus.NEW);
            deliveryOrder.setErrorMessage(null);
            deliveryOrderRepository.save(deliveryOrder);
            notifyDeliveryUpdate(tenantId, deliveryOrder);
        }

        return toResponse(deliveryOrder);
    }

    @Transactional
    public DeliveryOrderDto.Response acceptOrder(UUID tenantId, UUID deliveryOrderId, Integer prepMinutes) {
        DeliveryOrderEntity deliveryOrder = deliveryOrderRepository.findByIdAndTenantId(deliveryOrderId, tenantId)
                .orElseThrow(() -> PosException.notFound("Delivery buyurtma topilmadi: " + deliveryOrderId));

        DeliveryProvider provider = providerRegistry.getProvider(deliveryOrder.getProvider().getCode())
                .orElseThrow(() -> PosException.badRequest("Provayder adapteri topilmadi"));

        int prep = prepMinutes != null ? prepMinutes : 20;
        provider.acceptOrder(deliveryOrder.getProvider(), null, null, deliveryOrder.getExternalOrderId(), prep);

        deliveryOrder.setStatus(DeliveryOrderEntity.DeliveryOrderStatus.ACCEPTED);
        if (deliveryOrder.getPosOrder() != null) {
            routeToKitchen(deliveryOrder.getPosOrder(), deliveryOrder.getTenant(), deliveryOrder.getProvider());
        }
        deliveryOrderRepository.save(deliveryOrder);
        notifyDeliveryUpdate(tenantId, deliveryOrder);
        return toResponse(deliveryOrder);
    }

    @Transactional
    public DeliveryOrderDto.Response rejectOrder(UUID tenantId, UUID deliveryOrderId, String reason) {
        DeliveryOrderEntity deliveryOrder = deliveryOrderRepository.findByIdAndTenantId(deliveryOrderId, tenantId)
                .orElseThrow(() -> PosException.notFound("Delivery buyurtma topilmadi: " + deliveryOrderId));

        DeliveryProvider provider = providerRegistry.getProvider(deliveryOrder.getProvider().getCode())
                .orElseThrow(() -> PosException.badRequest("Provayder adapteri topilmadi"));

        provider.rejectOrder(deliveryOrder.getProvider(), null, null, deliveryOrder.getExternalOrderId(), reason);

        deliveryOrder.setStatus(DeliveryOrderEntity.DeliveryOrderStatus.REJECTED);
        deliveryOrder.setErrorMessage("Rad etildi: " + reason);
        deliveryOrderRepository.save(deliveryOrder);
        notifyDeliveryUpdate(tenantId, deliveryOrder);
        return toResponse(deliveryOrder);
    }

    @Transactional
    public DeliveryOrderDto.Response cancelOrder(UUID tenantId, UUID deliveryOrderId, String reason) {
        DeliveryOrderEntity deliveryOrder = deliveryOrderRepository.findByIdAndTenantId(deliveryOrderId, tenantId)
                .orElseThrow(() -> PosException.notFound("Delivery buyurtma topilmadi: " + deliveryOrderId));

        deliveryOrder.setStatus(DeliveryOrderEntity.DeliveryOrderStatus.CANCELLED);
        deliveryOrder.setErrorMessage("Bekor qilindi: " + reason);

        if (deliveryOrder.getPosOrder() != null) {
            deliveryOrder.getPosOrder().setStatus(Order.OrderStatus.CANCELLED);
            orderRepository.save(deliveryOrder.getPosOrder());
        }

        deliveryOrderRepository.save(deliveryOrder);
        notifyDeliveryUpdate(tenantId, deliveryOrder);
        return toResponse(deliveryOrder);
    }

    @Transactional(readOnly = true)
    public Page<DeliveryOrderDto.Response> getFilteredOrders(
            UUID tenantId, UUID providerId, String statusStr, String paymentType, String search, Pageable pageable) {

        DeliveryOrderEntity.DeliveryOrderStatus status = null;
        if (statusStr != null && !statusStr.isBlank() && !"ALL".equalsIgnoreCase(statusStr)) {
            try {
                status = DeliveryOrderEntity.DeliveryOrderStatus.valueOf(statusStr.toUpperCase());
            } catch (Exception ignored) {}
        }

        return deliveryOrderRepository.findFiltered(tenantId, providerId, status, paymentType, search, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DeliveryOrderDto.Response getOrder(UUID tenantId, UUID id) {
        DeliveryOrderEntity entity = deliveryOrderRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Delivery buyurtma topilmadi: " + id));
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public DeliveryDashboardDto.Summary getDashboardSummary(UUID tenantId) {
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.of("Asia/Tashkent")).toInstant();
        Instant endOfDay = startOfDay.plusSeconds(86400);

        List<DeliveryOrderEntity> todayOrders = deliveryOrderRepository.findByTenantIdAndCreatedAtBetween(tenantId, startOfDay, endOfDay);

        long activeCount = todayOrders.stream().filter(o ->
                o.getStatus() == DeliveryOrderEntity.DeliveryOrderStatus.NEW ||
                o.getStatus() == DeliveryOrderEntity.DeliveryOrderStatus.ACCEPTED ||
                o.getStatus() == DeliveryOrderEntity.DeliveryOrderStatus.PREPARING ||
                o.getStatus() == DeliveryOrderEntity.DeliveryOrderStatus.READY ||
                o.getStatus() == DeliveryOrderEntity.DeliveryOrderStatus.COURIER_ASSIGNED ||
                o.getStatus() == DeliveryOrderEntity.DeliveryOrderStatus.PICKED_UP ||
                o.getStatus() == DeliveryOrderEntity.DeliveryOrderStatus.DELIVERING
        ).count();

        long deliveredCount = todayOrders.stream().filter(o -> o.getStatus() == DeliveryOrderEntity.DeliveryOrderStatus.DELIVERED).count();
        long cancelledCount = todayOrders.stream().filter(o -> o.getStatus() == DeliveryOrderEntity.DeliveryOrderStatus.CANCELLED || o.getStatus() == DeliveryOrderEntity.DeliveryOrderStatus.REJECTED).count();
        long failedCount = todayOrders.stream().filter(o -> o.getStatus() == DeliveryOrderEntity.DeliveryOrderStatus.FAILED).count();
        long mappingRequiredCount = todayOrders.stream().filter(o -> o.getStatus() == DeliveryOrderEntity.DeliveryOrderStatus.MAPPING_REQUIRED).count();

        BigDecimal grossSales = todayOrders.stream()
                .filter(o -> o.getStatus() != DeliveryOrderEntity.DeliveryOrderStatus.CANCELLED && o.getStatus() != DeliveryOrderEntity.DeliveryOrderStatus.REJECTED)
                .map(DeliveryOrderEntity::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal deliveryFees = todayOrders.stream()
                .filter(o -> o.getStatus() != DeliveryOrderEntity.DeliveryOrderStatus.CANCELLED)
                .map(DeliveryOrderEntity::getDeliveryFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal commissions = todayOrders.stream()
                .filter(o -> o.getStatus() != DeliveryOrderEntity.DeliveryOrderStatus.CANCELLED)
                .map(DeliveryOrderEntity::getCommission)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netRevenue = grossSales.subtract(commissions);

        // Stats by provider
        List<DeliveryProviderEntity> providers = deliveryProviderRepository.findByTenantIdAndDeletedAtIsNull(tenantId);
        List<DeliveryDashboardDto.ProviderStat> providerStats = new ArrayList<>();

        for (DeliveryProviderEntity p : providers) {
            long pOrders = todayOrders.stream().filter(o -> o.getProvider().getId().equals(p.getId())).count();
            BigDecimal pSales = todayOrders.stream()
                    .filter(o -> o.getProvider().getId().equals(p.getId()) && o.getStatus() != DeliveryOrderEntity.DeliveryOrderStatus.CANCELLED)
                    .map(DeliveryOrderEntity::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            providerStats.add(DeliveryDashboardDto.ProviderStat.builder()
                    .providerCode(p.getCode())
                    .providerName(p.getName())
                    .status(p.getStatus().name())
                    .orderCount(pOrders)
                    .totalSales(pSales)
                    .build());
        }

        return DeliveryDashboardDto.Summary.builder()
                .todayOrdersCount(todayOrders.size())
                .activeDeliveryCount(activeCount)
                .deliveredCount(deliveredCount)
                .cancelledCount(cancelledCount)
                .failedCount(failedCount)
                .mappingRequiredCount(mappingRequiredCount)
                .grossSales(grossSales)
                .deliveryFeesTotal(deliveryFees)
                .commissionTotal(commissions)
                .netDeliveryRevenue(netRevenue)
                .providerStats(providerStats)
                .build();
    }

    private void notifyDeliveryUpdate(UUID tenantId, DeliveryOrderEntity order) {
        try {
            wsNotification.notifyDeliveryOrder(tenantId, toResponse(order));
        } catch (Exception ignored) {}
    }

    public DeliveryOrderDto.Response toResponse(DeliveryOrderEntity e) {
        List<DeliveryOrderDto.ItemResponse> items = e.getItems() != null ? e.getItems().stream()
                .map(i -> DeliveryOrderDto.ItemResponse.builder()
                        .id(i.getId())
                        .externalProductId(i.getExternalProductId())
                        .posProductId(i.getPosProduct() != null ? i.getPosProduct().getId() : null)
                        .posProductName(i.getPosProduct() != null ? i.getPosProduct().getName() : null)
                        .name(i.getName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .total(i.getTotal())
                        .mappingStatus(i.getMappingStatus())
                        .notes(i.getNotes())
                        .build())
                .collect(Collectors.toList()) : Collections.emptyList();

        return DeliveryOrderDto.Response.builder()
                .id(e.getId())
                .providerId(e.getProvider().getId())
                .providerName(e.getProvider().getName())
                .providerCode(e.getProvider().getCode())
                .externalOrderId(e.getExternalOrderId())
                .posOrderId(e.getPosOrder() != null ? e.getPosOrder().getId() : null)
                .posOrderNumber(e.getPosOrder() != null ? e.getPosOrder().getOrderNumber() : null)
                .status(e.getStatus().name())
                .customerName(e.getCustomerName())
                .customerPhone(e.getCustomerPhone())
                .deliveryAddress(e.getDeliveryAddress())
                .addressApartment(e.getAddressApartment())
                .addressEntrance(e.getAddressEntrance())
                .addressFloor(e.getAddressFloor())
                .addressComment(e.getAddressComment())
                .latitude(e.getLatitude())
                .longitude(e.getLongitude())
                .subtotal(e.getSubtotal())
                .deliveryFee(e.getDeliveryFee())
                .commission(e.getCommission())
                .discount(e.getDiscount())
                .serviceFee(e.getServiceFee())
                .total(e.getTotal())
                .paymentType(e.getPaymentType())
                .paymentStatus(e.getPaymentStatus())
                .courierName(e.getCourierName())
                .courierPhone(e.getCourierPhone())
                .courierVehicle(e.getCourierVehicle())
                .courierStatus(e.getCourierStatus())
                .errorMessage(e.getErrorMessage())
                .retryCount(e.getRetryCount())
                .createdAt(e.getCreatedAt())
                .items(items)
                .build();
    }
}
