package com.restaurantpos.common.websocket;

import com.restaurantpos.orders.dto.OrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * WebSocket notification service.
 * Broadcasts real-time events to connected clients (KDS, POS terminals).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /** Yangi buyurtma keldi — KDS ekranlari uchun */
    public void notifyNewOrder(UUID tenantId, OrderDto.Response order) {
        String destination = "/topic/kitchen/" + tenantId;
        messagingTemplate.convertAndSend(destination, WebSocketEvent.newOrder(order));
        log.debug("WS: new order {} -> {}", order.getId(), destination);
    }

    /** Buyurtma holati o'zgardi */
    public void notifyOrderStatusChanged(UUID tenantId, OrderDto.Response order) {
        String destination = "/topic/orders/" + tenantId;
        messagingTemplate.convertAndSend(destination, WebSocketEvent.orderUpdated(order));
        log.debug("WS: order status {} -> {}", order.getStatus(), destination);
    }

    /** Buyurtma to'landi — stol bo'shadi */
    public void notifyOrderPaid(UUID tenantId, UUID orderId) {
        String destination = "/topic/tables/" + tenantId;
        messagingTemplate.convertAndSend(destination, WebSocketEvent.orderPaid(orderId));
        log.debug("WS: order paid {} -> {}", orderId, destination);
    }

    /** Oshxona: mahsulot tayyor */
    public void notifyItemReady(UUID tenantId, UUID orderItemId) {
        String destination = "/topic/kitchen/" + tenantId;
        messagingTemplate.convertAndSend(destination, WebSocketEvent.itemReady(orderItemId));
        log.debug("WS: item ready {} -> {}", orderItemId, destination);
    }

    /** Aniq bitta oshxonaga yangi buyurtma/chipta keldi (/topic/kitchen/{kitchenId}) */
    public void notifyKitchenNewTicket(UUID kitchenId, Object ticketPayload) {
        String destination = "/topic/kitchen/" + kitchenId;
        messagingTemplate.convertAndSend(destination, new WebSocketEvent("KITCHEN_NEW_TICKET", ticketPayload));
        log.debug("WS: new ticket for kitchen {} -> {}", kitchenId, destination);
    }

    /** Aniq bitta oshxonadagi taom holati o'zgardi (/topic/kitchen/{kitchenId}) */
    public void notifyKitchenItemStatus(UUID kitchenId, Object itemPayload) {
        String destination = "/topic/kitchen/" + kitchenId;
        messagingTemplate.convertAndSend(destination, new WebSocketEvent("KITCHEN_ITEM_STATUS", itemPayload));
        log.debug("WS: item status for kitchen {} -> {}", kitchenId, destination);
    }

    /** Aniq bitta oshxonadagi taom bekor qilindi (/topic/kitchen/{kitchenId}) */
    public void notifyKitchenItemCancelled(UUID kitchenId, Object payload) {
        String destination = "/topic/kitchen/" + kitchenId;
        messagingTemplate.convertAndSend(destination, new WebSocketEvent("ORDER_ITEM_CANCELLED", payload));
        log.debug("WS: item cancelled for kitchen {} -> {}", kitchenId, destination);
    }

    /** Oshxona uchun butun buyurtma bekor qilindi (/topic/kitchen/{kitchenId}) */
    public void notifyKitchenOrderCancelled(UUID kitchenId, Object payload) {
        String destination = "/topic/kitchen/" + kitchenId;
        messagingTemplate.convertAndSend(destination, new WebSocketEvent("ORDER_CANCELLED", payload));
        log.debug("WS: order cancelled for kitchen {} -> {}", kitchenId, destination);
    }

    /** Stol holati o'zgardi (masalan bo'shadi yoki band bo'ldi) */
    public void notifyTableStatusChanged(UUID tenantId, UUID tableId, String status) {
        String destination = "/topic/tables/" + tenantId;
        messagingTemplate.convertAndSend(destination, new WebSocketEvent("TABLE_STATUS_CHANGED", java.util.Map.of("tableId", tableId, "status", status)));
        log.debug("WS: table status changed {} -> {} ({})", tableId, destination, status);
    }

    /** Stol to'liq ma'lumoti yangilandi (summa, taomlar soni, status) */
    public void notifyTableUpdated(UUID tenantId, Object tablePayload) {
        String destination = "/topic/tables/" + tenantId;
        messagingTemplate.convertAndSend(destination, new WebSocketEvent("TABLE_UPDATED", tablePayload));
        log.debug("WS: table updated -> {}", destination);
    }

    // -------------------------------------------------------

    public record WebSocketEvent(String type, Object payload) {
        static WebSocketEvent newOrder(OrderDto.Response order) {
            return new WebSocketEvent("NEW_ORDER", order);
        }
        static WebSocketEvent orderUpdated(OrderDto.Response order) {
            return new WebSocketEvent("ORDER_UPDATED", order);
        }
        static WebSocketEvent orderPaid(UUID orderId) {
            return new WebSocketEvent("ORDER_PAID", orderId);
        }
        static WebSocketEvent itemReady(UUID itemId) {
            return new WebSocketEvent("ITEM_READY", itemId);
        }
    }
}
