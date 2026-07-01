package com.example.ecommerce.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "inventory_reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class InventoryReservation extends BaseEntity {

    @Column(nullable = false, unique = true)
    private Long orderId;

    private Long productId;

    private String productName;

    private Integer quantity;

    private boolean reserved;

    private String reason;

    private String processedByInstance;
}
