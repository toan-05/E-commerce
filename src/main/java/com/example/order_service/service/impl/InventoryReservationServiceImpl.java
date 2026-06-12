package com.example.order_service.service.impl;

import com.example.order_service.entity.InventoryReservation;
import com.example.order_service.entity.enums.RecordStatus;
import com.example.order_service.exception.ErrorCode;
import com.example.order_service.exception.ResourceNotFoundException;
import com.example.order_service.repository.InventoryReservationRepository;
import com.example.order_service.service.InventoryReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryReservationServiceImpl implements InventoryReservationService {

    private final InventoryReservationRepository inventoryReservationRepository;

    @Override
    public InventoryReservation getReservation(Long orderId) {
        return inventoryReservationRepository.findByOrderIdAndStatus(orderId, RecordStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.INVENTORY_RESERVATION_NOT_FOUND,
                        "Inventory reservation",
                        orderId
                ));
    }

    @Override
    public Page<InventoryReservation> getReservations(Pageable pageable) {
        return inventoryReservationRepository.findAllByStatus(RecordStatus.ACTIVE, pageable);
    }

    @Override
    public void deleteReservation(Long orderId) {
        InventoryReservation reservation = getReservation(orderId);
        reservation.markDeleted();
        inventoryReservationRepository.save(reservation);
    }
}
