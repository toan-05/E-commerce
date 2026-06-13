package com.example.ecommerce.mapper;

import com.example.ecommerce.entity.InventoryReservation;
import com.example.ecommerce.event.InventoryResultEvent;
import com.example.ecommerce.event.OrderCreatedEvent;
import org.springframework.stereotype.Component;

@Component
public class InventoryResultMapper {

    public InventoryResultEvent reserved(OrderCreatedEvent event, String processedBy) {
        return result(event, true, "Stock reserved", processedBy);
    }

    public InventoryResultEvent rejected(OrderCreatedEvent event, String reason, String processedBy) {
        return result(event, false, reason, processedBy);
    }

    public InventoryReservation toReservation(InventoryResultEvent event) {
        return InventoryReservation.builder()
                .orderId(event.getOrderId())
                .productId(event.getProductId())
                .productName(event.getProductName())
                .quantity(event.getRequestedQuantity())
                .reserved(event.isReserved())
                .reason(event.getReason())
                .processedByInstance(event.getProcessedByInstance())
                .build();
    }

    public InventoryResultEvent toResultEvent(InventoryReservation reservation) {
        return InventoryResultEvent.builder()
                .orderId(reservation.getOrderId())
                .productId(reservation.getProductId())
                .productName(reservation.getProductName())
                .requestedQuantity(reservation.getQuantity())
                .reserved(reservation.isReserved())
                .reason(reservation.getReason())
                .processedByInstance(reservation.getProcessedByInstance())
                .build();
    }

    private InventoryResultEvent result(
            OrderCreatedEvent event,
            boolean reserved,
            String reason,
            String processedBy
    ) {
        return InventoryResultEvent.builder()
                .orderId(event.getOrderId())
                .productId(event.getProductId())
                .productName(event.getProductName())
                .requestedQuantity(event.getQuantity())
                .reserved(reserved)
                .reason(reason)
                .processedByInstance(processedBy)
                .build();
    }
}
