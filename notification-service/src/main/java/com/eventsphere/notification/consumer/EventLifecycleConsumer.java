package com.eventsphere.notification.consumer;

import com.eventsphere.notification.entity.ProcessedEvent;
import com.eventsphere.notification.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Component @RequiredArgsConstructor @Slf4j
public class EventLifecycleConsumer {

    private final ProcessedEventRepository processedEventRepo;

    @RabbitListener(queues = "notify.event.cancelled")
    @Transactional
    public void handleEventCancelled(Map<String, String> event) {
        String eventKey = "notify:event.cancelled:" + event.get("eventId");
        if (processedEventRepo.existsById(eventKey)) return;
        log.info("Event cancelled notification for event: {} ({})", event.get("eventId"), event.get("title"));
        processedEventRepo.save(ProcessedEvent.builder().eventKey(eventKey).build());
    }

    @RabbitListener(queues = "notify.refund.processed")
    @Transactional
    public void handleRefundProcessed(Map<String, String> event) {
        String eventKey = "notify:refund.processed:" + event.get("paymentId");
        if (processedEventRepo.existsById(eventKey)) return;
        log.info("Refund processed notification for payment: {}", event.get("paymentId"));
        processedEventRepo.save(ProcessedEvent.builder().eventKey(eventKey).build());
    }
}
