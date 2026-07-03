package com.eventsphere.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "sent_notifications")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SentNotification {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private UUID userId;
    @Column(nullable = false) private String template;
    @Builder.Default private String channel = "email";
    @Column(nullable = false) private String recipient;
    private String subject;
    @Builder.Default private String status = "SENT";
    private String errorMessage;
    @Builder.Default private Instant sentAt = Instant.now();
}
