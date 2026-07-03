package com.eventsphere.ticket.repository;

import com.eventsphere.ticket.entity.Ticket;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    Optional<Ticket> findByTicketNumber(String ticketNumber);
    Optional<Ticket> findByIdAndUserId(UUID id, UUID userId);
    Page<Ticket> findByUserIdOrderByIssuedAtDesc(UUID userId, Pageable pageable);
    List<Ticket> findByBookingId(UUID bookingId);
}
