package com.eventsphere.ticket.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "ticket_scans")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketScan {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private UUID ticketId;
    @Column(nullable = false) private UUID eventId;
    private UUID scannedBy;
    @Builder.Default private Instant scannedAt = Instant.now();
    private String location;
    private String deviceId;
    @Builder.Default private String result = "SUCCESS";
}
