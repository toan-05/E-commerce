package com.example.ecommerce.service.impl;

import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.InventoryReservation;
import com.example.ecommerce.entity.enums.RecordStatus;
import com.example.ecommerce.entity.enums.OrderStatus;
import com.example.ecommerce.event.InventoryResultEvent;
import com.example.ecommerce.event.OrderCreatedEvent;
import com.example.ecommerce.mapper.InventoryResultMapper;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.InventoryReservationRepository;
import com.example.ecommerce.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final ProductRepository productRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final OrderRepository orderRepository;
    private final InventoryResultMapper inventoryResultMapper;

    @Override
    @Transactional
    public InventoryResultEvent reserveInventory(OrderCreatedEvent event, String processedBy) {
        InventoryReservation reservation = inventoryReservationRepository
                .findByOrderId(event.getOrderId())
                .orElse(null);
        if (reservation != null) {
            return inventoryResultMapper.toResultEvent(reservation);
        }

        boolean orderIsReservable = orderRepository
                .findByIdAndStatusAndOrderStatus(event.getOrderId(), RecordStatus.ACTIVE, OrderStatus.CREATED)
                .isPresent();
        if (!orderIsReservable) {
            InventoryResultEvent result = inventoryResultMapper.rejected(
                    event,
                    "Order is not available for inventory reservation",
                    processedBy
            );
            inventoryReservationRepository.save(inventoryResultMapper.toReservation(result));
            return result;
        }

        Product product = productRepository.findByIdForUpdate(event.getProductId(), RecordStatus.ACTIVE).orElse(null);

        InventoryResultEvent result;
        if (product == null) {
            result = inventoryResultMapper.rejected(event, "Product not found", processedBy);
        } else if (product.getStockQuantity() < event.getQuantity()) {
            result = inventoryResultMapper.rejected(event, "Not enough stock", processedBy);
        } else {
            product.setStockQuantity(product.getStockQuantity() - event.getQuantity());
            productRepository.save(product);
            result = inventoryResultMapper.reserved(event, processedBy);
        }

        inventoryReservationRepository.save(inventoryResultMapper.toReservation(result));
        return result;
    }
}
