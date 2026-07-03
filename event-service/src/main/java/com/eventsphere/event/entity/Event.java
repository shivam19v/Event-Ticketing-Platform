package com.eventsphere.event.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity @Table(name = "events")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Event {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false) private UUID organizerId;
    @Column(nullable = false) private String title;
    @Column(columnDefinition = "TEXT") private String description;
    private String category;
    private String imageUrl;
    @Column(nullable = false) @Builder.Default private String status = "DRAFT";
    @Column(nullable = false) private Instant startTime;
    @Column(nullable = false) private Instant endTime;
    @CreationTimestamp private Instant createdAt;
    @UpdateTimestamp  private Instant updatedAt;

    @OneToOne(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Venue venue;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default private List<TicketType> ticketTypes = new ArrayList<>();
}
