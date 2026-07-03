package com.eventsphere.notification.consumer;

import com.eventsphere.notification.entity.ProcessedEvent;
import com.eventsphere.notification.repository.ProcessedEventRepository;
import com.eventsphere.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.UUID;

@Component @RequiredArgsConstructor @Slf4j
public class BookingEventConsumer {

    private final EmailService emailService;
    private final ProcessedEventRepository processedEventRepo;

    @RabbitListener(queues = "notify.booking.confirmed")
    @Transactional
    public void handleBookingConfirmed(Map<String, String> event) {
        String bookingId = event.get("bookingId");
        String eventKey = "notify:booking.confirmed:" + bookingId;
        if (processedEventRepo.existsById(eventKey)) {
            log.info("Already notified for {}, skipping", eventKey);
            return;
        }
        try {
            String userId = event.get("userId");
            String totalPrice = event.getOrDefault("totalPrice", "0.00");

            emailService.sendTemplatedEmail(
                UUID.fromString(userId),
                userId + "@placeholder.eventsphere.com",
                "Booking Confirmed!",
                "booking_confirmation",
                Map.of("firstName", "Guest", "eventName", "Your Event",
                       "bookingId", bookingId, "totalPrice", totalPrice)
            );
            processedEventRepo.save(ProcessedEvent.builder().eventKey(eventKey).build());
        } catch (Exception e) {
            log.error("Failed to process booking.confirmed notification", e);
            throw e;
        }
    }

    @RabbitListener(queues = "notify.booking.cancelled")
    @Transactional
    public void handleBookingCancelled(Map<String, String> event) {
        String bookingId = event.get("bookingId");
        String eventKey = "notify:booking.cancelled:" + bookingId;
        if (processedEventRepo.existsById(eventKey)) return;
        try {
            String userId = event.get("userId");
            emailService.sendTemplatedEmail(
                UUID.fromString(userId),
                userId + "@placeholder.eventsphere.com",
                "Booking Cancelled",
                "booking_cancelled",
                Map.of("firstName", "Guest", "eventName", "Your Event", "bookingId", bookingId)
            );
            processedEventRepo.save(ProcessedEvent.builder().eventKey(eventKey).build());
        } catch (Exception e) {
            log.error("Failed to process booking.cancelled notification", e);
            throw e;
        }
    }
}
