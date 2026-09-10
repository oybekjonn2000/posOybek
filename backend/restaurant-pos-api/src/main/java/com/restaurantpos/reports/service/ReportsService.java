package com.restaurantpos.reports.service;

import com.restaurantpos.payments.entity.Payment;
import com.restaurantpos.payments.repository.PaymentRepository;
import com.restaurantpos.reports.dto.ReportDto;
import com.restaurantpos.shifts.entity.Shift;
import com.restaurantpos.shifts.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportsService {

    private final PaymentRepository paymentRepository;
    private final ShiftRepository shiftRepository;

    @Transactional(readOnly = true)
    public ReportDto.DailySummary getDailySummary(UUID tenantId, LocalDate date) {
        ZoneId zone = ZoneId.of("Asia/Tashkent");
        Instant from = date.atStartOfDay(zone).toInstant();
        Instant to   = date.plusDays(1).atStartOfDay(zone).toInstant();

        List<Payment> payments = paymentRepository.findByTenantIdAndPaidAtBetweenAndRefundFalse(
                tenantId, from, to);
        List<Payment> refunds  = paymentRepository.findByTenantIdAndPaidAtBetweenAndRefundTrue(
                tenantId, from, to);

        BigDecimal totalRevenue = payments.stream()
                .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cashRevenue  = payments.stream()
                .map(Payment::getCashAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cardRevenue  = payments.stream()
                .map(Payment::getCardAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRefunds = refunds.stream()
                .map(p -> p.getAmount().abs()).reduce(BigDecimal.ZERO, BigDecimal::add);

        long completed = payments.size();
        BigDecimal avg = completed > 0
                ? totalRevenue.divide(BigDecimal.valueOf(completed), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Soatlik statistika
        List<ReportDto.HourlyRevenue> hourly = payments.stream()
                .collect(Collectors.groupingBy(p ->
                        p.getPaidAt().atZone(zone).getHour()))
                .entrySet().stream()
                .map(e -> ReportDto.HourlyRevenue.builder()
                        .hour(e.getKey())
                        .orders(e.getValue().size())
                        .revenue(e.getValue().stream().map(Payment::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                        .build())
                .sorted(Comparator.comparingInt(ReportDto.HourlyRevenue::getHour))
                .collect(Collectors.toList());

        return ReportDto.DailySummary.builder()
                .date(from)
                .totalRevenue(totalRevenue)
                .cashRevenue(cashRevenue)
                .cardRevenue(cardRevenue)
                .totalRefunds(totalRefunds)
                .totalOrders(completed)
                .completedOrders(completed)
                .refundedOrders(refunds.size())
                .avgOrderValue(avg)
                .topProducts(List.of())   // TODO: OrderItem aggregate
                .hourlyRevenue(hourly)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ReportDto.ShiftSummary> getShiftReports(UUID tenantId, LocalDate date) {
        ZoneId zone = ZoneId.of("Asia/Tashkent");
        Instant from = date.atStartOfDay(zone).toInstant();
        Instant to   = date.plusDays(1).atStartOfDay(zone).toInstant();

        return shiftRepository.findByTenantIdAndOpenedAtBetween(tenantId, from, to).stream()
                .map(s -> ReportDto.ShiftSummary.builder()
                        .shiftId(s.getId())
                        .cashierName(s.getCashier() != null
                                ? s.getCashier().getFirstName() + " " +
                                  (s.getCashier().getLastName() != null ? s.getCashier().getLastName() : "")
                                : "Unknown")
                        .openedAt(s.getOpenedAt())
                        .closedAt(s.getClosedAt())
                        .totalSales(s.getTotalSales())
                        .totalCash(s.getTotalCashSales())
                        .totalCard(s.getTotalCardSales())
                        .totalRefunds(s.getTotalRefunds())
                        .ordersCount(s.getOrdersCount())
                        .build())
                .collect(Collectors.toList());
    }
}
