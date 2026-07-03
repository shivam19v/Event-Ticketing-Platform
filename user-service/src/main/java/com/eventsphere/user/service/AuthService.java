package com.eventsphere.user.service;
import com.eventsphere.user.dto.*;
import com.eventsphere.user.entity.*;
import com.eventsphere.user.exception.ApiException;
import com.eventsphere.user.repository.*;
import com.eventsphere.user.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service @RequiredArgsConstructor @Slf4j
public class AuthService {
    private final UserRepository userRepo;
    private final RefreshTokenRepository tokenRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail()))
            throw new ApiException("Email already registered", HttpStatus.CONFLICT);
        User user = User.builder()
            .email(req.getEmail().toLowerCase())
            .password(encoder.encode(req.getPassword()))
            .firstName(req.getFirstName())
            .lastName(req.getLastName())
            .phone(req.getPhone())
            .build();
        user = userRepo.save(user);
        log.info("New user registered: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = userRepo.findByEmail(req.getEmail().toLowerCase())
            .orElseThrow(() -> new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED));
        if (!user.getIsActive())
            throw new ApiException("Account is deactivated", HttpStatus.FORBIDDEN);
        if (!encoder.matches(req.getPassword(), user.getPassword()))
            throw new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED);
        tokenRepo.revokeAllUserTokens(user.getId());
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        RefreshToken token = tokenRepo.findByToken(refreshToken)
            .orElseThrow(() -> new ApiException("Invalid refresh token", HttpStatus.UNAUTHORIZED));
        if (token.getRevoked() || token.getExpiresAt().isBefore(Instant.now()))
            throw new ApiException("Refresh token expired or revoked", HttpStatus.UNAUTHORIZED);
        token.setRevoked(true);
        tokenRepo.save(token);
        return buildAuthResponse(token.getUser());
    }

    @Transactional
    public void logout(UUID userId) { tokenRepo.revokeAllUserTokens(userId); }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken  = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        RefreshToken rt = RefreshToken.builder()
            .user(user).token(refreshToken)
            .expiresAt(Instant.now().plusMillis(jwtUtil.getRefreshExpiryMs()))
            .build();
        tokenRepo.save(rt);
        return AuthResponse.builder()
            .accessToken(accessToken).refreshToken(refreshToken)
            .expiresIn(jwtUtil.getExpiryMs() / 1000)
            .user(AuthResponse.UserDto.builder()
                .id(user.getId()).email(user.getEmail())
                .firstName(user.getFirstName()).lastName(user.getLastName())
                .role(user.getRole().name()).build())
            .build();
    }
}
