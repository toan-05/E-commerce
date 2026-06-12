package com.example.order_service.dto.request.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for replacing an order while it is still editable.
 */
public record UpdateOrderRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity
) {
}
