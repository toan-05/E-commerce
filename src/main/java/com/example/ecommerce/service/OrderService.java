package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.order.CreateOrderRequest;
import com.example.ecommerce.dto.request.order.UpdateOrderRequest;
import com.example.ecommerce.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Handles order lifecycle operations.
 */
public interface OrderService {

    /**
     * Creates an order and publishes an event for inventory reservation.
     */
    Order createOrder(CreateOrderRequest request);

    /**
     * Returns one order by id.
     */
    Order getOrder(Long id);

    /**
     * Returns orders by page.
     */
    Page<Order> getOrders(Pageable pageable);

    /**
     * Replaces an order while it is still editable.
     */
    Order updateOrder(Long id, UpdateOrderRequest request);

    /**
     * Deletes an order while it is still editable.
     */
    void deleteOrder(Long id);

    /**
     * Marks an order as inventory-reserved after the inventory result event is received.
     */
    Order markInventoryReserved(Long orderId, String processedBy);

    /**
     * Marks an order as inventory-rejected after the inventory result event is received.
     */
    Order markInventoryRejected(Long orderId, String reason, String processedBy);
}
