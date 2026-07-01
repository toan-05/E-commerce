package com.example.ecommerce.repository;

import com.example.ecommerce.entity.InventoryReservation;
import com.example.ecommerce.entity.enums.RecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    Optional<InventoryReservation> findByOrderId(Long orderId);

    Optional<InventoryReservation> findByOrderIdAndStatus(Long orderId, RecordStatus status);

    Page<InventoryReservation> findAllByStatus(RecordStatus status, Pageable pageable);

    boolean existsByOrderId(Long orderId);

    boolean existsByOrderIdAndStatus(Long orderId, RecordStatus status);
}
