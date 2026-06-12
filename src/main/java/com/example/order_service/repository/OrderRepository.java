package com.example.order_service.repository;

import com.example.order_service.entity.Order;
import com.example.order_service.entity.enums.OrderStatus;
import com.example.order_service.entity.enums.RecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdAndStatus(Long id, RecordStatus status);

    Optional<Order> findByIdAndStatusAndOrderStatus(Long id, RecordStatus status, OrderStatus orderStatus);

    Page<Order> findAllByStatus(RecordStatus status, Pageable pageable);
}
