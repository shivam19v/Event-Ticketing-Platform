package com.eventsphere.user.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "users")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true) private String email;
    @Column(nullable = false) private String password;
    private String firstName;
    private String lastName;
    private String phone;
    private String avatarUrl;
    @Column(nullable = false) @Enumerated(EnumType.STRING) @Builder.Default
    private Role role = Role.ATTENDEE;
    @Builder.Default private Boolean isVerified = false;
    @Builder.Default private Boolean isActive = true;
    @CreationTimestamp private Instant createdAt;
    @UpdateTimestamp  private Instant updatedAt;
    public enum Role { ADMIN, ORGANIZER, ATTENDEE, STAFF }
}
