package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.order.CreateOrderRequest;
import com.example.ecommerce.dto.request.order.UpdateOrderRequest;
import com.example.ecommerce.dto.response.common.ApiResponse;
import com.example.ecommerce.dto.response.order.OrderResponse;
import com.example.ecommerce.dto.response.common.PageResponse;
import jakarta.validation.Valid;
import com.example.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for order operations.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Creates a new order and starts inventory reservation.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success("Order accepted", OrderResponse.from(orderService.createOrder(request)));
    }

    /**
     * Returns one order by id.
     */
    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable Long id) {
        return ApiResponse.success("Order fetched", OrderResponse.from(orderService.getOrder(id)));
    }

    /**
     * Returns all orders.
     */
    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> getOrders(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success(
                "Orders fetched",
                PageResponse.from(orderService.getOrders(pageable), OrderResponse::from)
        );
    }

    /**
     * Replaces an order while it is still editable.
     */
    @PutMapping("/{id}")
    public ApiResponse<OrderResponse> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderRequest request
    ) {
        return ApiResponse.success("Order updated", OrderResponse.from(orderService.updateOrder(id, request)));
    }

    /**
     * Deletes an order while it is still editable.
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ApiResponse.success("Order deleted");
    }
}
