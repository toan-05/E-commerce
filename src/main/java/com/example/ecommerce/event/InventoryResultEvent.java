package com.example.ecommerce.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryResultEvent {

    private Long orderId;
    private Long productId;
    private String productName;
    private Integer requestedQuantity;
    private boolean reserved;
    private String reason;
    private String processedByInstance;
}
