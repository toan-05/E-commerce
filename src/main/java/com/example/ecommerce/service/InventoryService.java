package com.example.ecommerce.service;

import com.example.ecommerce.event.InventoryResultEvent;
import com.example.ecommerce.event.OrderCreatedEvent;

/**
 * Handles stock reservation after an order-created event is received.
 */
public interface InventoryService {

    /**
     * Reserves stock for an order-created event and returns the reservation result.
     */
    InventoryResultEvent reserveInventory(OrderCreatedEvent event, String processedBy);
}
