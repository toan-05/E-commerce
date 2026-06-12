package com.example.order_service.dto.response.order;

import com.example.order_service.entity.Order;
import com.example.order_service.entity.enums.OrderStatus;
import com.example.order_service.entity.enums.RecordStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        Long id,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        OrderStatus orderStatus,
        RecordStatus recordStatus,
        String statusReason,
        String handledBy,
        Instant createdAt,
        Instant updatedAt
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getProductId(),
                order.getProductName(),
                order.getQuantity(),
                order.getUnitPrice(),
                order.getTotalAmount(),
                order.getOrderStatus(),
                order.getStatus(),
                order.getStatusReason(),
                order.getHandledBy(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
