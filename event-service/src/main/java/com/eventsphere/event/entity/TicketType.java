package com.eventsphere.event.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "ticket_types")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketType {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false) private String name;
    private String description;
    @Column(nullable = false) private BigDecimal price;
    @Column(nullable = false) private Integer quantity;
    @Builder.Default private Integer sold = 0;
    private Instant saleStartTime;
    private Instant saleEndTime;
    @CreationTimestamp private Instant createdAt;
}
