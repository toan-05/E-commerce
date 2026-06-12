package com.example.order_service.dto.response.product;

import com.example.order_service.entity.Product;
import com.example.order_service.entity.enums.RecordStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String name,
        BigDecimal price,
        Integer stockQuantity,
        RecordStatus recordStatus,
        Instant createdAt,
        Instant updatedAt
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
