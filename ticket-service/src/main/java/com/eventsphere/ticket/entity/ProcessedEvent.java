package com.eventsphere.ticket.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "processed_events")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProcessedEvent {
    @Id private String eventKey;
    @Builder.Default private Instant processedAt = Instant.now();
}
