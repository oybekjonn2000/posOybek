package com.restaurantpos.payments.service;

import com.restaurantpos.common.exception.PosException;
import com.restaurantpos.orders.dto.OrderDto;
import com.restaurantpos.orders.entity.Order;
import com.restaurantpos.orders.repository.OrderRepository;
import com.restaurantpos.payments.dto.PaymentDto;
import com.restaurantpos.payments.entity.Payment;
import com.restaurantpos.payments.repository.PaymentRepository;
import com.restaurantpos.shifts.entity.Shift;
import com.restaurantpos.shifts.repository.ShiftRepository;
import com.restaurantpos.tables.entity.RestaurantTable;
import com.restaurantpos.tables.repository.RestaurantTableRepository;
import com.restaurantpos.users.entity.User;
import com.restaurantpos.users.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final RestaurantTableRepository tableRepository;
    private final UserRepository userRepository;
    private final ShiftRepository shiftRepository;
    private final EntityManager entityManager;
    private final com.restaurantpos.common.websocket.WebSocketNotificationService wsNotification;
    private final com.restaurantpos.orders.service.OrderService orderService;

    private long nextPaymentSequence() {
        return ((Number) entityManager
                .createNativeQuery("SELECT nextval('payment_number_seq')").getSingleResult()).longValue();
    }

    @Transactional
    public PaymentDto.Response processPayment(UUID tenantId, UUID cashierId, PaymentDto.ProcessRequest request) {
        Order order = orderRepository.findByIdAndTenantIdAndDeletedAtIsNull(request.getOrderId(), tenantId)
                .orElseThrow(() -> PosException.notFound("Order not found: " + request.getOrderId()));

        if (order.getStatus() == Order.OrderStatus.PAID) {
            throw PosException.badRequest("Order is already paid");
        }

        User cashier = userRepository.findById(cashierId).orElse(null);
        Shift shift = shiftRepository.findByTenantIdAndStatus(tenantId, Shift.ShiftStatus.OPEN).orElse(null);

        Payment payment = new Payment();
        payment.setTenant(order.getTenant());
        payment.setOrder(order);
        payment.setShift(shift);
        payment.setCashier(cashier);
        payment.setDevice(order.getDevice());

        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        payment.setPaymentNumber("PAY-" + dateStr + "-" + String.format("%04d", nextPaymentSequence()));

        Payment.PaymentMethod method = Payment.PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
        payment.setPaymentMethod(method);
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setAmount(request.getAmount());
        payment.setCashAmount(request.getCashAmount() != null ? request.getCashAmount() : BigDecimal.ZERO);
        payment.setCardAmount(request.getCardAmount() != null ? request.getCardAmount() : BigDecimal.ZERO);
        payment.setChangeAmount(request.getChangeAmount() != null ? request.getChangeAmount() : BigDecimal.ZERO);
        payment.setReferenceNumber(request.getReferenceNumber());
        payment.setNotes(request.getNotes());
        payment.setPaidAt(Instant.now());

        Payment saved = paymentRepository.save(payment);

        // Update Order status to PAID
        order.setStatus(Order.OrderStatus.PAID);
        order.setCashier(cashier);
        Instant now = Instant.now();
        order.setPaidAt(now);
        order.setClosedAt(now);
        Order savedOrder = orderRepository.save(order);

        // Free the table
        if (order.getTable() != null) {
            RestaurantTable table = order.getTable();
            table.setStatus(com.restaurantpos.tables.entity.RestaurantTable.TableStatus.FREE);
            table.setCurrentOrderId(null);
            RestaurantTable savedTable = tableRepository.save(table);
            wsNotification.notifyTableUpdated(tenantId, orderService.toTableResponse(savedTable, null));
        }

        // Notify order status changed & paid
        OrderDto.Response orderResponse = orderService.toResponse(savedOrder);
        wsNotification.notifyOrderStatusChanged(tenantId, orderResponse);
        wsNotification.notifyOrderPaid(tenantId, order.getId());

        // Update Shift totals if shift is active
        if (shift != null) {
            shift.setTotalSales(shift.getTotalSales().add(payment.getAmount()));
            shift.setTotalCashSales(shift.getTotalCashSales().add(payment.getCashAmount()));
            shift.setTotalCardSales(shift.getTotalCardSales().add(payment.getCardAmount()));
            shift.setOrdersCount(shift.getOrdersCount() + 1);
            shiftRepository.save(shift);
        }

        return toResponse(saved);
    }

    @Transactional
    public PaymentDto.Response refundPayment(UUID paymentId, UUID tenantId, UUID cashierId, PaymentDto.RefundRequest request) {
        Payment original = paymentRepository.findByIdAndTenantId(paymentId, tenantId)
                .orElseThrow(() -> PosException.notFound("Payment not found: " + paymentId));

        if (original.isRefund()) {
            throw PosException.badRequest("Cannot refund a refund transaction");
        }

        User cashier = userRepository.findById(cashierId).orElse(null);
        Shift shift = shiftRepository.findByTenantIdAndStatus(tenantId, Shift.ShiftStatus.OPEN).orElse(null);

        Payment refund = new Payment();
        refund.setTenant(original.getTenant());
        refund.setOrder(original.getOrder());
        refund.setShift(shift);
        refund.setCashier(cashier);
        refund.setDevice(original.getDevice());

        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        refund.setPaymentNumber("REF-" + dateStr + "-" + String.format("%04d", nextPaymentSequence()));
        refund.setPaymentMethod(original.getPaymentMethod());
        refund.setStatus(Payment.PaymentStatus.REFUNDED);
        refund.setAmount(original.getAmount().negate());
        refund.setCashAmount(original.getCashAmount().negate());
        refund.setCardAmount(original.getCardAmount().negate());
        refund.setRefund(true);
        refund.setOriginalPayment(original);
        refund.setRefundReason(request.getReason());
        refund.setPaidAt(Instant.now());

        Payment savedRefund = paymentRepository.save(refund);

        // Mark original order as REFUNDED
        Order order = original.getOrder();
        order.setStatus(Order.OrderStatus.REFUNDED);
        orderRepository.save(order);

        // Update Shift refunds
        if (shift != null) {
            shift.setTotalRefunds(shift.getTotalRefunds().add(original.getAmount()));
            shiftRepository.save(shift);
        }

        return toResponse(savedRefund);
    }

    @Transactional(readOnly = true)
    public List<PaymentDto.Response> getPaymentsByOrder(UUID orderId) {
        return paymentRepository.findByOrderId(orderId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private PaymentDto.Response toResponse(Payment payment) {
        return PaymentDto.Response.builder()
                .id(payment.getId())
                .orderId(payment.getOrder() != null ? payment.getOrder().getId() : null)
                .paymentNumber(payment.getPaymentNumber())
                .paymentMethod(payment.getPaymentMethod().name())
                .status(payment.getStatus().name())
                .amount(payment.getAmount())
                .cashAmount(payment.getCashAmount())
                .cardAmount(payment.getCardAmount())
                .changeAmount(payment.getChangeAmount())
                .refund(payment.isRefund())
                .referenceNumber(payment.getReferenceNumber())
                .notes(payment.getNotes())
                .paidAt(payment.getPaidAt())
                .cashierId(payment.getCashier() != null ? payment.getCashier().getId() : null)
                .cashierName(payment.getCashier() != null ? payment.getCashier().getFirstName() + " " + (payment.getCashier().getLastName() != null ? payment.getCashier().getLastName() : "") : null)
                .build();
    }
}
