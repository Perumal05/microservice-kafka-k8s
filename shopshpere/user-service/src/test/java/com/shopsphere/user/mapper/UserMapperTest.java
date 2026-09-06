package com.shopsphere.user.mapper;

import com.shopsphere.user.dto.request.CreateUserRequest;
import com.shopsphere.user.dto.request.UpdateUserRequest;
import com.shopsphere.user.dto.response.UserResponse;
import com.shopsphere.user.model.entity.User;
import com.shopsphere.user.model.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
    }

    @Test
    void toEntity_ShouldMapCreateUserRequestToUser() {
        CreateUserRequest request = new CreateUserRequest(
                "test@example.com",
                "password123",
                "John",
                "Doe",
                "+123456789",
                UserStatus.ACTIVE
        );

        User user = userMapper.toEntity(request, "hashed_password");

        assertNotNull(user);
        assertEquals("test@example.com", user.getEmail());
        assertEquals("hashed_password", user.getPasswordHash());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("+123456789", user.getPhone());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    void toResponse_ShouldMapUserToUserResponseWithoutPassword() {
        Instant now = Instant.now();
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hashed_password")
                .firstName("John")
                .lastName("Doe")
                .phone("+123456789")
                .status(UserStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        UserResponse response = userMapper.toResponse(user);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("test@example.com", response.email());
        assertEquals("John", response.firstName());
        assertEquals("Doe", response.lastName());
        assertEquals("+123456789", response.phone());
        assertEquals(UserStatus.ACTIVE, response.status());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());
    }

    @Test
    void updateEntityFromRequest_ShouldUpdateFields() {
        User user = User.builder()
                .firstName("OldFirst")
                .lastName("OldLast")
                .phone("111111")
                .build();

        UpdateUserRequest request = new UpdateUserRequest("NewFirst", "NewLast", "222222");

        userMapper.updateEntityFromRequest(request, user);

        assertEquals("NewFirst", user.getFirstName());
        assertEquals("NewLast", user.getLastName());
        assertEquals("222222", user.getPhone());
    }
}
