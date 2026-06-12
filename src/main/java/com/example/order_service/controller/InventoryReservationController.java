package com.example.order_service.controller;

import com.example.order_service.dto.response.common.ApiResponse;
import com.example.order_service.dto.response.inventory.InventoryReservationResponse;
import com.example.order_service.dto.response.common.PageResponse;
import com.example.order_service.service.InventoryReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for checking and cleaning inventory reservations.
 */
@RestController
@RequestMapping("/api/v1/inventory-reservations")
@RequiredArgsConstructor
public class InventoryReservationController {

    private final InventoryReservationService inventoryReservationService;

    /**
     * Returns one inventory reservation by order id.
     */
    @GetMapping("/{orderId}")
    public ApiResponse<InventoryReservationResponse> getReservation(@PathVariable Long orderId) {
        return ApiResponse.success(
                "Inventory reservation fetched",
                InventoryReservationResponse.from(inventoryReservationService.getReservation(orderId))
        );
    }

    /**
     * Returns inventory reservations by page.
     */
    @GetMapping
    public ApiResponse<PageResponse<InventoryReservationResponse>> getReservations(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success(
                "Inventory reservations fetched",
                PageResponse.from(inventoryReservationService.getReservations(pageable), InventoryReservationResponse::from)
        );
    }

    /**
     * Deletes one inventory reservation by order id.
     */
    @DeleteMapping("/{orderId}")
    public ApiResponse<Void> deleteReservation(@PathVariable Long orderId) {
        inventoryReservationService.deleteReservation(orderId);
        return ApiResponse.success("Inventory reservation deleted");
    }
}
