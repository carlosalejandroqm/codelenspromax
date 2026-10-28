package com.codelens.user.dto;

import com.codelens.user.model.User;

import java.time.Instant;

public record UserDto(
        String id,
        String username,
        String email,
        String fullName,
        Instant createdAt,
        Instant lastLoginAt
) {
    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getUsername(), user.getEmail(), user.getFullName(),
                user.getCreatedAt(), user.getLastLoginAt());
    }
}
