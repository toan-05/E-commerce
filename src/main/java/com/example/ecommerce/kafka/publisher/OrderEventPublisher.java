package com.example.ecommerce.kafka.publisher;

import com.example.ecommerce.config.AppProperties;
import com.example.ecommerce.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AppProperties appProperties;

    public void publishOrderCreated(OrderCreatedEvent event) {
        String key = String.valueOf(event.getOrderId());
        String topic = appProperties.getOrderCreatedTopic();
        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish order-created event. orderId={}", event.getOrderId(), ex);
                    } else {
                        log.info("Published order-created event. topic={}, key={}", topic, key);
                    }
                });
    }
}
