package com.example.ecommerce.service.impl;

import com.example.ecommerce.entity.InventoryReservation;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.enums.OrderStatus;
import com.example.ecommerce.entity.enums.RecordStatus;
import com.example.ecommerce.event.InventoryResultEvent;
import com.example.ecommerce.event.OrderCreatedEvent;
import com.example.ecommerce.mapper.InventoryResultMapper;
import com.example.ecommerce.repository.InventoryReservationRepository;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryReservationRepository inventoryReservationRepository;

    @Mock
    private OrderRepository orderRepository;

    private InventoryServiceImpl inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryServiceImpl(
                productRepository,
                inventoryReservationRepository,
                orderRepository,
                new InventoryResultMapper()
        );
    }

    @Test
    void reserveInventoryDeductsStockAndStoresReservation() {
        OrderCreatedEvent event = orderCreatedEvent();
        Product product = Product.builder()
                .id(10L)
                .name("Keyboard")
                .price(BigDecimal.valueOf(99))
                .stockQuantity(5)
                .build();
        when(inventoryReservationRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(orderRepository.findByIdAndStatusAndOrderStatus(1L, RecordStatus.ACTIVE, OrderStatus.CREATED))
                .thenReturn(Optional.of(Order.builder().id(1L).orderStatus(OrderStatus.CREATED).build()));
        when(productRepository.findByIdForUpdate(10L, RecordStatus.ACTIVE)).thenReturn(Optional.of(product));

        InventoryResultEvent result = inventoryService.reserveInventory(event, "app");

        assertThat(result.isReserved()).isTrue();
        assertThat(product.getStockQuantity()).isEqualTo(3);
        verify(productRepository).save(product);

        ArgumentCaptor<InventoryReservation> captor = ArgumentCaptor.forClass(InventoryReservation.class);
        verify(inventoryReservationRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(1L);
        assertThat(captor.getValue().getQuantity()).isEqualTo(2);
        assertThat(captor.getValue().isReserved()).isTrue();
    }

    @Test
    void reserveInventoryReturnsStoredResultForDuplicateEvent() {
        InventoryReservation reservation = InventoryReservation.builder()
                .orderId(1L)
                .productId(10L)
                .productName("Keyboard")
                .quantity(2)
                .reserved(true)
                .reason("Stock reserved")
                .processedByInstance("app")
                .build();
        when(inventoryReservationRepository.findByOrderId(1L)).thenReturn(Optional.of(reservation));

        InventoryResultEvent result = inventoryService.reserveInventory(orderCreatedEvent(), "app");

        assertThat(result.isReserved()).isTrue();
        assertThat(result.getProcessedByInstance()).isEqualTo("app");
        verify(productRepository, never()).findByIdForUpdate(10L, RecordStatus.ACTIVE);
        verify(productRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reserveInventoryRejectsWhenOrderIsNotReservable() {
        when(inventoryReservationRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(orderRepository.findByIdAndStatusAndOrderStatus(1L, RecordStatus.ACTIVE, OrderStatus.CREATED))
                .thenReturn(Optional.empty());

        InventoryResultEvent result = inventoryService.reserveInventory(orderCreatedEvent(), "app");

        assertThat(result.isReserved()).isFalse();
        assertThat(result.getReason()).isEqualTo("Order is not available for inventory reservation");
        verify(productRepository, never()).findByIdForUpdate(10L, RecordStatus.ACTIVE);
    }

    private OrderCreatedEvent orderCreatedEvent() {
        return OrderCreatedEvent.builder()
                .orderId(1L)
                .productId(10L)
                .productName("Keyboard")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(99))
                .totalAmount(BigDecimal.valueOf(198))
                .createdByInstance("app")
                .build();
    }
}
