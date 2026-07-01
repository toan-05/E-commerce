package com.example.ecommerce.mapper;

import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.dto.request.order.CreateOrderRequest;
import com.example.ecommerce.entity.enums.OrderStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OrderMapper {

    public Order toOrder(CreateOrderRequest request, Product product, String handledBy) {
        return Order.builder()
                .productId(product.getId())
                .productName(product.getName())
                .quantity(request.quantity())
                .unitPrice(product.getPrice())
                .totalAmount(product.getPrice().multiply(BigDecimal.valueOf(request.quantity())))
                .orderStatus(OrderStatus.CREATED)
                .statusReason("Order accepted")
                .handledBy(handledBy)
                .build();
    }
}
