package com.restaurantpos.kitchen.service;

import com.restaurantpos.common.exception.PosException;
import com.restaurantpos.common.websocket.WebSocketNotificationService;
import com.restaurantpos.kitchen.dto.KitchenDto;
import com.restaurantpos.kitchen.entity.Kitchen;
import com.restaurantpos.kitchen.repository.KitchenRepository;
import com.restaurantpos.orders.dto.OrderDto;
import com.restaurantpos.orders.entity.Order;
import com.restaurantpos.orders.entity.OrderItem;
import com.restaurantpos.orders.repository.OrderItemRepository;
import com.restaurantpos.orders.repository.OrderRepository;
import com.restaurantpos.orders.service.OrderService;
import com.restaurantpos.tenants.entity.Tenant;
import com.restaurantpos.tenants.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KitchenService {

    private final KitchenRepository kitchenRepository;
    private final TenantRepository tenantRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderService orderService;
    private final WebSocketNotificationService wsNotification;
    private final com.restaurantpos.printers.repository.PrinterAssignmentRepository assignmentRepository;
    private final com.restaurantpos.printers.repository.PrinterRepository printerRepository;
    private final com.restaurantpos.products.repository.CategoryRepository categoryRepository;
    private final com.restaurantpos.products.repository.ProductRepository productRepository;
    private final com.restaurantpos.kitchen.repository.KitchenTicketRepository kitchenTicketRepository;

    @Transactional(readOnly = true)
    public List<KitchenDto.Response> getKitchens(UUID tenantId) {
        List<Kitchen> kitchens = kitchenRepository.findByTenantIdAndDeletedAtIsNullOrderBySortOrderAsc(tenantId);
        List<com.restaurantpos.printers.entity.PrinterAssignment> assignments = assignmentRepository.findByTenantIdAndDeletedAtIsNull(tenantId);

        Map<UUID, com.restaurantpos.printers.entity.PrinterAssignment> primaryByKitchen = assignments.stream()
                .filter(a -> a.getKitchen() != null && a.isActive() && a.isPrimary())
                .collect(Collectors.toMap(a -> a.getKitchen().getId(), a -> a, (k1, k2) -> k1));

        return kitchens.stream().map(k -> {
            com.restaurantpos.printers.entity.PrinterAssignment pa = primaryByKitchen.get(k.getId());
            UUID printerId = pa != null ? pa.getPrinter().getId() : null;
            String printerName = pa != null ? pa.getPrinter().getName() : null;
            String printerStatus = pa != null ? pa.getPrinter().getStatus().name() : null;

            return KitchenDto.Response.builder()
                    .id(k.getId())
                    .name(k.getName())
                    .code(k.getCode())
                    .description(k.getDescription())
                    .sortOrder(k.getSortOrder())
                    .active(k.isActive())
                    .color(k.getColor())
                    .autoPrint(k.isAutoPrint())
                    .soundNotification(k.isSoundNotification())
                    .preparationTimeMinutes(k.getPreparationTimeMinutes())
                    .printerId(printerId)
                    .printerName(printerName)
                    .printerStatus(printerStatus)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public KitchenDto.Response createKitchen(UUID tenantId, KitchenDto.CreateRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> PosException.notFound("Tenant not found"));

        kitchenRepository.findByTenantIdAndCodeAndDeletedAtIsNull(tenantId, request.getCode().trim().toUpperCase())
                .ifPresent(k -> {
                    throw PosException.badRequest("Bu kodli oshxona allaqachon mavjud: " + request.getCode());
                });

        Kitchen kitchen = new Kitchen();
        kitchen.setTenant(tenant);
        kitchen.setName(request.getName().trim());
        kitchen.setCode(request.getCode().trim().toUpperCase());
        kitchen.setDescription(request.getDescription());
        kitchen.setSortOrder(request.getSortOrder());
        kitchen.setActive(true);
        if (request.getColor() != null) kitchen.setColor(request.getColor());
        if (request.getAutoPrint() != null) kitchen.setAutoPrint(request.getAutoPrint());
        if (request.getSoundNotification() != null) kitchen.setSoundNotification(request.getSoundNotification());
        if (request.getPreparationTimeMinutes() != null) kitchen.setPreparationTimeMinutes(request.getPreparationTimeMinutes());

        Kitchen saved = kitchenRepository.save(kitchen);

        if (request.getPrinterId() != null) {
            assignKitchenPrinter(tenant, saved, request.getPrinterId());
        }

        return getKitchens(tenantId).stream()
                .filter(k -> k.getId().equals(saved.getId()))
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public KitchenDto.Response updateKitchen(UUID tenantId, UUID kitchenId, KitchenDto.UpdateRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> PosException.notFound("Tenant not found"));

        Kitchen kitchen = kitchenRepository.findByIdAndTenantIdAndDeletedAtIsNull(kitchenId, tenantId)
                .orElseThrow(() -> PosException.notFound("Oshxona topilmadi: " + kitchenId));

        if (request.getName() != null && !request.getName().isBlank()) kitchen.setName(request.getName().trim());
        if (request.getCode() != null && !request.getCode().isBlank()) kitchen.setCode(request.getCode().trim().toUpperCase());
        if (request.getDescription() != null) kitchen.setDescription(request.getDescription());
        if (request.getSortOrder() != null) kitchen.setSortOrder(request.getSortOrder());
        if (request.getActive() != null) kitchen.setActive(request.getActive());
        if (request.getColor() != null) kitchen.setColor(request.getColor());
        if (request.getAutoPrint() != null) kitchen.setAutoPrint(request.getAutoPrint());
        if (request.getSoundNotification() != null) kitchen.setSoundNotification(request.getSoundNotification());
        if (request.getPreparationTimeMinutes() != null) kitchen.setPreparationTimeMinutes(request.getPreparationTimeMinutes());

        Kitchen saved = kitchenRepository.save(kitchen);

        if (request.getPrinterId() != null) {
            assignKitchenPrinter(tenant, saved, request.getPrinterId());
        }

        return getKitchens(tenantId).stream()
                .filter(k -> k.getId().equals(saved.getId()))
                .findFirst()
                .orElse(null);
    }

    private void assignKitchenPrinter(Tenant tenant, Kitchen kitchen, UUID printerId) {
        com.restaurantpos.printers.entity.Printer printer = printerRepository.findByIdAndTenantIdAndDeletedAtIsNull(printerId, tenant.getId())
                .orElseThrow(() -> PosException.notFound("Printer topilmadi: " + printerId));

        List<com.restaurantpos.printers.entity.PrinterAssignment> existing = assignmentRepository
                .findByTenantIdAndKitchenIdAndActiveTrueAndDeletedAtIsNull(tenant.getId(), kitchen.getId());

        for (com.restaurantpos.printers.entity.PrinterAssignment ea : existing) {
            if (!ea.getPrinter().getId().equals(printer.getId())) {
                ea.setPrimary(false);
                assignmentRepository.save(ea);
            }
        }

        com.restaurantpos.printers.entity.PrinterAssignment assignment = existing.stream()
                .filter(a -> a.getPrinter().getId().equals(printer.getId()))
                .findFirst()
                .orElseGet(() -> {
                    com.restaurantpos.printers.entity.PrinterAssignment pa = new com.restaurantpos.printers.entity.PrinterAssignment();
                    pa.setTenant(tenant);
                    pa.setPrinter(printer);
                    pa.setKitchen(kitchen);
                    pa.setPurpose("KITCHEN");
                    return pa;
                });

        assignment.setPrimary(true);
        assignment.setActive(true);
        assignmentRepository.save(assignment);
    }

    @Transactional(readOnly = true)
    public List<OrderDto.Response> getActiveKitchenOrders(UUID tenantId, UUID kitchenId) {
        List<Order.OrderStatus> kitchenStatuses = List.of(
                Order.OrderStatus.OPEN,
                Order.OrderStatus.IN_PROGRESS
        );

        List<Order> orders = orderRepository.findByTenantIdAndStatusInAndDeletedAtIsNullOrderByOpenedAtDesc(tenantId, kitchenStatuses);

        if (kitchenId == null) {
            return orders.stream().map(orderService::toResponse).collect(Collectors.toList());
        }

        return orders.stream()
                .map(order -> filterOrderForKitchen(order, kitchenId))
                .filter(res -> res != null && res.getItems() != null && !res.getItems().isEmpty())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDto.Response> getActiveKitchenOrdersForKitchens(UUID tenantId, Set<UUID> kitchenIds) {
        if (kitchenIds == null || kitchenIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Order.OrderStatus> kitchenStatuses = List.of(
                Order.OrderStatus.OPEN,
                Order.OrderStatus.IN_PROGRESS
        );

        List<Order> orders = orderRepository.findByTenantIdAndStatusInAndDeletedAtIsNullOrderByOpenedAtDesc(tenantId, kitchenStatuses);

        return orders.stream()
                .map(order -> filterOrderForKitchens(order, kitchenIds))
                .filter(res -> res != null && res.getItems() != null && !res.getItems().isEmpty())
                .collect(Collectors.toList());
    }

    private OrderDto.Response filterOrderForKitchens(Order order, Set<UUID> kitchenIds) {
        OrderDto.Response full = orderService.toResponse(order);
        List<OrderDto.ItemResponse> filteredItems = full.getItems().stream()
                .filter(i -> i.getKitchenId() != null && kitchenIds.contains(i.getKitchenId()))
                .collect(Collectors.toList());

        if (filteredItems.isEmpty()) {
            return null;
        }

        full.setItems(filteredItems);
        // Financial Data Isolation: Kitchen user must NOT see full order financials!
        full.setSubtotal(null);
        full.setDiscountAmount(null);
        full.setDiscountPercent(null);
        full.setTaxAmount(null);
        full.setTotal(null);
        full.setPaidAmount(null);
        full.setChangeAmount(null);
        return full;
    }

    private OrderDto.Response filterOrderForKitchen(Order order, UUID kitchenId) {
        OrderDto.Response full = orderService.toResponse(order);
        List<OrderDto.ItemResponse> filteredItems = full.getItems().stream()
                .filter(i -> kitchenId.equals(i.getKitchenId()))
                .collect(Collectors.toList());

        if (filteredItems.isEmpty()) {
            return null;
        }

        full.setItems(filteredItems);
        // Financial Data Isolation: Kitchen user must NOT see full order financials!
        full.setSubtotal(null);
        full.setDiscountAmount(null);
        full.setDiscountPercent(null);
        full.setTaxAmount(null);
        full.setTotal(null);
        full.setPaidAmount(null);
        full.setChangeAmount(null);
        return full;
    }

    @Transactional(readOnly = true)
    public OrderDto.Response getKitchenOrderById(UUID orderId, UUID tenantId, UUID userKitchenId) {
        Order order = orderRepository.findByIdAndTenantIdAndDeletedAtIsNull(orderId, tenantId)
                .orElseThrow(() -> PosException.notFound("Buyurtma topilmadi: " + orderId));

        if (userKitchenId != null) {
            OrderDto.Response filtered = filterOrderForKitchen(order, userKitchenId);
            if (filtered == null) {
                throw PosException.forbidden("Sizda boshqa oshxona ma'lumotlarini ko'rish huquqi yo'q!");
            }
            return filtered;
        }
        return orderService.toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderDto.Response getKitchenOrderByIdForKitchens(UUID orderId, UUID tenantId, Set<UUID> userKitchenIds) {
        Order order = orderRepository.findByIdAndTenantIdAndDeletedAtIsNull(orderId, tenantId)
                .orElseThrow(() -> PosException.notFound("Buyurtma topilmadi: " + orderId));

        if (userKitchenIds != null && !userKitchenIds.isEmpty()) {
            OrderDto.Response filtered = filterOrderForKitchens(order, userKitchenIds);
            if (filtered == null) {
                throw PosException.forbidden("Sizda boshqa oshxona ma'lumotlarini ko'rish huquqi yo'q!");
            }
            return filtered;
        }
        return orderService.toResponse(order);
    }

    @Transactional(readOnly = true)
    public com.restaurantpos.kitchen.entity.KitchenTicket getKitchenTicketById(UUID ticketId, UUID tenantId, UUID userKitchenId) {
        com.restaurantpos.kitchen.entity.KitchenTicket ticket = kitchenTicketRepository.findByIdAndTenantId(ticketId, tenantId)
                .orElseThrow(() -> PosException.notFound("Kitchen ticket topilmadi: " + ticketId));

        if (userKitchenId != null && ticket.getKitchen() != null && !userKitchenId.equals(ticket.getKitchen().getId())) {
            throw PosException.forbidden("Sizda boshqa oshxona ma'lumotlarini ko'rish huquqi yo'q!");
        }
        return ticket;
    }

    @Transactional
    public void updateKitchenOrderStatus(UUID orderId, UUID tenantId, String statusStr, UUID userKitchenId) {
        updateKitchenOrderStatusForKitchens(orderId, tenantId, statusStr, userKitchenId != null ? Set.of(userKitchenId) : null);
    }

    @Transactional
    public void updateKitchenOrderStatusForKitchens(UUID orderId, UUID tenantId, String statusStr, Set<UUID> kitchenIds) {
        Order order = orderRepository.findByIdAndTenantIdAndDeletedAtIsNull(orderId, tenantId)
                .orElseThrow(() -> PosException.notFound("Buyurtma topilmadi: " + orderId));

        if (kitchenIds != null && !kitchenIds.isEmpty()) {
            boolean hasItemsForKitchen = order.getItems().stream()
                    .anyMatch(i -> i.getKitchen() != null && kitchenIds.contains(i.getKitchen().getId()));
            if (!hasItemsForKitchen) {
                throw PosException.forbidden("Sizda boshqa oshxona ma'lumotlarini o'zgartirish huquqi yo'q!");
            }
        }

        OrderItem.KitchenStatus status = OrderItem.KitchenStatus.valueOf(statusStr.toUpperCase());
        for (OrderItem item : order.getItems()) {
            if (kitchenIds == null || (item.getKitchen() != null && kitchenIds.contains(item.getKitchen().getId()))) {
                item.setKitchenStatus(status);
                if (status == OrderItem.KitchenStatus.READY) {
                    item.setReadyAt(Instant.now());
                } else if (status == OrderItem.KitchenStatus.DELIVERED || status == OrderItem.KitchenStatus.SERVED) {
                    item.setDeliveredQuantity(item.getQuantity());
                }
            }
        }
        orderRepository.save(order);
    }

    @Transactional
    public void updateItemKitchenStatus(UUID itemId, String statusStr) {
        updateItemKitchenStatus(itemId, statusStr, null);
    }

    @Transactional
    public void updateItemKitchenStatus(UUID itemId, String statusStr, UUID userKitchenId) {
        updateItemKitchenStatusForKitchens(itemId, statusStr, userKitchenId != null ? Set.of(userKitchenId) : null);
    }

    @Transactional
    public void updateItemKitchenStatusForKitchens(UUID itemId, String statusStr, Set<UUID> kitchenIds) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> PosException.notFound("Order item not found: " + itemId));

        if (kitchenIds != null && !kitchenIds.isEmpty() && item.getKitchen() != null && !kitchenIds.contains(item.getKitchen().getId())) {
            throw PosException.forbidden("Sizda boshqa oshxona mahsuloti statusini o'zgartirish huquqi yo'q!");
        }

        OrderItem.KitchenStatus status = OrderItem.KitchenStatus.valueOf(statusStr.toUpperCase());
        item.setKitchenStatus(status);

        if (status == OrderItem.KitchenStatus.READY) {
            item.setReadyAt(Instant.now());
        } else if (status == OrderItem.KitchenStatus.DELIVERED || status == OrderItem.KitchenStatus.SERVED) {
            item.setDeliveredQuantity(item.getQuantity());
        }

        orderItemRepository.save(item);

        // Real-time: Aniq shu oshxonaning kanaliga (/topic/kitchen/{kitchenId}) xabar yuborish
        if (item.getKitchen() != null) {
            Map<String, Object> updatePayload = Map.of(
                    "itemId", item.getId(),
                    "orderId", item.getOrder().getId(),
                    "status", status.name(),
                    "readyAt", item.getReadyAt() != null ? item.getReadyAt().toString() : ""
            );
            wsNotification.notifyKitchenItemStatus(item.getKitchen().getId(), updatePayload);
        }

        // Barcha KDS ekranlariga umumiy xabar
        wsNotification.notifyItemReady(item.getOrder().getTenant().getId(), itemId);
        // POS ekranlariga buyurtma yangilanganini bildirish
        wsNotification.notifyOrderStatusChanged(item.getOrder().getTenant().getId(), orderService.toResponse(item.getOrder()));
    }

    @Transactional
    public void deleteKitchen(UUID tenantId, UUID kitchenId) {
        Kitchen kitchen = kitchenRepository.findByIdAndTenantIdAndDeletedAtIsNull(kitchenId, tenantId)
                .orElseThrow(() -> PosException.notFound("Oshxona topilmadi: " + kitchenId));

        long categoryCount = categoryRepository.countByTenantIdAndKitchenIdAndDeletedAtIsNull(tenantId, kitchenId);
        long productCount = productRepository.countByTenantIdAndKitchenIdAndDeletedAtIsNull(tenantId, kitchenId);

        if (categoryCount > 0 || productCount > 0) {
            throw PosException.badRequest(String.format(
                    "Bu oshxonaga %d ta kategoriya va %d ta mahsulot bog'langan. Avval ularni ko'chiring yoki faolsizlantiring.",
                    categoryCount, productCount));
        }

        kitchen.setDeletedAt(Instant.now());
        kitchen.setActive(false);
        kitchenRepository.save(kitchen);
    }
}
