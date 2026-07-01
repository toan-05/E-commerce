package com.example.ecommerce.dto.response.inventory;

import com.example.ecommerce.entity.InventoryReservation;
import com.example.ecommerce.entity.enums.RecordStatus;

import java.time.Instant;

/**
 * Response returned when reading an inventory reservation.
 */
public record InventoryReservationResponse(
        Long orderId,
        Long productId,
        String productName,
        Integer quantity,
        boolean reserved,
        String reason,
        String processedByInstance,
        RecordStatus recordStatus,
        Instant createdAt,
        Instant updatedAt
) {

    public static InventoryReservationResponse from(InventoryReservation reservation) {
        return new InventoryReservationResponse(
                reservation.getOrderId(),
                reservation.getProductId(),
                reservation.getProductName(),
                reservation.getQuantity(),
                reservation.isReserved(),
                reservation.getReason(),
                reservation.getProcessedByInstance(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt()
        );
    }
}
