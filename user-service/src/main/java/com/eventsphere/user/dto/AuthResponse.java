package com.eventsphere.user.dto;
import lombok.*;
import java.util.UUID;
@Data @Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private UserDto user;
    @Data @Builder
    public static class UserDto {
        private UUID id;
        private String email;
        private String firstName;
        private String lastName;
        private String role;
    }
}
