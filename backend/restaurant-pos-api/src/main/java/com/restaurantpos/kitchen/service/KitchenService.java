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
    private final com.restaurantpos.users.repository.EmployeeKitchenRepository employeeKitchenRepository;
    private final com.restaurantpos.users.repository.UserRepository userRepository;
    private final com.restaurantpos.users.service.UserService userService;
    private final com.restaurantpos.products.service.CategoryService categoryService;

    @Transactional(readOnly = true)
    public List<KitchenDto.Response> getKitchens(UUID tenantId) {
        List<Kitchen> kitchens = kitchenRepository.findByTenantIdAndDeletedAtIsNullOrderBySortOrderAsc(tenantId);
        List<com.restaurantpos.printers.entity.PrinterAssignment> assignments = assignmentRepository.findByTenantIdAndDeletedAtIsNull(tenantId);

        Map<UUID, com.restaurantpos.printers.entity.PrinterAssignment> primaryByKitchen = assignments.stream()
                .filter(a -> a.getKitchen() != null && a.isActive() && a.isPrimary())
                .collect(Collectors.toMap(a -> a.getKitchen().getId(), a -> a, (k1, k2) -> k1));

        return kitchens.stream().map(k -> toResponseWithCounts(k, tenantId, primaryByKitchen.get(k.getId()))).collect(Collectors.toList());
    }

    private KitchenDto.Response toResponseWithCounts(Kitchen k, UUID tenantId, com.restaurantpos.printers.entity.PrinterAssignment pa) {
        UUID printerId = pa != null ? pa.getPrinter().getId() : null;
        String printerName = pa != null ? pa.getPrinter().getName() : null;
        String printerStatus = pa != null ? pa.getPrinter().getStatus().name() : null;

        int employeesCount = (int) employeeKitchenRepository.countByKitchenId(k.getId());
        int categoriesCount = (int) categoryRepository.countByTenantIdAndKitchenIdAndDeletedAtIsNull(tenantId, k.getId());

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
                .assignedEmployeesCount(employeesCount)
                .assignedCategoriesCount(categoriesCount)
                .createdAt(k.getCreatedAt())
                .updatedAt(k.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public KitchenDto.Response getKitchenById(UUID tenantId, UUID kitchenId) {
        Kitchen kitchen = kitchenRepository.findByIdAndTenantIdAndDeletedAtIsNull(kitchenId, tenantId)
                .orElseThrow(() -> PosException.notFound("Oshxona topilmadi: " + kitchenId));
        com.restaurantpos.printers.entity.PrinterAssignment pa = assignmentRepository
                .findByTenantIdAndKitchenIdAndActiveTrueAndDeletedAtIsNull(tenantId, kitchenId)
                .stream().filter(com.restaurantpos.printers.entity.PrinterAssignment::isPrimary).findFirst().orElse(null);
        return toResponseWithCounts(kitchen, tenantId, pa);
    }

    @Transactional(readOnly = true)
    public List<KitchenDto.Response> getActiveKitchens(UUID tenantId) {
        return getKitchens(tenantId).stream()
                .filter(KitchenDto.Response::isActive)
                .collect(Collectors.toList());
    }

    @Transactional
    public KitchenDto.Response createKitchen(UUID tenantId, KitchenDto.CreateRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> PosException.notFound("Tenant not found"));

        String name = request.getName().trim();
        if (kitchenRepository.existsByTenantIdAndNameIgnoreCaseAndDeletedAtIsNull(tenantId, name)) {
            throw PosException.badRequest("Bu nomdagi oshxona allaqachon mavjud: " + name);
        }

        String code = request.getCode() != null && !request.getCode().isBlank()
                ? request.getCode().trim().toUpperCase()
                : generateCodeFromName(name);

        String baseCode = code;
        int attempts = 0;
        while (kitchenRepository.existsByTenantIdAndCodeIgnoreCaseAndDeletedAtIsNull(tenantId, code)) {
            attempts++;
            code = baseCode + "-" + ((int) (Math.random() * 9000) + 1000);
            if (attempts > 50) {
                code = baseCode + "-" + (System.currentTimeMillis() % 100000);
                break;
            }
        }

        Kitchen kitchen = new Kitchen();
        kitchen.setTenant(tenant);
        kitchen.setName(name);
        kitchen.setCode(code);
        kitchen.setDescription(request.getDescription());
        kitchen.setSortOrder(request.getSortOrder() > 0 ? request.getSortOrder() : getNextSortOrder(tenantId));
        kitchen.setActive(request.getActive() != null ? request.getActive() : true);
        if (request.getColor() != null && !request.getColor().isBlank()) kitchen.setColor(request.getColor());
        if (request.getAutoPrint() != null) kitchen.setAutoPrint(request.getAutoPrint());
        if (request.getSoundNotification() != null) kitchen.setSoundNotification(request.getSoundNotification());
        if (request.getPreparationTimeMinutes() != null) kitchen.setPreparationTimeMinutes(request.getPreparationTimeMinutes());

        Kitchen saved = kitchenRepository.save(kitchen);

        if (request.getPrinterId() != null) {
            assignKitchenPrinter(tenant, saved, request.getPrinterId());
        }

        // Assign categories if provided (forceReassign=true because admin is explicitly creating a kitchen with categories)
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            assignCategoriesToKitchen(tenantId, saved.getId(),
                    new com.restaurantpos.kitchen.dto.KitchenDto.AssignCategoriesRequest(request.getCategoryIds(), true));
        }

        return getKitchenById(tenantId, saved.getId());
    }

    @Transactional
    public KitchenDto.Response updateKitchen(UUID tenantId, UUID kitchenId, KitchenDto.UpdateRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> PosException.notFound("Tenant not found"));

        Kitchen kitchen = kitchenRepository.findByIdAndTenantIdAndDeletedAtIsNull(kitchenId, tenantId)
                .orElseThrow(() -> PosException.notFound("Oshxona topilmadi: " + kitchenId));

        if (request.getName() != null && !request.getName().isBlank()) {
            String newName = request.getName().trim();
            if (kitchenRepository.existsByTenantIdAndNameIgnoreCaseAndIdNotAndDeletedAtIsNull(tenantId, newName, kitchenId)) {
                throw PosException.badRequest("Bu nomdagi oshxona allaqachon mavjud: " + newName);
            }
            kitchen.setName(newName);
        }
        if (request.getCode() != null && !request.getCode().isBlank()) {
            String newCode = request.getCode().trim().toUpperCase();
            if (kitchenRepository.existsByTenantIdAndCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(tenantId, newCode, kitchenId)) {
                throw PosException.badRequest("Bu kodli oshxona allaqachon mavjud: " + newCode);
            }
            kitchen.setCode(newCode);
        }
        if (request.getDescription() != null) kitchen.setDescription(request.getDescription());
        if (request.getSortOrder() != null) kitchen.setSortOrder(request.getSortOrder());
        if (request.getActive() != null) kitchen.setActive(request.getActive());
        if (request.getColor() != null) kitchen.setColor(request.getColor());
        if (request.getAutoPrint() != null) kitchen.setAutoPrint(request.getAutoPrint());
        if (request.getSoundNotification() != null) kitchen.setSoundNotification(request.getSoundNotification());
        if (request.getPreparationTimeMinutes() != null) kitchen.setPreparationTimeMinutes(request.getPreparationTimeMinutes());
        kitchen.setUpdatedAt(Instant.now());

        Kitchen saved = kitchenRepository.save(kitchen);

        if (request.getPrinterId() != null) {
            assignKitchenPrinter(tenant, saved, request.getPrinterId());
        }

        // Update category assignments if provided (null = don't change; empty list = remove all)
        if (request.getCategoryIds() != null) {
            assignCategoriesToKitchen(tenantId, saved.getId(),
                    new com.restaurantpos.kitchen.dto.KitchenDto.AssignCategoriesRequest(request.getCategoryIds(), true));
        }

        return getKitchenById(tenantId, saved.getId());
    }

    @Transactional
    public KitchenDto.Response toggleKitchenStatus(UUID tenantId, UUID kitchenId, boolean active) {
        Kitchen kitchen = kitchenRepository.findByIdAndTenantIdAndDeletedAtIsNull(kitchenId, tenantId)
                .orElseThrow(() -> PosException.notFound("Oshxona topilmadi: " + kitchenId));
        kitchen.setActive(active);
        kitchen.setUpdatedAt(Instant.now());
        kitchenRepository.save(kitchen);
        return getKitchenById(tenantId, kitchenId);
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

    @Transactional(readOnly = true)
    public List<KitchenDto.Response> getKitchensFiltered(UUID tenantId, String search, String status) {
        List<KitchenDto.Response> all = getKitchens(tenantId);
        return all.stream()
                .filter(k -> {
                    if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
                        if (status.equalsIgnoreCase("ACTIVE") && !k.isActive()) return false;
                        if (status.equalsIgnoreCase("INACTIVE") && k.isActive()) return false;
                    }
                    if (search != null && !search.isBlank()) {
                        String s = search.trim().toLowerCase();
                        boolean matchesName = k.getName() != null && k.getName().toLowerCase().contains(s);
                        boolean matchesDesc = k.getDescription() != null && k.getDescription().toLowerCase().contains(s);
                        boolean matchesCode = k.getCode() != null && k.getCode().toLowerCase().contains(s);
                        return matchesName || matchesDesc || matchesCode;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<com.restaurantpos.users.dto.UserDto.Response> getKitchenEmployees(UUID tenantId, UUID kitchenId) {
        Kitchen kitchen = kitchenRepository.findByIdAndTenantIdAndDeletedAtIsNull(kitchenId, tenantId)
                .orElseThrow(() -> PosException.notFound("Oshxona topilmadi: " + kitchenId));
        List<com.restaurantpos.users.entity.EmployeeKitchen> assignments = employeeKitchenRepository.findByKitchenId(kitchen.getId());
        return assignments.stream()
                .map(com.restaurantpos.users.entity.EmployeeKitchen::getEmployee)
                .filter(u -> u != null && u.getDeletedAt() == null && u.getTenant().getId().equals(tenantId))
                .distinct()
                .map(userService::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<com.restaurantpos.products.dto.CategoryDto.Response> getKitchenCategories(UUID tenantId, UUID kitchenId) {
        Kitchen kitchen = kitchenRepository.findByIdAndTenantIdAndDeletedAtIsNull(kitchenId, tenantId)
                .orElseThrow(() -> PosException.notFound("Oshxona topilmadi: " + kitchenId));
        return categoryService.getAllCategories(tenantId, false, kitchen.getId());
    }

    /**
     * Assign a list of categories to the given kitchen.
     * Business rule: 1 Category → FAQAT 1 Kitchen.
     * - If forceReassign=true:  categories already in another kitchen are MOVED here.
     * - If forceReassign=false: conflicting categories are SKIPPED and returned in the response.
     */
    @Transactional
    public com.restaurantpos.kitchen.dto.KitchenDto.AssignCategoriesResponse assignCategoriesToKitchen(
            UUID tenantId, UUID kitchenId,
            com.restaurantpos.kitchen.dto.KitchenDto.AssignCategoriesRequest request) {

        Kitchen kitchen = kitchenRepository.findByIdAndTenantIdAndDeletedAtIsNull(kitchenId, tenantId)
                .orElseThrow(() -> PosException.notFound("Oshxona topilmadi: " + kitchenId));

        List<UUID> requestedIds = request.getCategoryIds() != null ? request.getCategoryIds() : java.util.Collections.emptyList();

        // Fetch all requested categories that belong to this tenant
        List<com.restaurantpos.products.entity.Category> requestedCategories =
                categoryRepository.findByTenantIdAndIdInAndDeletedAtIsNull(tenantId, requestedIds);

        List<String> conflictNames = new java.util.ArrayList<>();
        int assignedCount = 0;

        for (com.restaurantpos.products.entity.Category cat : requestedCategories) {
            boolean belongsToAnotherKitchen = cat.getKitchen() != null
                    && !cat.getKitchen().getId().equals(kitchenId);

            if (belongsToAnotherKitchen && !request.isForceReassign()) {
                // Skip conflicting category — report back to caller
                conflictNames.add(cat.getName() + " (hozir: " + cat.getKitchen().getName() + ")");
            } else {
                // Assign (or re-assign) this category to the target kitchen
                cat.setKitchen(kitchen);
                categoryRepository.save(cat);
                assignedCount++;
            }
        }

        String message;
        if (conflictNames.isEmpty()) {
            message = assignedCount + " ta kategoriya muvaffaqiyatli biriktirildi";
        } else {
            message = assignedCount + " ta kategoriya biriktirildi, " + conflictNames.size() + " ta kategoriya allaqachon boshqa oshxonaga tegishli (o'tkazilmadi)";
        }

        return com.restaurantpos.kitchen.dto.KitchenDto.AssignCategoriesResponse.builder()
                .assignedCount(assignedCount)
                .conflictCount(conflictNames.size())
                .conflictingCategoryNames(conflictNames)
                .message(message)
                .build();
    }

    @Transactional
    public void assignKitchenEmployees(UUID tenantId, UUID kitchenId, List<UUID> employeeIds) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> PosException.notFound("Tenant not found"));
        Kitchen kitchen = kitchenRepository.findByIdAndTenantIdAndDeletedAtIsNull(kitchenId, tenantId)
                .orElseThrow(() -> PosException.notFound("Oshxona topilmadi: " + kitchenId));

        if (!kitchen.isActive() && employeeIds != null && !employeeIds.isEmpty()) {
            throw PosException.badRequest("Faol bo'lmagan (INACTIVE) oshxonaga xodimlarni biriktirib bo'lmaydi.");
        }

        List<com.restaurantpos.users.entity.EmployeeKitchen> existing = employeeKitchenRepository.findByKitchenId(kitchen.getId());
        Set<UUID> targetEmployeeIds = employeeIds != null ? new java.util.HashSet<>(employeeIds) : Collections.emptySet();

        for (com.restaurantpos.users.entity.EmployeeKitchen ek : existing) {
            if (!targetEmployeeIds.contains(ek.getEmployee().getId())) {
                com.restaurantpos.users.entity.User emp = ek.getEmployee();
                boolean isKitchenRole = emp.getRoles().stream().anyMatch(r -> "KITCHEN".equalsIgnoreCase(r.getName()));
                if (isKitchenRole) {
                    long otherKitchensCount = emp.getEmployeeKitchens().stream()
                            .filter(otherEk -> !otherEk.getKitchen().getId().equals(kitchen.getId()) && otherEk.getKitchen().isActive())
                            .count();
                    if (otherKitchensCount == 0) {
                        throw PosException.badRequest("Xodim '" + emp.getFullName() + "' KITCHEN rolida va kamida bitta faol oshxonaga ega bo'lishi shart!");
                    }
                }
                emp.getEmployeeKitchens().remove(ek);
                employeeKitchenRepository.delete(ek);
                if (emp.getKitchen() != null && emp.getKitchen().getId().equals(kitchen.getId())) {
                    emp.setKitchen(emp.getEmployeeKitchens().isEmpty() ? null : emp.getEmployeeKitchens().iterator().next().getKitchen());
                    userRepository.save(emp);
                }
            }
        }

        Set<UUID> currentAssignedEmpIds = existing.stream().map(ek -> ek.getEmployee().getId()).collect(Collectors.toSet());
        for (UUID empId : targetEmployeeIds) {
            if (!currentAssignedEmpIds.contains(empId)) {
                com.restaurantpos.users.entity.User emp = userRepository.findByIdAndDeletedAtIsNull(empId)
                        .filter(u -> u.getTenant().getId().equals(tenantId))
                        .orElseThrow(() -> PosException.notFound("Xodim topilmadi: " + empId));
                com.restaurantpos.users.entity.EmployeeKitchen ek = new com.restaurantpos.users.entity.EmployeeKitchen(tenant, emp, kitchen);
                emp.getEmployeeKitchens().add(ek);
                employeeKitchenRepository.save(ek);
                if (emp.getKitchen() == null) {
                    emp.setKitchen(kitchen);
                }
                userRepository.save(emp);
            }
        }
    }

    @Transactional
    public void deleteKitchen(UUID tenantId, UUID kitchenId) {
        Kitchen kitchen = kitchenRepository.findByIdAndTenantIdAndDeletedAtIsNull(kitchenId, tenantId)
                .orElseThrow(() -> PosException.notFound("Oshxona topilmadi: " + kitchenId));

        long categoryCount = categoryRepository.countByTenantIdAndKitchenIdAndDeletedAtIsNull(tenantId, kitchenId);
        long productCount = productRepository.countByTenantIdAndKitchenIdAndDeletedAtIsNull(tenantId, kitchenId);
        long orderItemCount = orderItemRepository.countByKitchenId(kitchenId);

        if (categoryCount > 0 || productCount > 0 || orderItemCount > 0) {
            throw PosException.badRequest(
                    "Bu oshxonaga boshqa ma'lumotlar bog'langan (" +
                    (categoryCount > 0 ? categoryCount + " ta kategoriya, " : "") +
                    (productCount > 0 ? productCount + " ta mahsulot, " : "") +
                    (orderItemCount > 0 ? orderItemCount + " ta buyurtma mahsuloti" : "") +
                    "). Uni o'chirish o'rniga faolsizlantirish (INACTIVE) tavsiya etiladi.");
        }

        employeeKitchenRepository.deleteByKitchenId(kitchenId);
        kitchen.setDeletedAt(Instant.now());
        kitchen.setActive(false);
        kitchenRepository.save(kitchen);
    }

    private String generateCodeFromName(String name) {
        if (name == null || name.isBlank()) return "KIT-" + (System.currentTimeMillis() % 10000);
        String normalized = name.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (normalized.length() > 6) {
            normalized = normalized.substring(0, 6);
        }
        if (normalized.isBlank()) normalized = "KIT";
        return normalized;
    }

    private int getNextSortOrder(UUID tenantId) {
        return kitchenRepository.findByTenantIdAndDeletedAtIsNullOrderBySortOrderAsc(tenantId).size() + 1;
    }
}
