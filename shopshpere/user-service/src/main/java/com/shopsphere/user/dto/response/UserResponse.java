package com.shopsphere.user.dto.response;

import com.shopsphere.user.model.entity.UserStatus;

import java.time.Instant;

public record UserResponse(
    Long id,
    String email,
    String firstName,
    String lastName,
    String phone,
    UserStatus status,
    Instant createdAt,
    Instant updatedAt
) {}
