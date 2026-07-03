package com.eventsphere.event.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "seat_maps")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SeatMap {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private UUID eventId;
    private UUID ticketTypeId;
    @Column(nullable = false) private String sectionName;
    private String rowLabel;
    private Integer seatNumber;
    @Builder.Default private String status = "AVAILABLE";
    private Instant createdAt;

    @PrePersist public void prePersist() { createdAt = Instant.now(); }
}
