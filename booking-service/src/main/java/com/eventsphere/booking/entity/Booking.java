package com.eventsphere.booking.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "bookings")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private UUID reservationId;
    @Column(nullable = false) private UUID userId;
    @Column(nullable = false) private UUID eventId;
    @Column(nullable = false) private UUID ticketTypeId;
    @Column(nullable = false) private Integer quantity;
    @Column(nullable = false) private BigDecimal totalPrice;
    @Column(nullable = false) @Builder.Default private String bookingStatus = "AWAITING_PAYMENT";
    private UUID paymentId;
    @Builder.Default private Instant createdAt = Instant.now();
    @UpdateTimestamp private Instant updatedAt;
    private Instant completedAt;
}
