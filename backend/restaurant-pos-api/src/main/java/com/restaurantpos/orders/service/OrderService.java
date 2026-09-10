package com.restaurantpos.orders.service;

import com.restaurantpos.common.exception.PosException;
import com.restaurantpos.customers.entity.Customer;
import com.restaurantpos.customers.repository.CustomerRepository;
import com.restaurantpos.orders.dto.CancellationReceiptDto;
import com.restaurantpos.orders.dto.OrderDto;
import com.restaurantpos.orders.entity.CancellationReceipt;
import com.restaurantpos.orders.entity.Order;
import com.restaurantpos.orders.entity.OrderItem;
import com.restaurantpos.orders.entity.OrderItemModifier;
import com.restaurantpos.orders.repository.CancellationReceiptRepository;
import com.restaurantpos.orders.repository.OrderItemRepository;
import com.restaurantpos.orders.repository.OrderRepository;
import com.restaurantpos.products.entity.Modifier;
import com.restaurantpos.products.entity.Product;
import com.restaurantpos.products.repository.ModifierRepository;
import com.restaurantpos.products.repository.ProductRepository;
import com.restaurantpos.shifts.entity.Shift;
import com.restaurantpos.shifts.repository.ShiftRepository;
import com.restaurantpos.tables.entity.RestaurantTable;
import com.restaurantpos.tables.repository.RestaurantTableRepository;
import com.restaurantpos.tenants.entity.Tenant;
import com.restaurantpos.tenants.repository.TenantRepository;
import com.restaurantpos.users.entity.User;
import com.restaurantpos.users.repository.UserRepository;
import com.restaurantpos.payments.entity.Payment;
import com.restaurantpos.payments.repository.PaymentRepository;
import com.restaurantpos.tables.dto.TableDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CancellationReceiptRepository cancellationReceiptRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final ModifierRepository modifierRepository;
    private final RestaurantTableRepository tableRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final ShiftRepository shiftRepository;
    private final com.restaurantpos.kitchen.repository.KitchenTicketRepository kitchenTicketRepository;
    private final com.restaurantpos.common.websocket.WebSocketNotificationService wsNotification;

    private static final AtomicInteger ORDER_COUNTER = new AtomicInteger(100);
    private static final AtomicInteger CANCEL_COUNTER = new AtomicInteger(100);

    @Transactional(readOnly = true)
    public List<OrderDto.Response> getActiveOrders(UUID tenantId) {
        return orderRepository.findActiveOrders(tenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDto.Response> getOrderHistory(UUID tenantId, UUID tableId, String paymentMethod, String search) {
        List<Order> orders = orderRepository.findHistoryOrders(tenantId);
        return orders.stream()
                .filter(o -> {
                    if (tableId != null && (o.getTable() == null || !tableId.equals(o.getTable().getId()))) {
                        return false;
                    }
                    if (search != null && !search.trim().isBlank()) {
                        String q = search.trim().toLowerCase();
                        boolean matchNum = o.getOrderNumber() != null && o.getOrderNumber().toLowerCase().contains(q);
                        boolean matchTbl = o.getTable() != null && o.getTable().getName() != null && o.getTable().getName().toLowerCase().contains(q);
                        boolean matchWaiter = o.getWaiter() != null && (o.getWaiter().getFirstName() + " " + (o.getWaiter().getLastName() != null ? o.getWaiter().getLastName() : "")).toLowerCase().contains(q);
                        if (!matchNum && !matchTbl && !matchWaiter) return false;
                    }
                    return true;
                })
                .map(this::toResponse)
                .filter(res -> {
                    if (paymentMethod != null && !paymentMethod.trim().isBlank() && !"ALL".equalsIgnoreCase(paymentMethod)) {
                        return paymentMethod.equalsIgnoreCase(res.getPaymentMethod());
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    public TableDto.Response toTableResponse(RestaurantTable table, Order activeOrder) {
        String activeOrderNumber = null;
        Integer itemCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        if (activeOrder != null && activeOrder.getStatus() != Order.OrderStatus.PAID && activeOrder.getStatus() != Order.OrderStatus.CANCELLED) {
            activeOrderNumber = activeOrder.getOrderNumber();
            itemCount = activeOrder.getItems() != null
                    ? activeOrder.getItems().stream()
                        .filter(i -> !i.isVoided())
                        .mapToInt(i -> i.getQuantity().intValue())
                        .sum()
                    : 0;
            totalAmount = activeOrder.getTotal() != null ? activeOrder.getTotal() : (activeOrder.getSubtotal() != null ? activeOrder.getSubtotal() : BigDecimal.ZERO);
        }

        return TableDto.Response.builder()
                .id(table.getId())
                .zoneId(table.getZone() != null ? table.getZone().getId() : null)
                .zoneName(table.getZone() != null ? table.getZone().getName() : null)
                .tableNumber(table.getTableNumber())
                .name(table.getName())
                .capacity(table.getCapacity())
                .shape(table.getShape())
                .posX(table.getPosX())
                .posY(table.getPosY())
                .width(table.getWidth())
                .height(table.getHeight())
                .status(table.getStatus() != null ? table.getStatus().name() : "FREE")
                .currentOrderId(table.getCurrentOrderId())
                .activeOrderNumber(activeOrderNumber)
                .itemCount(itemCount)
                .totalAmount(totalAmount)
                .active(table.isActive())
                .build();
    }

    @Transactional(readOnly = true)
    public OrderDto.Response getOrderById(UUID id, UUID tenantId) {
        Order order = orderRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Order not found: " + id));
        return toResponse(order);
    }

    @Transactional
    public OrderDto.Response createOrder(UUID tenantId, UUID userId, OrderDto.CreateRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> PosException.notFound("Tenant not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> PosException.notFound("User not found"));

        RestaurantTable table = null;
        if (request.getTableId() != null) {
            table = tableRepository.findByIdAndTenantIdAndDeletedAtIsNull(request.getTableId(), tenantId)
                    .orElseThrow(() -> PosException.notFound("Table not found"));
        }

        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId()).orElse(null);
        }

        Shift currentShift = shiftRepository.findByTenantIdAndStatus(tenantId, Shift.ShiftStatus.OPEN).orElse(null);

        Order order = new Order();
        order.setTenant(tenant);
        order.setTable(table);
        order.setCustomer(customer);
        order.setWaiter(user);
        order.setShift(currentShift);
        order.setOrderType(parseOrderType(request.getOrderType()));
        order.setStatus(Order.OrderStatus.OPEN);
        order.setNotes(request.getNotes());
        order.setKitchenNotes(request.getKitchenNotes());

        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        order.setOrderNumber("ORD-" + dateStr + "-" + String.format("%04d", ORDER_COUNTER.incrementAndGet() % 10000));

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (OrderDto.ItemRequest itemReq : request.getItems()) {
                OrderItem item = buildOrderItem(order, itemReq, tenantId);
                order.getItems().add(item);
            }
        }

        order.recalculateTotals();

        Order saved = orderRepository.save(order);

        // If table assigned, mark table as OCCUPIED
        if (table != null) {
            table.setStatus(RestaurantTable.TableStatus.OCCUPIED);
            table.setCurrentOrderId(saved.getId());
            tableRepository.save(table);
        }

        routeOrderToKitchens(saved, tenant);
        saved = orderRepository.save(saved);
        if (table != null) {
            wsNotification.notifyTableUpdated(tenantId, toTableResponse(table, saved));
        }
        return toResponse(saved);
    }

    @Transactional
    public OrderDto.Response addItemsToOrder(UUID orderId, UUID tenantId, OrderDto.AddItemsRequest request) {
        Order order = orderRepository.findByIdAndTenantIdAndDeletedAtIsNull(orderId, tenantId)
                .orElseThrow(() -> PosException.notFound("Order not found: " + orderId));

        if (order.getStatus() == Order.OrderStatus.PAID || order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw PosException.badRequest("Cannot add items to " + order.getStatus() + " order");
        }

        for (OrderDto.ItemRequest itemReq : request.getItems()) {
            OrderItem item = buildOrderItem(order, itemReq, tenantId);
            order.getItems().add(item);
        }

        order.recalculateTotals();
        Order saved = orderRepository.save(order);
        wsNotification.notifyOrderStatusChanged(tenantId, toResponse(saved));
        if (saved.getTable() != null) {
            wsNotification.notifyTableUpdated(tenantId, toTableResponse(saved.getTable(), saved));
        }
        return toResponse(saved);
    }

    @Transactional
    public OrderDto.Response voidOrderItem(UUID orderId, UUID itemId, UUID userId, UUID tenantId, OrderDto.VoidItemRequest request) {
        Order order = orderRepository.findByIdAndTenantIdAndDeletedAtIsNull(orderId, tenantId)
                .orElseThrow(() -> PosException.notFound("Order not found: " + orderId));

        OrderItem item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> PosException.notFound("Order item not found: " + itemId));

        User user = userRepository.findById(userId).orElse(null);

        item.setVoided(true);
        item.setVoidReason(request.getReason());
        item.setVoidedAt(Instant.now());
        item.setVoidedBy(user);

        order.recalculateTotals();
        Order saved = orderRepository.save(order);
        if (saved.getTable() != null) {
            wsNotification.notifyTableUpdated(tenantId, toTableResponse(saved.getTable(), saved));
        }
        return toResponse(saved);
    }

    @Transactional
    public OrderDto.Response applyDiscount(UUID orderId, UUID tenantId, OrderDto.ApplyDiscountRequest request) {
        Order order = orderRepository.findByIdAndTenantIdAndDeletedAtIsNull(orderId, tenantId)
                .orElseThrow(() -> PosException.notFound("Order not found: " + orderId));

        if (request.getPercent() != null) {
            order.setDiscountPercent(request.getPercent());
        }
        if (request.getAmount() != null) {
            order.setDiscountAmount(request.getAmount());
        }

        order.recalculateTotals();
        Order saved = orderRepository.save(order);
        if (saved.getTable() != null) {
            wsNotification.notifyTableUpdated(tenantId, toTableResponse(saved.getTable(), saved));
        }
        return toResponse(saved);
    }

    @Transactional
    public OrderDto.Response updateOrderStatus(UUID orderId, UUID tenantId, OrderDto.UpdateStatusRequest request) {
        Order order = orderRepository.findByIdAndTenantIdAndDeletedAtIsNull(orderId, tenantId)
                .orElseThrow(() -> PosException.notFound("Order not found: " + orderId));

        String s = request.getStatus().toUpperCase();
        if ("SENT_TO_KITCHEN".equals(s)) s = "IN_PROGRESS";
        Order.OrderStatus status = Order.OrderStatus.valueOf(s);
        order.setStatus(status);

        if (status == Order.OrderStatus.IN_PROGRESS && order.getSentToKitchenAt() == null) {
            order.setSentToKitchenAt(Instant.now());
        } else if (status == Order.OrderStatus.READY) {
            order.setReadyAt(Instant.now());
        } else if (status == Order.OrderStatus.CANCELLED) {
            order.setClosedAt(Instant.now());
            if (order.getTable() != null) {
                RestaurantTable table = order.getTable();
                table.setStatus(RestaurantTable.TableStatus.FREE);
                table.setCurrentOrderId(null);
                tableRepository.save(table);
            }
        }

        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    private OrderItem buildOrderItem(Order order, OrderDto.ItemRequest req, UUID tenantId) {
        Product product = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(req.getProductId(), tenantId)
                .orElseThrow(() -> PosException.notFound("Product not found: " + req.getProductId()));

        if (product.getKitchen() == null) {
            throw PosException.badRequest("'" + product.getName() + "' mahsulotiga oshxona biriktirilmagan! Iltimos, avval mahsulotga oshxona biriktiring.");
        }

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setKitchen(product.getKitchen());
        item.setProductName(product.getName());
        item.setProductSku(product.getSku());
        item.setQuantity(req.getQuantity());
        item.setUnitPrice(product.getSalePrice());
        item.setNotes(req.getNotes());
        item.setKitchenStatus(OrderItem.KitchenStatus.NEW);

        if (req.getModifiers() != null && !req.getModifiers().isEmpty()) {
            for (OrderDto.ModifierRequest modReq : req.getModifiers()) {
                Modifier mod = modifierRepository.findById(modReq.getModifierId())
                        .orElseThrow(() -> PosException.notFound("Modifier not found: " + modReq.getModifierId()));

                OrderItemModifier applied = new OrderItemModifier();
                applied.setOrderItem(item);
                applied.setModifier(mod);
                applied.setModifierName(mod.getName());
                applied.setPrice(mod.getPrice());
                applied.setQuantity(modReq.getQuantity());
                item.getModifiers().add(applied);
            }
        }

        item.calculateSubtotal();
        return item;
    }

    private Order.OrderType parseOrderType(String typeStr) {
        if (typeStr == null) return Order.OrderType.DINE_IN;
        try {
            return Order.OrderType.valueOf(typeStr.toUpperCase());
        } catch (Exception e) {
            return Order.OrderType.DINE_IN;
        }
    }

    public OrderDto.Response toResponse(Order order) {
        List<OrderDto.ItemResponse> items = Collections.emptyList();
        if (order.getItems() != null) {
            items = order.getItems().stream()
                    .map(this::toItemResponse)
                    .collect(Collectors.toList());
        }

        UUID cashierId = order.getCashier() != null ? order.getCashier().getId() : null;
        String cashierName = order.getCashier() != null ? order.getCashier().getFirstName() + " " + (order.getCashier().getLastName() != null ? order.getCashier().getLastName() : "") : null;
        String paymentMethod = null;
        BigDecimal paidAmount = null;
        BigDecimal changeAmount = null;

        if (order.getStatus() == Order.OrderStatus.PAID) {
            List<Payment> payments = paymentRepository.findByOrderId(order.getId());
            if (!payments.isEmpty()) {
                Payment p = payments.get(0);
                paymentMethod = p.getPaymentMethod() != null ? p.getPaymentMethod().name() : null;
                paidAmount = p.getAmount();
                changeAmount = p.getChangeAmount();
                if (cashierId == null && p.getCashier() != null) {
                    cashierId = p.getCashier().getId();
                    cashierName = p.getCashier().getFirstName() + " " + (p.getCashier().getLastName() != null ? p.getCashier().getLastName() : "");
                }
            }
        }

        return OrderDto.Response.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderType(order.getOrderType().name())
                .status(order.getStatus().name())
                .tableId(order.getTable() != null ? order.getTable().getId() : null)
                .tableNumber(order.getTable() != null ? order.getTable().getTableNumber() : null)
                .tableName(order.getTable() != null ? order.getTable().getName() : null)
                .customerId(order.getCustomer() != null ? order.getCustomer().getId() : null)
                .customerName(order.getCustomer() != null ? order.getCustomer().getFullName() : null)
                .waiterId(order.getWaiter() != null ? order.getWaiter().getId() : null)
                .waiterName(order.getWaiter() != null ? order.getWaiter().getFirstName() + " " + (order.getWaiter().getLastName() != null ? order.getWaiter().getLastName() : "") : null)
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .discountPercent(order.getDiscountPercent())
                .taxAmount(order.getTaxAmount())
                .total(order.getTotal())
                .notes(order.getNotes())
                .kitchenNotes(order.getKitchenNotes())
                .openedAt(order.getOpenedAt())
                .sentToKitchenAt(order.getSentToKitchenAt())
                .readyAt(order.getReadyAt())
                .paidAt(order.getPaidAt())
                .closedAt(order.getClosedAt())
                .cashierId(cashierId)
                .cashierName(cashierName)
                .paymentMethod(paymentMethod)
                .paidAmount(paidAmount)
                .changeAmount(changeAmount)
                .items(items)
                .version(order.getVersion())
                .build();
    }

    @Transactional
    public OrderDto.Response sendNewItemsToKitchen(UUID orderId, UUID tenantId, OrderDto.SendToKitchenRequest request) {
        Order order = orderRepository.findByIdAndTenantIdAndDeletedAtIsNull(orderId, tenantId)
                .orElseThrow(() -> PosException.notFound("Order not found: " + orderId));

        if (order.getStatus() == Order.OrderStatus.PAID || order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw PosException.badRequest("Cannot send items for " + order.getStatus() + " order");
        }

        // 1. If request has new items, build and add them with status NEW
        if (request != null && request.getItems() != null && !request.getItems().isEmpty()) {
            for (OrderDto.ItemRequest itemReq : request.getItems()) {
                OrderItem item = buildOrderItem(order, itemReq, tenantId);
                order.getItems().add(item);
            }
            order.recalculateTotals();
        }

        // 2. Filter strictly for items that need to be sent:
        // status == NEW, not voided, quantity > 0
        List<OrderItem> itemsToSend = order.getItems().stream()
                .filter(i -> !i.isVoided() && i.getKitchenStatus() == OrderItem.KitchenStatus.NEW && i.getKitchen() != null)
                .collect(Collectors.toList());

        if (itemsToSend.isEmpty()) {
            throw PosException.badRequest("Oshxonaga yuborish uchun yangi mahsulotlar yo'q");
        }

        // 3. Group by kitchen
        java.util.Map<com.restaurantpos.kitchen.entity.Kitchen, List<OrderItem>> itemsByKitchen = itemsToSend.stream()
                .collect(Collectors.groupingBy(OrderItem::getKitchen));

        Instant now = Instant.now();

        for (java.util.Map.Entry<com.restaurantpos.kitchen.entity.Kitchen, List<OrderItem>> entry : itemsByKitchen.entrySet()) {
            com.restaurantpos.kitchen.entity.Kitchen kitchen = entry.getKey();
            List<OrderItem> kitchenItems = entry.getValue();

            com.restaurantpos.kitchen.entity.KitchenTicket ticket = new com.restaurantpos.kitchen.entity.KitchenTicket();
            ticket.setTenant(order.getTenant());
            ticket.setOrder(order);
            ticket.setKitchen(kitchen);
            ticket.setTicketNumber(order.getOrderNumber() + "-" + kitchen.getCode() + "-" + String.format("%03d", System.currentTimeMillis() % 1000));
            ticket.setStatus(com.restaurantpos.kitchen.entity.KitchenTicket.TicketStatus.NEW);
            ticket.setNotes(order.getKitchenNotes() != null ? order.getKitchenNotes() : order.getNotes());
            kitchenTicketRepository.save(ticket);

            // Targeted WebSocket notification: ONLY newly sent items for this specific kitchen
            OrderDto.Response kitchenPayload = buildKitchenOrderPayload(order, kitchen.getId(), kitchenItems);
            wsNotification.notifyKitchenNewTicket(kitchen.getId(), kitchenPayload);
        }

        // 4. Transition newly sent items: NEW -> SENT_TO_KITCHEN
        for (OrderItem item : itemsToSend) {
            item.setKitchenStatus(OrderItem.KitchenStatus.SENT_TO_KITCHEN);
            item.setSentToKitchenAt(now);
            item.setSentQuantity(item.getQuantity());
        }

        // 5. Update order status
        order.setStatus(Order.OrderStatus.IN_PROGRESS);
        if (order.getSentToKitchenAt() == null) {
            order.setSentToKitchenAt(now);
        }

        Order saved = orderRepository.save(order);

        // 6. Notify POS / orders channel
        wsNotification.notifyOrderStatusChanged(tenantId, toResponse(saved));
        if (saved.getTable() != null) {
            wsNotification.notifyTableUpdated(tenantId, toTableResponse(saved.getTable(), saved));
        }

        return toResponse(saved);
    }

    private void routeOrderToKitchens(Order order, Tenant tenant) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }

        // Filter strictly ONLY NEW and non-voided items!
        List<OrderItem> newItems = order.getItems().stream()
                .filter(i -> !i.isVoided() && i.getKitchenStatus() == OrderItem.KitchenStatus.NEW && i.getKitchen() != null)
                .collect(Collectors.toList());

        if (newItems.isEmpty()) {
            return;
        }

        order.setStatus(Order.OrderStatus.IN_PROGRESS);
        Instant now = Instant.now();
        if (order.getSentToKitchenAt() == null) {
            order.setSentToKitchenAt(now);
        }

        // Group items by kitchen
        java.util.Map<com.restaurantpos.kitchen.entity.Kitchen, List<OrderItem>> itemsByKitchen = newItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getKitchen));

        for (java.util.Map.Entry<com.restaurantpos.kitchen.entity.Kitchen, List<OrderItem>> entry : itemsByKitchen.entrySet()) {
            com.restaurantpos.kitchen.entity.Kitchen kitchen = entry.getKey();
            List<OrderItem> kitchenItems = entry.getValue();

            com.restaurantpos.kitchen.entity.KitchenTicket ticket = new com.restaurantpos.kitchen.entity.KitchenTicket();
            ticket.setTenant(tenant);
            ticket.setOrder(order);
            ticket.setKitchen(kitchen);
            ticket.setTicketNumber(order.getOrderNumber() + "-" + kitchen.getCode() + "-" + String.format("%03d", System.currentTimeMillis() % 1000));
            ticket.setStatus(com.restaurantpos.kitchen.entity.KitchenTicket.TicketStatus.NEW);
            ticket.setNotes(order.getKitchenNotes() != null ? order.getKitchenNotes() : order.getNotes());
            kitchenTicketRepository.save(ticket);

            // WebSocket event: Har bir oshxona kanaliga (/topic/kitchen/{kitchenId}) FAQAT YANGI taomlar yuboriladi!
            OrderDto.Response kitchenPayload = buildKitchenOrderPayload(order, kitchen.getId(), kitchenItems);
            wsNotification.notifyKitchenNewTicket(kitchen.getId(), kitchenPayload);
        }

        // Mark routed items as SENT_TO_KITCHEN
        for (OrderItem item : newItems) {
            item.setKitchenStatus(OrderItem.KitchenStatus.SENT_TO_KITCHEN);
            item.setSentToKitchenAt(now);
            item.setSentQuantity(item.getQuantity());
        }

        // Umumiy KDS kanaliga xabar
        wsNotification.notifyNewOrder(tenant.getId(), toResponse(order));
    }

    private OrderDto.Response buildKitchenOrderPayload(Order order, UUID kitchenId, List<OrderItem> kitchenItems) {
        OrderDto.Response resp = toResponse(order);
        List<OrderDto.ItemResponse> filtered = kitchenItems.stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());
        resp.setItems(filtered);
        return resp;
    }

    public OrderDto.Response toKitchenOrderPayload(Order order, UUID kitchenId) {
        OrderDto.Response resp = toResponse(order);
        List<OrderDto.ItemResponse> filtered = order.getItems().stream()
                .filter(i -> i.getKitchen() != null && i.getKitchen().getId().equals(kitchenId))
                .map(this::toItemResponse)
                .collect(Collectors.toList());
        resp.setItems(filtered);
        return resp;
    }

    public OrderDto.ItemResponse toItemResponse(OrderItem i) {
        List<OrderDto.ModifierResponse> mods = Collections.emptyList();
        if (i.getModifiers() != null) {
            mods = i.getModifiers().stream().map(m -> OrderDto.ModifierResponse.builder()
                    .id(m.getId())
                    .modifierId(m.getModifier() != null ? m.getModifier().getId() : null)
                    .modifierName(m.getModifierName())
                    .price(m.getPrice())
                    .quantity(m.getQuantity())
                    .build()
            ).collect(Collectors.toList());
        }

        BigDecimal remaining = (i.getQuantity() != null && i.getSentQuantity() != null)
                ? i.getQuantity().subtract(i.getSentQuantity()).max(BigDecimal.ZERO)
                : (i.getKitchenStatus() == OrderItem.KitchenStatus.NEW ? i.getQuantity() : BigDecimal.ZERO);

        return OrderDto.ItemResponse.builder()
                .id(i.getId())
                .productId(i.getProduct() != null ? i.getProduct().getId() : null)
                .kitchenId(i.getKitchen() != null ? i.getKitchen().getId() : (i.getProduct() != null && i.getProduct().getKitchen() != null ? i.getProduct().getKitchen().getId() : null))
                .kitchenName(i.getKitchen() != null ? i.getKitchen().getName() : (i.getProduct() != null && i.getProduct().getKitchen() != null ? i.getProduct().getKitchen().getName() : null))
                .productName(i.getProductName())
                .productSku(i.getProductSku())
                .quantity(i.getQuantity())
                .unitPrice(i.getUnitPrice())
                .discountAmount(i.getDiscountAmount())
                .subtotal(i.getSubtotal())
                .notes(i.getNotes())
                .kitchenStatus(i.getKitchenStatus() != null ? i.getKitchenStatus().name() : null)
                .sentQuantity(i.getSentQuantity() != null ? i.getSentQuantity() : BigDecimal.ZERO)
                .deliveredQuantity(i.getDeliveredQuantity() != null ? i.getDeliveredQuantity() : BigDecimal.ZERO)
                .cancelledQuantity(i.getCancelledQuantity() != null ? i.getCancelledQuantity() : BigDecimal.ZERO)
                .remainingToSend(remaining)
                .voided(i.isVoided())
                .voidReason(i.getVoidReason())
                .voidedAt(i.getVoidedAt())
                .voidedByName(i.getVoidedBy() != null ? (i.getVoidedBy().getFirstName() + " " + (i.getVoidedBy().getLastName() != null ? i.getVoidedBy().getLastName() : "")).trim() : null)
                .modifiers(mods)
                .build();
    }

    @Transactional
    public CancellationReceiptDto.CancellationResult cancelOrderItem(
            UUID orderId, UUID itemId, UUID userId, UUID tenantId, CancellationReceiptDto.CancelItemRequest request) {
        Order order = orderRepository.findByIdAndTenantIdAndDeletedAtIsNull(orderId, tenantId)
                .orElseThrow(() -> PosException.notFound("Buyurtma topilmadi: " + orderId));

        if (order.getStatus() == Order.OrderStatus.PAID) {
            throw PosException.badRequest("To'langan buyurtma mahsulotini bekor qilib bo'lmaydi!");
        }
        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw PosException.badRequest("Buyurtma allaqachon bekor qilingan!");
        }

        OrderItem item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> PosException.notFound("Mahsulot buyurtmada topilmadi: " + itemId));

        if (item.isVoided()) {
            throw PosException.badRequest("Bu mahsulot allaqachon bekor qilingan!");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> PosException.notFound("Foydalanuvchi topilmadi"));

        String reason = (request.getReason() != null && !request.getReason().isBlank())
                ? request.getReason().trim() : "Mijoz rad etdi";

        BigDecimal cancelQty;
        BigDecimal cancelledAmount;
        boolean isPartial = request.getQuantity() != null &&
                request.getQuantity().compareTo(BigDecimal.ZERO) > 0 &&
                request.getQuantity().compareTo(item.getQuantity()) < 0;

        if (isPartial) {
            cancelQty = request.getQuantity();
            BigDecimal remainingQty = item.getQuantity().subtract(cancelQty);

            item.setQuantity(remainingQty);
            item.calculateSubtotal();

            OrderItem voidedPortion = new OrderItem();
            voidedPortion.setOrder(order);
            voidedPortion.setProduct(item.getProduct());
            voidedPortion.setKitchen(item.getKitchen());
            voidedPortion.setProductName(item.getProductName());
            voidedPortion.setProductSku(item.getProductSku());
            voidedPortion.setQuantity(cancelQty);
            voidedPortion.setUnitPrice(item.getUnitPrice());
            voidedPortion.calculateSubtotal();
            voidedPortion.setVoided(true);
            voidedPortion.setKitchenStatus(OrderItem.KitchenStatus.CANCELLED);
            voidedPortion.setCancelledQuantity(cancelQty);
            voidedPortion.setVoidReason(reason);
            voidedPortion.setVoidedAt(Instant.now());
            voidedPortion.setVoidedBy(user);
            voidedPortion.setNotes(item.getNotes());
            order.getItems().add(voidedPortion);

            cancelledAmount = voidedPortion.getSubtotal();
        } else {
            cancelQty = item.getQuantity();
            cancelledAmount = item.getSubtotal();

            item.setVoided(true);
            item.setKitchenStatus(OrderItem.KitchenStatus.CANCELLED);
            item.setCancelledQuantity(cancelQty);
            item.setVoidReason(reason);
            item.setVoidedAt(Instant.now());
            item.setVoidedBy(user);
        }

        // Recalculate totals (automatically excludes voided items)
        order.recalculateTotals();

        // Check if all items in the order are now cancelled
        boolean allVoided = order.getItems().stream().allMatch(OrderItem::isVoided);
        if (allVoided) {
            order.setStatus(Order.OrderStatus.CANCELLED);
            order.setClosedAt(Instant.now());
            if (order.getTable() != null) {
                RestaurantTable table = order.getTable();
                table.setStatus(RestaurantTable.TableStatus.FREE);
                table.setCurrentOrderId(null);
                tableRepository.save(table);
                wsNotification.notifyTableStatusChanged(tenantId, table.getId(), "FREE");
            }
        }

        Order saved = orderRepository.save(order);
        if (order.getTable() != null) {
            if (allVoided) {
                wsNotification.notifyTableUpdated(tenantId, toTableResponse(order.getTable(), null));
            } else {
                wsNotification.notifyTableUpdated(tenantId, toTableResponse(order.getTable(), saved));
            }
        }

        // Generate Cancellation Receipt in database
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String receiptNumber = "CAN-" + dateStr + "-" + String.format("%04d", CANCEL_COUNTER.incrementAndGet() % 10000) + "-" + String.format("%03d", System.currentTimeMillis() % 1000);

        CancellationReceipt receipt = new CancellationReceipt();
        receipt.setTenant(order.getTenant());
        receipt.setReceiptNumber(receiptNumber);
        receipt.setOrder(saved);
        receipt.setOrderNumber(saved.getOrderNumber());
        receipt.setTable(saved.getTable());
        receipt.setTableName(saved.getTable() != null ? saved.getTable().getName() : (saved.getTable() != null ? "Stol " + saved.getTable().getTableNumber() : null));
        receipt.setCancelledBy(user);
        receipt.setCancelledByName((user.getFirstName() + " " + (user.getLastName() != null ? user.getLastName() : "")).trim());
        receipt.setReason(reason);
        receipt.setItem(item);
        receipt.setItemName(item.getProductName());
        receipt.setCancelledQuantity(cancelQty);
        receipt.setUnitPrice(item.getUnitPrice());
        receipt.setTotalAmount(cancelledAmount);
        receipt.setFullOrder(false);
        CancellationReceipt savedReceipt = cancellationReceiptRepository.save(receipt);

        // Targeted WebSocket Notification to Kitchen (/topic/kitchen/{kitchenId})
        if (item.getKitchen() != null) {
            Map<String, Object> wsPayload = new HashMap<>();
            wsPayload.put("orderId", saved.getId());
            wsPayload.put("orderNumber", saved.getOrderNumber());
            wsPayload.put("tableId", saved.getTable() != null ? saved.getTable().getId() : null);
            wsPayload.put("tableName", saved.getTable() != null ? saved.getTable().getName() : (saved.getTable() != null ? "Stol " + saved.getTable().getTableNumber() : "Noma'lum"));
            wsPayload.put("tableNumber", saved.getTable() != null ? saved.getTable().getTableNumber() : null);
            wsPayload.put("itemId", item.getId());
            wsPayload.put("productName", item.getProductName());
            wsPayload.put("cancelledQuantity", cancelQty);
            wsPayload.put("reason", reason);
            wsPayload.put("cancelledByName", savedReceipt.getCancelledByName());
            wsPayload.put("cancelledAt", savedReceipt.getCreatedAt().toString());
            wsPayload.put("allVoided", allVoided);
            wsPayload.put("receiptNumber", savedReceipt.getReceiptNumber());
            wsPayload.put("order", toKitchenOrderPayload(saved, item.getKitchen().getId()));

            wsNotification.notifyKitchenItemCancelled(item.getKitchen().getId(), wsPayload);
        }

        // Global POS / Orders WebSocket Notification (/topic/orders/{tenantId})
        wsNotification.notifyOrderStatusChanged(tenantId, toResponse(saved));

        return CancellationReceiptDto.CancellationResult.builder()
                .order(toResponse(saved))
                .receipt(toReceiptResponse(savedReceipt))
                .build();
    }

    @Transactional
    public CancellationReceiptDto.CancellationResult cancelOrder(
            UUID orderId, UUID userId, UUID tenantId, CancellationReceiptDto.CancelOrderRequest request) {
        Order order = orderRepository.findByIdAndTenantIdAndDeletedAtIsNull(orderId, tenantId)
                .orElseThrow(() -> PosException.notFound("Buyurtma topilmadi: " + orderId));

        if (order.getStatus() == Order.OrderStatus.PAID) {
            throw PosException.badRequest("To'langan buyurtmani bekor qilib bo'lmaydi!");
        }
        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw PosException.badRequest("Buyurtma allaqachon bekor qilingan!");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> PosException.notFound("Foydalanuvchi topilmadi"));

        String reason = (request.getReason() != null && !request.getReason().isBlank())
                ? request.getReason().trim() : "Mijoz rad etdi";

        BigDecimal totalCancelledAmount = BigDecimal.ZERO;
        Set<com.restaurantpos.kitchen.entity.Kitchen> affectedKitchens = new HashSet<>();

        for (OrderItem item : order.getItems()) {
            if (!item.isVoided()) {
                totalCancelledAmount = totalCancelledAmount.add(item.getSubtotal());
                item.setVoided(true);
                item.setKitchenStatus(OrderItem.KitchenStatus.CANCELLED);
                item.setVoidReason(reason);
                item.setVoidedAt(Instant.now());
                item.setVoidedBy(user);
                if (item.getKitchen() != null) {
                    affectedKitchens.add(item.getKitchen());
                }
            }
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setClosedAt(Instant.now());
        order.recalculateTotals();

        // Release table if linked
        if (order.getTable() != null) {
            RestaurantTable table = order.getTable();
            table.setStatus(RestaurantTable.TableStatus.FREE);
            table.setCurrentOrderId(null);
            tableRepository.save(table);
            wsNotification.notifyTableStatusChanged(tenantId, table.getId(), "FREE");
        }

        Order saved = orderRepository.save(order);
        if (order.getTable() != null) {
            wsNotification.notifyTableUpdated(tenantId, toTableResponse(order.getTable(), null));
        }

        // Generate Cancellation Receipt for full order
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String receiptNumber = "CAN-" + dateStr + "-" + String.format("%04d", CANCEL_COUNTER.incrementAndGet() % 10000) + "-" + String.format("%03d", System.currentTimeMillis() % 1000);

        CancellationReceipt receipt = new CancellationReceipt();
        receipt.setTenant(order.getTenant());
        receipt.setReceiptNumber(receiptNumber);
        receipt.setOrder(saved);
        receipt.setOrderNumber(saved.getOrderNumber());
        receipt.setTable(saved.getTable());
        receipt.setTableName(saved.getTable() != null ? saved.getTable().getName() : (saved.getTable() != null ? "Stol " + saved.getTable().getTableNumber() : null));
        receipt.setCancelledBy(user);
        receipt.setCancelledByName((user.getFirstName() + " " + (user.getLastName() != null ? user.getLastName() : "")).trim());
        receipt.setReason(reason);
        receipt.setItemName("BUTUN BUYURTMA");
        receipt.setCancelledQuantity(BigDecimal.valueOf(order.getItems().size()));
        receipt.setTotalAmount(totalCancelledAmount);
        receipt.setFullOrder(true);
        CancellationReceipt savedReceipt = cancellationReceiptRepository.save(receipt);

        // Send targeted WebSocket notification to each affected kitchen station
        for (com.restaurantpos.kitchen.entity.Kitchen kitchen : affectedKitchens) {
            Map<String, Object> wsPayload = new HashMap<>();
            wsPayload.put("orderId", saved.getId());
            wsPayload.put("orderNumber", saved.getOrderNumber());
            wsPayload.put("tableId", saved.getTable() != null ? saved.getTable().getId() : null);
            wsPayload.put("tableName", saved.getTable() != null ? saved.getTable().getName() : (saved.getTable() != null ? "Stol " + saved.getTable().getTableNumber() : "Noma'lum"));
            wsPayload.put("tableNumber", saved.getTable() != null ? saved.getTable().getTableNumber() : null);
            wsPayload.put("reason", reason);
            wsPayload.put("cancelledByName", savedReceipt.getCancelledByName());
            wsPayload.put("cancelledAt", savedReceipt.getCreatedAt().toString());
            wsPayload.put("allVoided", true);
            wsPayload.put("receiptNumber", savedReceipt.getReceiptNumber());
            wsPayload.put("order", toKitchenOrderPayload(saved, kitchen.getId()));

            wsNotification.notifyKitchenOrderCancelled(kitchen.getId(), wsPayload);
        }

        // Global POS / Orders WebSocket Notification
        wsNotification.notifyOrderStatusChanged(tenantId, toResponse(saved));

        return CancellationReceiptDto.CancellationResult.builder()
                .order(toResponse(saved))
                .receipt(toReceiptResponse(savedReceipt))
                .build();
    }

    @Transactional(readOnly = true)
    public List<CancellationReceiptDto.Response> getCancellationReceipts(UUID orderId, UUID tenantId) {
        return cancellationReceiptRepository.findByTenantIdAndOrderIdOrderByCreatedAtDesc(tenantId, orderId).stream()
                .map(this::toReceiptResponse)
                .collect(Collectors.toList());
    }

    public CancellationReceiptDto.Response toReceiptResponse(CancellationReceipt r) {
        return CancellationReceiptDto.Response.builder()
                .id(r.getId())
                .receiptNumber(r.getReceiptNumber())
                .orderId(r.getOrder() != null ? r.getOrder().getId() : null)
                .orderNumber(r.getOrderNumber())
                .tableId(r.getTable() != null ? r.getTable().getId() : null)
                .tableName(r.getTableName())
                .cancelledById(r.getCancelledBy() != null ? r.getCancelledBy().getId() : null)
                .cancelledByName(r.getCancelledByName())
                .reason(r.getReason())
                .itemId(r.getItem() != null ? r.getItem().getId() : null)
                .itemName(r.getItemName())
                .cancelledQuantity(r.getCancelledQuantity())
                .unitPrice(r.getUnitPrice())
                .totalAmount(r.getTotalAmount())
                .fullOrder(r.isFullOrder())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
