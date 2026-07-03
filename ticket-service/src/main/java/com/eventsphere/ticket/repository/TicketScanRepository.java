package com.eventsphere.ticket.repository;

import com.eventsphere.ticket.entity.TicketScan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TicketScanRepository extends JpaRepository<TicketScan, UUID> {
    List<TicketScan> findByTicketIdOrderByScannedAtDesc(UUID ticketId);
}
