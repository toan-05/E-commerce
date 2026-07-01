package com.example.ecommerce.kafka.publisher;

import com.example.ecommerce.config.AppProperties;
import com.example.ecommerce.event.InventoryResultEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AppProperties appProperties;

    public void publishInventoryResult(InventoryResultEvent event) {
        String key = String.valueOf(event.getOrderId());
        String topic = appProperties.getInventoryResultTopic();
        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish inventory-result event. orderId={}", event.getOrderId(), ex);
                    } else {
                        log.info("Published inventory-result event. topic={}, key={}", topic, key);
                    }
                });
    }
}
