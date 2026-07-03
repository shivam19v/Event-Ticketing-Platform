package com.eventsphere.ticket.consumer;
import com.eventsphere.ticket.entity.ProcessedEvent;
import com.eventsphere.ticket.repository.ProcessedEventRepository;
import com.eventsphere.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.UUID;

@Component @RequiredArgsConstructor @Slf4j
public class BookingEventConsumer {
    private final TicketService ticketService;
    private final ProcessedEventRepository processedEventRepo;

    @RabbitListener(queues = "ticket.booking.confirmed")
    @Transactional
    public void handleBookingConfirmed(Map<String, String> event) {
        String bookingId = event.get("bookingId");
        String eventKey = "booking.confirmed:" + bookingId;

        // Idempotency: skip if we already issued tickets for this booking
        if (processedEventRepo.existsById(eventKey)) {
            log.info("Event {} already processed, skipping (idempotent)", eventKey);
            return;
        }

        try {
            UUID bookingUuid = UUID.fromString(bookingId);
            UUID eventUuid = UUID.fromString(event.get("eventId"));
            UUID userUuid = UUID.fromString(event.get("userId"));
            int quantity = event.containsKey("quantity") ? Integer.parseInt(event.get("quantity")) : 1;

            ticketService.issueTickets(bookingUuid, eventUuid, userUuid, quantity);
            processedEventRepo.save(ProcessedEvent.builder().eventKey(eventKey).build());

            log.info("Tickets issued for booking: {}", bookingId);
        } catch (Exception e) {
            log.error("Failed to process booking.confirmed for booking {}", bookingId, e);
            throw e; // triggers RabbitMQ retry / DLQ
        }
    }
}
