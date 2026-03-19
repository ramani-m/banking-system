package com.ramani.banking.notification.consumer;

import com.ramani.banking.notification.dto.TransactionEvent;
import com.ramani.banking.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "transaction-events",
            groupId = "notification-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onTransactionEvent(
            @Payload TransactionEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received transaction event: id={} status={} from topic={} partition={} offset={}",
                event.getTransactionId(), event.getStatus(), topic, partition, offset);

        try {
            notificationService.handleTransactionEvent(event);
        } catch (Exception e) {
            log.error("Failed to process transaction event {}: {}", event.getTransactionId(), e.getMessage(), e);
            // In production: send to dead-letter topic
        }
    }
}
