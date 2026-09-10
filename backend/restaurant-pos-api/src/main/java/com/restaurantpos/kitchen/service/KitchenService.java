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
import java.util.List;
import java.util.Map;
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

    @Transactional(readOnly = true)
    public List<KitchenDto.Response> getKitchens(UUID tenantId) {
        return kitchenRepository.findByTenantIdAndDeletedAtIsNullOrderBySortOrderAsc(tenantId)
                .stream().map(k -> KitchenDto.Response.builder()
                        .id(k.getId())
                        .name(k.getName())
                        .code(k.getCode())
                        .description(k.getDescription())
                        .sortOrder(k.getSortOrder())
                        .active(k.isActive())
                        .build()
                ).collect(Collectors.toList());
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

        Kitchen saved = kitchenRepository.save(kitchen);
        return KitchenDto.Response.builder()
                .id(saved.getId())
                .name(saved.getName())
                .code(saved.getCode())
                .description(saved.getDescription())
                .sortOrder(saved.getSortOrder())
                .active(saved.isActive())
                .build();
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

    private OrderDto.Response filterOrderForKitchen(Order order, UUID kitchenId) {
        OrderDto.Response full = orderService.toResponse(order);
        List<OrderDto.ItemResponse> filteredItems = full.getItems().stream()
                .filter(i -> kitchenId.equals(i.getKitchenId()))
                .collect(Collectors.toList());

        if (filteredItems.isEmpty()) {
            return null;
        }

        full.setItems(filteredItems);
        return full;
    }

    @Transactional
    public void updateItemKitchenStatus(UUID itemId, String statusStr) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> PosException.notFound("Order item not found: " + itemId));

        OrderItem.KitchenStatus status = OrderItem.KitchenStatus.valueOf(statusStr.toUpperCase());
        item.setKitchenStatus(status);

        if (status == OrderItem.KitchenStatus.READY) {
            item.setReadyAt(Instant.now());
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
    }
}
