package com.restaurantpos.tables.service;

import com.restaurantpos.common.exception.PosException;
import com.restaurantpos.common.websocket.WebSocketNotificationService;
import com.restaurantpos.orders.entity.Order;
import com.restaurantpos.orders.repository.OrderRepository;
import com.restaurantpos.tables.dto.TableDto;
import com.restaurantpos.tables.entity.RestaurantTable;
import com.restaurantpos.tables.entity.TableZone;
import com.restaurantpos.tables.repository.RestaurantTableRepository;
import com.restaurantpos.tables.repository.TableZoneRepository;
import com.restaurantpos.tenants.entity.Tenant;
import com.restaurantpos.tenants.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.restaurantpos.orders.entity.OrderItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TableService {

    private final RestaurantTableRepository tableRepository;
    private final TableZoneRepository zoneRepository;
    private final TenantRepository tenantRepository;
    private final OrderRepository orderRepository;
    private final com.restaurantpos.users.repository.UserRepository userRepository;
    private final com.restaurantpos.shifts.repository.ShiftRepository shiftRepository;
    private final WebSocketNotificationService wsNotification;

    private static final java.util.concurrent.atomic.AtomicInteger ORDER_COUNTER = new java.util.concurrent.atomic.AtomicInteger(500);


    @Transactional(readOnly = true)
    public List<TableDto.ZoneResponse> getZones(UUID tenantId) {
        return zoneRepository.findByTenantIdAndDeletedAtIsNullOrderBySortOrderAsc(tenantId)
                .stream().map(z -> TableDto.ZoneResponse.builder()
                        .id(z.getId())
                        .name(z.getName())
                        .description(z.getDescription())
                        .sortOrder(z.getSortOrder())
                        .active(z.isActive())
                        .build()
                ).collect(Collectors.toList());
    }

    @Transactional
    public TableDto.ZoneResponse createZone(UUID tenantId, TableDto.CreateZoneRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> PosException.notFound("Tenant not found"));

        String zoneName = request.getName().trim();
        zoneRepository.findByTenantIdAndNameIgnoreCaseAndDeletedAtIsNull(tenantId, zoneName)
                .ifPresent(z -> {
                    throw PosException.badRequest("Bu nomdagi zona allaqachon mavjud: " + zoneName);
                });

        TableZone zone = new TableZone();
        zone.setTenant(tenant);
        zone.setName(zoneName);
        zone.setDescription(request.getDescription());
        zone.setSortOrder(request.getSortOrder());
        zone.setActive(true);

        TableZone saved = zoneRepository.save(zone);
        return TableDto.ZoneResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .sortOrder(saved.getSortOrder())
                .active(saved.isActive())
                .build();
    }

    @Transactional
    public List<TableDto.Response> getTables(UUID tenantId, UUID zoneId, com.restaurantpos.auth.security.UserPrincipal currentUser) {
        List<RestaurantTable> tables = zoneId != null
                ? tableRepository.findByTenantIdAndZoneIdAndDeletedAtIsNullOrderByTableNumberAsc(tenantId, zoneId)
                : tableRepository.findByTenantIdAndDeletedAtIsNullOrderByTableNumberAsc(tenantId);

        List<UUID> orderIds = tables.stream()
                .map(RestaurantTable::getCurrentOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, Order> ordersMap = java.util.Collections.emptyMap();
        if (!orderIds.isEmpty()) {
            ordersMap = orderRepository.findAllById(orderIds).stream()
                    .collect(Collectors.toMap(Order::getId, o -> o));
        }

        Map<UUID, Order> finalOrdersMap = ordersMap;
        return tables.stream()
                .map(t -> {
                    Order order = t.getCurrentOrderId() != null ? finalOrdersMap.get(t.getCurrentOrderId()) : null;
                    // Auto-heal: Agar stol band deb belgilangan bo'lsa, lekin buyurtma mavjud bo'lmasa, bekor qilingan bo'lsa yoki faol taomi bo'lmasa, stolni darhol BO'SH (FREE) holatga qaytarish
                    if (t.getStatus() == RestaurantTable.TableStatus.OCCUPIED) {
                        boolean hasActiveItems = order != null && order.getItems() != null &&
                                order.getItems().stream().anyMatch(i -> !i.isVoided() && i.getKitchenStatus() != OrderItem.KitchenStatus.CANCELLED);
                        boolean isOrderActive = order != null && order.getStatus() != Order.OrderStatus.CANCELLED && order.getStatus() != Order.OrderStatus.PAID;

                        if (!isOrderActive || !hasActiveItems) {
                            if (order != null && order.getStatus() == Order.OrderStatus.OPEN && (order.getItems() == null || order.getItems().isEmpty())) {
                                order.setStatus(Order.OrderStatus.CANCELLED);
                                order.setClosedAt(Instant.now());
                                orderRepository.save(order);
                            }
                            t.setStatus(RestaurantTable.TableStatus.FREE);
                            t.setCurrentOrderId(null);
                            t.setWaiter(null);
                            tableRepository.save(t);
                            order = null;
                        }
                    }
                    return toResponse(t, order, currentUser);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public TableDto.Response getTableById(UUID id, UUID tenantId, com.restaurantpos.auth.security.UserPrincipal currentUser) {
        RestaurantTable table = tableRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Table not found: " + id));

        // Data Isolation: If table is occupied by another waiter, throw 403 Forbidden!
        if (currentUser != null && currentUser.isWaiter()) {
            if (table.getWaiter() != null && !table.getWaiter().getId().equals(currentUser.getUserId())) {
                throw PosException.forbidden("Bu stol boshqa ofitsantga biriktirilgan.");
            }
        }

        Order order = table.getCurrentOrderId() != null ? orderRepository.findById(table.getCurrentOrderId()).orElse(null) : null;
        if (table.getStatus() == RestaurantTable.TableStatus.OCCUPIED) {
            boolean hasActiveItems = order != null && order.getItems() != null &&
                    order.getItems().stream().anyMatch(i -> !i.isVoided() && i.getKitchenStatus() != OrderItem.KitchenStatus.CANCELLED);
            boolean isOrderActive = order != null && order.getStatus() != Order.OrderStatus.CANCELLED && order.getStatus() != Order.OrderStatus.PAID;

            if (!isOrderActive || !hasActiveItems) {
                if (order != null && order.getStatus() == Order.OrderStatus.OPEN && (order.getItems() == null || order.getItems().isEmpty())) {
                    order.setStatus(Order.OrderStatus.CANCELLED);
                    order.setClosedAt(Instant.now());
                    orderRepository.save(order);
                }
                table.setStatus(RestaurantTable.TableStatus.FREE);
                table.setCurrentOrderId(null);
                table.setWaiter(null);
                table = tableRepository.save(table);
                order = null;
            }
        }
        return toResponse(table, order, currentUser);
    }

    @Transactional
    public TableDto.Response releaseTable(UUID id, UUID tenantId, com.restaurantpos.auth.security.UserPrincipal currentUser) {
        RestaurantTable table = tableRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Table not found: " + id));

        if (currentUser != null && currentUser.isWaiter()) {
            if (table.getWaiter() != null && !table.getWaiter().getId().equals(currentUser.getUserId())) {
                throw PosException.forbidden("Bu stol boshqa ofitsantga biriktirilgan.");
            }
        }

        if (table.getCurrentOrderId() != null) {
            Order order = orderRepository.findById(table.getCurrentOrderId()).orElse(null);
            if (order != null && order.getStatus() != Order.OrderStatus.PAID) {
                order.setStatus(Order.OrderStatus.CANCELLED);
                order.setClosedAt(Instant.now());
                orderRepository.save(order);
            }
        }

        table.setStatus(RestaurantTable.TableStatus.FREE);
        table.setCurrentOrderId(null);
        table.setWaiter(null);
        RestaurantTable saved = tableRepository.save(table);

        TableDto.Response res = toResponse(saved, null, currentUser);
        wsNotification.notifyTableUpdated(tenantId, res);
        return res;
    }

    @Transactional
    public TableDto.Response occupyTable(UUID id, UUID tenantId, com.restaurantpos.auth.security.UserPrincipal currentUser) {
        // Pessimistic lock to prevent race conditions when two waiters attempt to open the same table concurrently
        RestaurantTable table = tableRepository.findByIdWithLock(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Table not found: " + id));

        if (currentUser != null && currentUser.isWaiter()) {
            if (table.getWaiter() != null && !table.getWaiter().getId().equals(currentUser.getUserId())) {
                throw PosException.forbidden("Bu stol boshqa ofitsantga biriktirilgan.");
            }
        }

        // If table already has an active order owned by this waiter, return it
        if (table.getStatus() == RestaurantTable.TableStatus.OCCUPIED && table.getCurrentOrderId() != null) {
            Order existingOrder = orderRepository.findById(table.getCurrentOrderId()).orElse(null);
            return toResponse(table, existingOrder, currentUser);
        }

        com.restaurantpos.users.entity.User waiter = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> PosException.notFound("Ofitsiant topilmadi"));

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> PosException.notFound("Tenant topilmadi"));

        com.restaurantpos.shifts.entity.Shift currentShift = shiftRepository
                .findByTenantIdAndStatus(tenantId, com.restaurantpos.shifts.entity.Shift.ShiftStatus.OPEN)
                .orElse(null);

        // 1. Create active order for table
        Order order = new Order();
        order.setTenant(tenant);
        order.setTable(table);
        order.setWaiter(waiter);
        order.setShift(currentShift);
        order.setOrderType(Order.OrderType.DINE_IN);
        order.setStatus(Order.OrderStatus.OPEN);

        String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        order.setOrderNumber("ORD-" + dateStr + "-" + String.format("%04d", ORDER_COUNTER.incrementAndGet() % 10000));
        order.recalculateTotals();

        Order savedOrder = orderRepository.save(order);

        // 2. Bind table to current waiter and set to OCCUPIED
        table.setStatus(RestaurantTable.TableStatus.OCCUPIED);
        table.setWaiter(waiter);
        table.setCurrentOrderId(savedOrder.getId());
        RestaurantTable savedTable = tableRepository.save(table);

        TableDto.Response res = toResponse(savedTable, savedOrder, currentUser);
        wsNotification.notifyTableUpdated(tenantId, res);
        return res;
    }


    @Transactional
    public TableDto.Response updateTableStatus(UUID id, UUID tenantId, TableDto.UpdateStatusRequest request) {
        return updateTableStatus(id, tenantId, null, request);
    }

    @Transactional
    public TableDto.Response updateTableStatus(UUID id, UUID tenantId, com.restaurantpos.auth.security.UserPrincipal currentUser, TableDto.UpdateStatusRequest request) {
        RestaurantTable table = tableRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Table not found: " + id));

        if (currentUser != null && currentUser.isWaiter()) {
            if (table.getWaiter() != null && !table.getWaiter().getId().equals(currentUser.getUserId())) {
                throw PosException.forbidden("Bu stol boshqa ofitsantga biriktirilgan.");
            }
        }

        table.setStatus(RestaurantTable.TableStatus.valueOf(request.getStatus().toUpperCase()));
        if (currentUser == null || !currentUser.isWaiter()) {
            table.setCurrentOrderId(request.getCurrentOrderId());
        }

        RestaurantTable saved = tableRepository.save(table);
        Order order = saved.getCurrentOrderId() != null ? orderRepository.findById(saved.getCurrentOrderId()).orElse(null) : null;
        TableDto.Response res = toResponse(saved, order, currentUser);
        wsNotification.notifyTableUpdated(tenantId, res);
        return res;
    }

    @Transactional
    public TableDto.Response updateTableLayout(UUID id, UUID tenantId, TableDto.UpdateLayoutRequest request) {
        RestaurantTable table = tableRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Table not found: " + id));

        table.setPosX(request.getPosX());
        table.setPosY(request.getPosY());
        table.setWidth(request.getWidth());
        table.setHeight(request.getHeight());

        RestaurantTable saved = tableRepository.save(table);
        return toResponse(saved);
    }

    @Transactional
    public TableDto.Response createTable(UUID tenantId, TableDto.CreateRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> PosException.notFound("Tenant not found"));

        TableZone zone = null;
        if (request.getZoneId() != null) {
            zone = zoneRepository.findByIdAndTenantIdAndDeletedAtIsNull(request.getZoneId(), tenantId)
                    .orElseThrow(() -> PosException.badRequest("Tanlangan stol zonasi topilmadi"));
        } else if (request.getZoneName() != null && !request.getZoneName().trim().isBlank()) {
            String zName = request.getZoneName().trim();
            zone = zoneRepository.findByTenantIdAndNameIgnoreCaseAndDeletedAtIsNull(tenantId, zName)
                    .orElseGet(() -> {
                        TableZone newZ = new TableZone();
                        newZ.setTenant(tenant);
                        newZ.setName(zName);
                        newZ.setDescription("Stol qo'shishda yaratilgan zona");
                        newZ.setSortOrder(99);
                        newZ.setActive(true);
                        return zoneRepository.save(newZ);
                    });
        } else {
            throw PosException.badRequest("Stol joylashuvi (zona: Zal, Ko'cha, Ayvon, Podval...) tanlanishi shart!");
        }

        RestaurantTable table = new RestaurantTable();
        table.setTenant(tenant);
        table.setZone(zone);
        table.setTableNumber(request.getTableNumber());
        table.setName(request.getName() != null ? request.getName() : "Stol " + request.getTableNumber());
        table.setCapacity(request.getCapacity());
        table.setShape(request.getShape() != null ? request.getShape() : "rectangle");
        table.setPosX(request.getPosX());
        table.setPosY(request.getPosY());
        table.setWidth(request.getWidth());
        table.setHeight(request.getHeight());
        table.setStatus(RestaurantTable.TableStatus.FREE);
        table.setActive(true);

        RestaurantTable saved = tableRepository.save(table);
        return toResponse(saved);
    }

    @Transactional
    public TableDto.Response updateTable(UUID id, UUID tenantId, TableDto.UpdateRequest request) {
        RestaurantTable table = tableRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Table not found"));

        if (request.getZoneId() != null) {
            TableZone zone = zoneRepository.findByIdAndTenantIdAndDeletedAtIsNull(request.getZoneId(), tenantId)
                    .orElseThrow(() -> PosException.badRequest("Tanlangan stol zonasi topilmadi"));
            table.setZone(zone);
        }
        if (request.getTableNumber() != null && !request.getTableNumber().isBlank()) {
            table.setTableNumber(request.getTableNumber());
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            table.setName(request.getName());
        }
        if (request.getCapacity() > 0) {
            table.setCapacity(request.getCapacity());
        }
        if (request.getShape() != null) {
            table.setShape(request.getShape());
        }
        if (request.getActive() != null) {
            table.setActive(request.getActive());
        }

        RestaurantTable saved = tableRepository.save(table);
        return toResponse(saved);
    }

    @Transactional
    public void deleteTable(UUID id, UUID tenantId) {
        RestaurantTable table = tableRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Table not found"));
        table.setDeletedAt(java.time.Instant.now());
        table.setActive(false);
        tableRepository.save(table);
    }

    public TableDto.Response toResponse(RestaurantTable table, Order activeOrder, com.restaurantpos.auth.security.UserPrincipal currentUser) {
        String activeOrderNumber = null;
        Integer itemCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        boolean isOtherWaiter = false;
        if (currentUser != null && currentUser.isWaiter()) {
            if (table.getWaiter() != null && !table.getWaiter().getId().equals(currentUser.getUserId())) {
                isOtherWaiter = true;
            }
        }

        UUID waiterId = table.getWaiter() != null ? table.getWaiter().getId() : null;
        String waiterName = null;
        Boolean myTable = null;

        if (table.getWaiter() != null) {
            if (isOtherWaiter) {
                waiterName = "Boshqa ofitsiant";
                myTable = false;
            } else {
                waiterName = (table.getWaiter().getFirstName() + " " + (table.getWaiter().getLastName() != null ? table.getWaiter().getLastName() : "")).trim();
                myTable = true;
            }
        } else {
            myTable = true; // Free table
        }

        UUID currentOrderId = table.getCurrentOrderId();

        if (isOtherWaiter) {
            // Data Isolation: Sanitize sensitive order info for other waiters!
            activeOrderNumber = null;
            itemCount = 0;
            totalAmount = BigDecimal.ZERO;
            currentOrderId = null;
        } else if (activeOrder != null && activeOrder.getStatus() != Order.OrderStatus.PAID && activeOrder.getStatus() != Order.OrderStatus.CANCELLED) {
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
                .currentOrderId(currentOrderId)
                .activeOrderNumber(activeOrderNumber)
                .itemCount(itemCount)
                .totalAmount(totalAmount)
                .waiterId(waiterId)
                .waiterName(waiterName)
                .myTable(myTable)
                .active(table.isActive())
                .build();
    }

    public TableDto.Response toResponse(RestaurantTable table, Order activeOrder) {
        return toResponse(table, activeOrder, null);
    }

    public TableDto.Response toResponse(RestaurantTable table) {
        Order order = table.getCurrentOrderId() != null ? orderRepository.findById(table.getCurrentOrderId()).orElse(null) : null;
        return toResponse(table, order, null);
    }
}
