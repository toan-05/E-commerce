package com.example.order_service.service;

import com.example.order_service.entity.InventoryReservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Handles read and cleanup operations for inventory reservations.
 */
public interface InventoryReservationService {

    /**
     * Returns one inventory reservation by order id.
     */
    InventoryReservation getReservation(Long orderId);

    /**
     * Returns inventory reservations by page.
     */
    Page<InventoryReservation> getReservations(Pageable pageable);

    /**
     * Deletes one inventory reservation by order id.
     */
    void deleteReservation(Long orderId);
}
