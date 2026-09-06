package com.shopsphere.user.mapper;

import com.shopsphere.user.dto.request.CreateUserRequest;
import com.shopsphere.user.dto.request.UpdateUserRequest;
import com.shopsphere.user.dto.response.UserResponse;
import com.shopsphere.user.model.entity.User;
import com.shopsphere.user.model.entity.UserStatus;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(CreateUserRequest request, String passwordHash) {
        if (request == null) {
            return null;
        }
        return User.builder()
                .email(request.email())
                .passwordHash(passwordHash)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .status(request.status() != null ? request.status() : UserStatus.ACTIVE)
                .build();
    }

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public void updateEntityFromRequest(UpdateUserRequest request, User user) {
        if (request == null || user == null) {
            return;
        }
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
    }
}
