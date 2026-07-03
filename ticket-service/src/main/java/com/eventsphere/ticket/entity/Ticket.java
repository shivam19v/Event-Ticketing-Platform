package com.eventsphere.ticket.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "tickets")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Ticket {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private UUID bookingId;
    @Column(nullable = false) private UUID eventId;
    @Column(nullable = false) private UUID userId;
    @Column(nullable = false, unique = true) private String ticketNumber;
    @Column(nullable = false, columnDefinition = "TEXT") private String qrCodeData;
    @Builder.Default private String status = "VALID";
    @Builder.Default private Instant issuedAt = Instant.now();
    private Instant firstUsedAt;
    private Instant lastUsedAt;
    private Instant createdAt;

    @PrePersist public void prePersist() { if (createdAt == null) createdAt = Instant.now(); }
}
