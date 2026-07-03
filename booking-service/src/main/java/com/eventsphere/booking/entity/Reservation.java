package com.eventsphere.booking.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "reservations")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private UUID userId;
    @Column(nullable = false) private UUID eventId;
    @Column(nullable = false) private UUID ticketTypeId;
    @Column(nullable = false) private Integer quantity;
    @Column(name = "seat_ids") private String seatIds; // comma-separated UUIDs
    @Column(nullable = false) @Builder.Default private String status = "PENDING";
    @Column(nullable = false) private BigDecimal totalPrice;
    @Builder.Default private Instant reservedAt = Instant.now();
    @Column(nullable = false) private Instant expiresAt;
    private Instant confirmedAt;
    private Instant cancelledAt;
}
