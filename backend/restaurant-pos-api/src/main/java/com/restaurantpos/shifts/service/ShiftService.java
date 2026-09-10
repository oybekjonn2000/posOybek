package com.restaurantpos.shifts.service;

import com.restaurantpos.common.exception.PosException;
import com.restaurantpos.shifts.dto.ShiftDto;
import com.restaurantpos.shifts.entity.Shift;
import com.restaurantpos.shifts.repository.ShiftRepository;
import com.restaurantpos.tenants.entity.Tenant;
import com.restaurantpos.tenants.repository.TenantRepository;
import com.restaurantpos.users.entity.User;
import com.restaurantpos.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    private static final AtomicInteger SHIFT_COUNTER = new AtomicInteger(10);

    @Transactional(readOnly = true)
    public ShiftDto.Response getCurrentShift(UUID tenantId) {
        Shift shift = shiftRepository.findByTenantIdAndStatus(tenantId, Shift.ShiftStatus.OPEN)
                .orElse(null);
        return shift != null ? toResponse(shift) : null;
    }

    @Transactional
    public ShiftDto.Response openShift(UUID tenantId, UUID cashierId, ShiftDto.OpenRequest request) {
        Optional<Shift> existing = shiftRepository.findByTenantIdAndStatus(tenantId, Shift.ShiftStatus.OPEN);
        if (existing.isPresent()) {
            throw PosException.badRequest("There is already an open shift (Shift #" + existing.get().getShiftNumber() + ")");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> PosException.notFound("Tenant not found"));

        User cashier = userRepository.findById(cashierId)
                .orElseThrow(() -> PosException.notFound("User not found"));

        Shift shift = new Shift();
        shift.setTenant(tenant);
        shift.setCashier(cashier);
        shift.setStatus(Shift.ShiftStatus.OPEN);
        shift.setOpeningCash(request.getOpeningCash());
        shift.setClosingCashExpected(request.getOpeningCash());
        shift.setOpenedAt(Instant.now());
        shift.setNotes(request.getNotes());

        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        shift.setShiftNumber("SH-" + dateStr + "-" + String.format("%03d", SHIFT_COUNTER.incrementAndGet() % 1000));

        Shift saved = shiftRepository.save(shift);
        return toResponse(saved);
    }

    @Transactional
    public ShiftDto.Response closeShift(UUID tenantId, UUID shiftId, ShiftDto.CloseRequest request) {
        Shift shift = shiftRepository.findByIdAndTenantId(shiftId, tenantId)
                .orElseThrow(() -> PosException.notFound("Shift not found: " + shiftId));

        if (shift.getStatus() == Shift.ShiftStatus.CLOSED) {
            throw PosException.badRequest("Shift is already closed");
        }

        BigDecimal expected = shift.getOpeningCash().add(shift.getTotalCashSales());
        BigDecimal actual = request.getClosingCashActual();
        BigDecimal difference = actual.subtract(expected);

        shift.setStatus(Shift.ShiftStatus.CLOSED);
        shift.setClosingCashExpected(expected);
        shift.setClosingCashActual(actual);
        shift.setCashDifference(difference);
        shift.setClosedAt(Instant.now());
        if (request.getNotes() != null) {
            shift.setNotes(shift.getNotes() != null ? shift.getNotes() + " | " + request.getNotes() : request.getNotes());
        }

        Shift saved = shiftRepository.save(shift);
        return toResponse(saved);
    }

    private ShiftDto.Response toResponse(Shift shift) {
        return ShiftDto.Response.builder()
                .id(shift.getId())
                .shiftNumber(shift.getShiftNumber())
                .status(shift.getStatus().name())
                .cashierId(shift.getCashier() != null ? shift.getCashier().getId() : null)
                .cashierName(shift.getCashier() != null ? shift.getCashier().getFirstName() + " " + (shift.getCashier().getLastName() != null ? shift.getCashier().getLastName() : "") : null)
                .openingCash(shift.getOpeningCash())
                .closingCashExpected(shift.getClosingCashExpected())
                .closingCashActual(shift.getClosingCashActual())
                .cashDifference(shift.getCashDifference())
                .totalSales(shift.getTotalSales())
                .totalCashSales(shift.getTotalCashSales())
                .totalCardSales(shift.getTotalCardSales())
                .totalRefunds(shift.getTotalRefunds())
                .totalDiscounts(shift.getTotalDiscounts())
                .ordersCount(shift.getOrdersCount())
                .openedAt(shift.getOpenedAt())
                .closedAt(shift.getClosedAt())
                .notes(shift.getNotes())
                .build();
    }
}
