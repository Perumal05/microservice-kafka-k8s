package com.shopsphere.user.service;

import com.shopsphere.user.dto.request.CreateUserRequest;
import com.shopsphere.user.dto.request.UpdateUserRequest;
import com.shopsphere.user.dto.response.UserResponse;

public interface UserService {
    UserResponse createUser(CreateUserRequest request);
    UserResponse getUserById(Long userId);
    UserResponse updateUser(Long userId, UpdateUserRequest request);
}
