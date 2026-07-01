package com.example.ecommerce.service.impl;

import com.example.ecommerce.config.AppProperties;
import com.example.ecommerce.dto.request.order.UpdateOrderRequest;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.dto.request.order.CreateOrderRequest;
import com.example.ecommerce.entity.enums.OrderStatus;
import com.example.ecommerce.entity.enums.RecordStatus;
import com.example.ecommerce.event.OrderCreatedEvent;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.kafka.publisher.OrderEventPublisher;
import com.example.ecommerce.mapper.OrderMapper;
import com.example.ecommerce.repository.InventoryReservationRepository;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductService productService;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @Mock
    private InventoryReservationRepository inventoryReservationRepository;

    @Mock
    private AppProperties appProperties;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                orderRepository,
                productService,
                orderEventPublisher,
                new OrderMapper(),
                appProperties,
                inventoryReservationRepository
        );
    }

    @Test
    void createOrderStoresSnapshotAndPublishesEvent() {
        Product product = Product.builder()
                .id(10L)
                .name("Keyboard")
                .price(BigDecimal.valueOf(99))
                .stockQuantity(5)
                .build();
        when(appProperties.getInstanceName()).thenReturn("app");
        when(productService.getProduct(10L)).thenReturn(product);
        when(orderRepository.save(org.mockito.ArgumentMatchers.any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    order.setId(1L);
                    return order;
                });

        Order order = orderService.createOrder(new CreateOrderRequest(10L, 2));

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("198");
        assertThat(order.getHandledBy()).isEqualTo("app");

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(orderEventPublisher).publishOrderCreated(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getOrderId()).isEqualTo(1L);
        assertThat(eventCaptor.getValue().getTotalAmount()).isEqualByComparingTo("198");
    }

    @Test
    void updateOrderReplacesSnapshotWhenOrderIsEditable() {
        Order order = Order.builder()
                .id(1L)
                .productId(10L)
                .productName("Keyboard")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(99))
                .totalAmount(BigDecimal.valueOf(198))
                .orderStatus(OrderStatus.CREATED)
                .statusReason("Order accepted")
                .handledBy("app")
                .build();
        Product product = Product.builder()
                .id(20L)
                .name("Mouse")
                .price(BigDecimal.valueOf(50))
                .stockQuantity(10)
                .build();
        when(orderRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE)).thenReturn(Optional.of(order));
        when(inventoryReservationRepository.existsByOrderId(1L)).thenReturn(false);
        when(productService.getProduct(20L)).thenReturn(product);
        when(orderRepository.save(order)).thenReturn(order);

        Order updated = orderService.updateOrder(1L, new UpdateOrderRequest(20L, 3));

        assertThat(updated.getProductId()).isEqualTo(20L);
        assertThat(updated.getProductName()).isEqualTo("Mouse");
        assertThat(updated.getQuantity()).isEqualTo(3);
        assertThat(updated.getTotalAmount()).isEqualByComparingTo("150");
    }

    @Test
    void updateOrderFailsWhenReservationAlreadyExists() {
        Order order = Order.builder()
                .id(1L)
                .orderStatus(OrderStatus.CREATED)
                .build();
        when(orderRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE)).thenReturn(Optional.of(order));
        when(inventoryReservationRepository.existsByOrderId(1L)).thenReturn(true);

        assertThatThrownBy(() -> orderService.updateOrder(1L, new UpdateOrderRequest(20L, 3)))
                .isInstanceOf(BusinessException.class);

        verify(orderRepository, never()).save(order);
    }
}
