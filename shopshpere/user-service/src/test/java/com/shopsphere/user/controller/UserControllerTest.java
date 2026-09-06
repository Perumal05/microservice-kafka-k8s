package com.shopsphere.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.user.dto.request.CreateUserRequest;
import com.shopsphere.user.dto.request.UpdateUserRequest;
import com.shopsphere.user.model.entity.UserStatus;
import com.shopsphere.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void createUser_ShouldReturn201Created() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "alice@example.com",
                "securePass123",
                "Alice",
                "Smith",
                "+1-555-0199",
                UserStatus.ACTIVE
        );

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.email", is("alice@example.com")))
                .andExpect(jsonPath("$.firstName", is("Alice")))
                .andExpect(jsonPath("$.lastName", is("Smith")))
                .andExpect(jsonPath("$.phone", is("+1-555-0199")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void createUser_DuplicateEmail_ShouldReturn409Conflict() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "alice@example.com", "pass12345", "Alice", "Smith", null, UserStatus.ACTIVE);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("DUPLICATE_RESOURCE")))
                .andExpect(jsonPath("$.message", containsString("User already exists with email")));
    }

    @Test
    void createUser_ValidationFailure_ShouldReturn400BadRequest() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "not-an-email", "", "", "", null, null);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    void getUserById_ShouldReturnUser_WhenUserExists() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "bob@example.com", "pass12345", "Bob", "Jones", null, UserStatus.ACTIVE);

        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Long userId = objectMapper.readTree(responseJson).get("id").asLong();

        mockMvc.perform(get("/api/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(userId.intValue())))
                .andExpect(jsonPath("$.email", is("bob@example.com")));
    }

    @Test
    void getUserById_ShouldReturn404_WhenUserDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/users/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    void updateUser_ShouldReturnUpdatedUser() throws Exception {
        CreateUserRequest createReq = new CreateUserRequest(
                "charlie@example.com", "pass12345", "Charlie", "Brown", null, UserStatus.ACTIVE);

        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long userId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        UpdateUserRequest updateReq = new UpdateUserRequest("Charles", "Brown Jr", "555-9999");

        mockMvc.perform(put("/api/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Charles")))
                .andExpect(jsonPath("$.lastName", is("Brown Jr")))
                .andExpect(jsonPath("$.phone", is("555-9999")));
    }
}
