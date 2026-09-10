package com.restaurantpos.tables.service;

import com.restaurantpos.common.exception.PosException;
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TableService {

    private final RestaurantTableRepository tableRepository;
    private final TableZoneRepository zoneRepository;
    private final TenantRepository tenantRepository;

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

    @Transactional(readOnly = true)
    public List<TableDto.Response> getTables(UUID tenantId, UUID zoneId) {
        List<RestaurantTable> tables = zoneId != null
                ? tableRepository.findByTenantIdAndZoneIdAndDeletedAtIsNullOrderByTableNumberAsc(tenantId, zoneId)
                : tableRepository.findByTenantIdAndDeletedAtIsNullOrderByTableNumberAsc(tenantId);

        return tables.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TableDto.Response getTableById(UUID id, UUID tenantId) {
        RestaurantTable table = tableRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Table not found: " + id));
        return toResponse(table);
    }

    @Transactional
    public TableDto.Response updateTableStatus(UUID id, UUID tenantId, TableDto.UpdateStatusRequest request) {
        RestaurantTable table = tableRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Table not found: " + id));

        table.setStatus(RestaurantTable.TableStatus.valueOf(request.getStatus().toUpperCase()));
        table.setCurrentOrderId(request.getCurrentOrderId());

        RestaurantTable saved = tableRepository.save(table);
        return toResponse(saved);
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

    private TableDto.Response toResponse(RestaurantTable table) {
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
                .active(table.isActive())
                .build();
    }
}
