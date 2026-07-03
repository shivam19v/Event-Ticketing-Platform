package com.eventsphere.user.dto;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
@Data @Builder
public class UserResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String avatarUrl;
    private String role;
    private Boolean isVerified;
    private Instant createdAt;
}
