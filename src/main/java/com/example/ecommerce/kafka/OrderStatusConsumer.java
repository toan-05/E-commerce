package com.example.ecommerce.kafka;

import com.example.ecommerce.config.AppProperties;
import com.example.ecommerce.event.InventoryResultEvent;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderStatusConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusConsumer.class);

    private final OrderService orderService;
    private final AppProperties appProperties;

    @KafkaListener(
            topics = "${app.kafka.inventory-result-topic}",
            groupId = "order-status-updater"
    )
    public void updateOrderStatus(InventoryResultEvent event) {
        String instanceName = appProperties.getInstanceName();
        String inventoryProcessor = event.getProcessedByInstance();
        try {
            if (event.isReserved()) {
                orderService.markInventoryReserved(event.getOrderId(), inventoryProcessor);
            } else {
                orderService.markInventoryRejected(event.getOrderId(), event.getReason(), inventoryProcessor);
            }
        } catch (ResourceNotFoundException ex) {
            log.warn("Skipping inventory result for unavailable order. orderId={}, updater={}", event.getOrderId(), instanceName);
            return;
        }

        log.info(
                "Order status updated from Kafka event. orderId={}, reserved={}, inventoryProcessor={}, updater={}",
                event.getOrderId(),
                event.isReserved(),
                inventoryProcessor,
                instanceName
        );
    }
}
