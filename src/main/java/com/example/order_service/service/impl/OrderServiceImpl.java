package com.example.order_service.service.impl;

import com.example.order_service.config.AppProperties;
import com.example.order_service.dto.request.order.UpdateOrderRequest;
import com.example.order_service.entity.Order;
import com.example.order_service.entity.Product;
import com.example.order_service.dto.request.order.CreateOrderRequest;
import com.example.order_service.entity.enums.OrderStatus;
import com.example.order_service.entity.enums.RecordStatus;
import com.example.order_service.event.OrderCreatedEvent;
import com.example.order_service.exception.BusinessException;
import com.example.order_service.exception.ErrorCode;
import com.example.order_service.exception.ResourceNotFoundException;
import com.example.order_service.kafka.publisher.OrderEventPublisher;
import com.example.order_service.mapper.OrderMapper;
import com.example.order_service.repository.InventoryReservationRepository;
import com.example.order_service.repository.OrderRepository;
import com.example.order_service.service.OrderService;
import com.example.order_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final OrderEventPublisher orderEventPublisher;
    private final OrderMapper orderMapper;
    private final AppProperties appProperties;
    private final InventoryReservationRepository inventoryReservationRepository;

    @Override
    public Order createOrder(CreateOrderRequest request) {
        Product product = productService.getProduct(request.productId());

        Order order = orderMapper.toOrder(request, product, appProperties.getInstanceName());
        Order savedOrder = orderRepository.save(order);
        orderEventPublisher.publishOrderCreated(OrderCreatedEvent.from(savedOrder));

        return savedOrder;
    }

    @Override
    public Order getOrder(Long id) {
        return findOrder(id);
    }

    @Override
    public Page<Order> getOrders(Pageable pageable) {
        return orderRepository.findAllByStatus(RecordStatus.ACTIVE, pageable);
    }

    @Override
    public Order updateOrder(Long id, UpdateOrderRequest request) {
        Order order = findOrder(id);
        ensureOrderIsEditable(order, ErrorCode.ORDER_UPDATE_NOT_ALLOWED, "Order can no longer be updated: " + id);

        Product product = productService.getProduct(request.productId());
        order.setProductId(product.getId());
        order.setProductName(product.getName());
        order.setQuantity(request.quantity());
        order.setUnitPrice(product.getPrice());
        order.setTotalAmount(product.getPrice().multiply(BigDecimal.valueOf(request.quantity())));
        order.setStatusReason("Order updated");

        return orderRepository.save(order);
    }

    @Override
    public void deleteOrder(Long id) {
        Order order = findOrder(id);
        ensureOrderIsEditable(order, ErrorCode.ORDER_DELETE_NOT_ALLOWED, "Order can no longer be deleted: " + id);
        order.markDeleted();
        orderRepository.save(order);
    }

    @Override
    public Order markInventoryReserved(Long orderId, String processedBy) {
        Order order = findOrder(orderId);
        order.setOrderStatus(OrderStatus.INVENTORY_RESERVED);
        order.setStatusReason("Stock reserved");
        order.setHandledBy(order.getHandledBy() + " | inventory:" + processedBy);
        return orderRepository.save(order);
    }

    @Override
    public Order markInventoryRejected(Long orderId, String reason, String processedBy) {
        Order order = findOrder(orderId);
        order.setOrderStatus(OrderStatus.INVENTORY_REJECTED);
        order.setStatusReason(reason);
        order.setHandledBy(order.getHandledBy() + " | inventory:" + processedBy);
        return orderRepository.save(order);
    }

    private Order findOrder(Long id) {
        return orderRepository.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, "Order", id));
    }

    private void ensureOrderIsEditable(Order order, ErrorCode errorCode, String message) {
        boolean hasReservation = inventoryReservationRepository.existsByOrderId(order.getId());
        if (order.getOrderStatus() != OrderStatus.CREATED || hasReservation) {
            throw new BusinessException(errorCode, message);
        }
    }
}
