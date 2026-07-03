package com.eventsphere.user.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "refresh_tokens")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false, unique = true, length = 500) private String token;
    @Column(nullable = false) private Instant expiresAt;
    @Builder.Default private Boolean revoked = false;
    private Instant createdAt;
    @PrePersist public void prePersist() { createdAt = Instant.now(); }
}
