package com.restaurantpos.kitchen.repository;

import com.restaurantpos.kitchen.entity.KitchenTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KitchenTicketRepository extends JpaRepository<KitchenTicket, UUID> {

    List<KitchenTicket> findByTenantIdAndKitchenIdAndStatusInOrderByCreatedAtAsc(
            UUID tenantId, UUID kitchenId, List<KitchenTicket.TicketStatus> statuses);

    List<KitchenTicket> findByTenantIdAndOrderIdOrderByCreatedAtAsc(UUID tenantId, UUID orderId);

    Optional<KitchenTicket> findByIdAndTenantId(UUID id, UUID tenantId);
}
