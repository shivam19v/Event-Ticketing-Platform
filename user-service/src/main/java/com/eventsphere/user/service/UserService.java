package com.eventsphere.user.service;
import com.eventsphere.user.dto.*;
import com.eventsphere.user.entity.User;
import com.eventsphere.user.exception.ApiException;
import com.eventsphere.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepo;

    @Cacheable(value = "users", key = "#id")
    public UserResponse getUser(UUID id) {
        User u = userRepo.findById(id)
            .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        return toResponse(u);
    }

    @CacheEvict(value = "users", key = "#id")
    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest req, UUID requesterId) {
        if (!id.equals(requesterId))
            throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
        User u = userRepo.findById(id)
            .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        if (req.getFirstName() != null) u.setFirstName(req.getFirstName());
        if (req.getLastName()  != null) u.setLastName(req.getLastName());
        if (req.getPhone()     != null) u.setPhone(req.getPhone());
        if (req.getAvatarUrl() != null) u.setAvatarUrl(req.getAvatarUrl());
        return toResponse(userRepo.save(u));
    }

    private UserResponse toResponse(User u) {
        return UserResponse.builder()
            .id(u.getId()).email(u.getEmail())
            .firstName(u.getFirstName()).lastName(u.getLastName())
            .phone(u.getPhone()).avatarUrl(u.getAvatarUrl())
            .role(u.getRole().name()).isVerified(u.getIsVerified())
            .createdAt(u.getCreatedAt()).build();
    }
}
