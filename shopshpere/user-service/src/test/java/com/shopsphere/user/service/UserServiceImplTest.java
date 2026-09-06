package com.shopsphere.user.service;

import com.shopsphere.user.dto.request.CreateUserRequest;
import com.shopsphere.user.dto.request.UpdateUserRequest;
import com.shopsphere.user.dto.response.UserResponse;
import com.shopsphere.user.exception.DuplicateResourceException;
import com.shopsphere.user.exception.ResourceNotFoundException;
import com.shopsphere.user.mapper.UserMapper;
import com.shopsphere.user.model.entity.User;
import com.shopsphere.user.model.entity.UserStatus;
import com.shopsphere.user.repository.UserRepository;
import com.shopsphere.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    private UserRepository userRepository;
    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userMapper = new UserMapper();
        passwordEncoder = mock(PasswordEncoder.class);
        userService = new UserServiceImpl(userRepository, userMapper, passwordEncoder);
    }

    @Test
    void createUser_Success() {
        CreateUserRequest request = new CreateUserRequest(
                "jane@example.com", "secret123", "Jane", "Doe", "555-0100", UserStatus.ACTIVE);

        User savedEntity = User.builder()
                .id(1L)
                .email("jane@example.com")
                .passwordHash("encoded_pass")
                .firstName("Jane")
                .lastName("Doe")
                .phone("555-0100")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(savedEntity);

        UserResponse result = userService.createUser(request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("jane@example.com", result.email());
        assertEquals("Jane", result.firstName());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_DuplicateEmail_ShouldThrowDuplicateResourceException() {
        CreateUserRequest request = new CreateUserRequest(
                "jane@example.com", "secret123", "Jane", "Doe", "555-0100", UserStatus.ACTIVE);

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.createUser(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserById_Success() {
        User user = User.builder()
                .id(1L)
                .email("jane@example.com")
                .firstName("Jane")
                .lastName("Doe")
                .phone("555-0100")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("jane@example.com", result.email());
    }

    @Test
    void getUserById_NotFound_ShouldThrowResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(99L));
    }

    @Test
    void updateUser_Success() {
        UpdateUserRequest request = new UpdateUserRequest("Janet", "Doe", "555-0200");
        User user = User.builder()
                .id(1L)
                .email("jane@example.com")
                .firstName("Jane")
                .lastName("Doe")
                .phone("555-0100")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse result = userService.updateUser(1L, request);

        assertNotNull(result);
        assertEquals("Janet", result.firstName());
        assertEquals("555-0200", result.phone());
        verify(userRepository).save(user);
    }
}
