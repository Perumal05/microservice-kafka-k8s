package com.shopsphere.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.user.dto.request.CreateAddressRequest;
import com.shopsphere.user.dto.request.CreateUserRequest;
import com.shopsphere.user.dto.request.UpdateAddressRequest;
import com.shopsphere.user.model.entity.AddressType;
import com.shopsphere.user.model.entity.UserStatus;
import com.shopsphere.user.repository.AddressRepository;
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
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    private Long userId1;
    private Long userId2;

    @BeforeEach
    void setUp() throws Exception {
        addressRepository.deleteAll();
        userRepository.deleteAll();

        // Create User 1
        CreateUserRequest userReq1 = new CreateUserRequest(
                "user1@example.com", "pass12345", "User", "One", null, UserStatus.ACTIVE);
        MvcResult res1 = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReq1)))
                .andExpect(status().isCreated())
                .andReturn();
        userId1 = objectMapper.readTree(res1.getResponse().getContentAsString()).get("id").asLong();

        // Create User 2
        CreateUserRequest userReq2 = new CreateUserRequest(
                "user2@example.com", "pass12345", "User", "Two", null, UserStatus.ACTIVE);
        MvcResult res2 = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReq2)))
                .andExpect(status().isCreated())
                .andReturn();
        userId2 = objectMapper.readTree(res2.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void createAddress_ShouldReturn201Created() throws Exception {
        CreateAddressRequest request = new CreateAddressRequest(
                "100 Broadway", "Suite 200", "New York", "NY", "10005", "USA", AddressType.WORK, true);

        mockMvc.perform(post("/api/users/{userId}/addresses", userId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.userId", is(userId1.intValue())))
                .andExpect(jsonPath("$.addressLine1", is("100 Broadway")))
                .andExpect(jsonPath("$.type", is("WORK")))
                .andExpect(jsonPath("$.isDefault", is(true)));
    }

    @Test
    void createAddress_WhenNewIsDefault_ShouldUnsetPreviousDefault() throws Exception {
        // Create initial default address
        CreateAddressRequest req1 = new CreateAddressRequest(
                "1st St", null, "City", "State", "10001", "USA", AddressType.HOME, true);
        MvcResult res1 = mockMvc.perform(post("/api/users/{userId}/addresses", userId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andReturn();
        Long addr1Id = objectMapper.readTree(res1.getResponse().getContentAsString()).get("id").asLong();

        // Create second default address
        CreateAddressRequest req2 = new CreateAddressRequest(
                "2nd St", null, "City", "State", "10002", "USA", AddressType.WORK, true);
        mockMvc.perform(post("/api/users/{userId}/addresses", userId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isDefault", is(true)));

        // Verify address 1 is no longer default
        mockMvc.perform(get("/api/users/{userId}/addresses", userId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.id == " + addr1Id + ")].isDefault", contains(false)));
    }

    @Test
    void updateAddress_BelongingToAnotherUser_ShouldReturn400BadRequest() throws Exception {
        // User 2 creates address
        CreateAddressRequest reqUser2 = new CreateAddressRequest(
                "User2 Street", null, "City", "State", "10001", "USA", AddressType.HOME, false);
        MvcResult resUser2 = mockMvc.perform(post("/api/users/{userId}/addresses", userId2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqUser2)))
                .andExpect(status().isCreated())
                .andReturn();
        Long user2AddressId = objectMapper.readTree(resUser2.getResponse().getContentAsString()).get("id").asLong();

        // User 1 attempts to modify User 2's address -> 400 BAD REQUEST
        UpdateAddressRequest updateReq = new UpdateAddressRequest(
                "Hacked Address", null, "City", "State", "10001", "USA", AddressType.HOME, false);

        mockMvc.perform(put("/api/users/{userId}/addresses/{addressId}", userId1, user2AddressId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("does not belong to user id")));
    }

    @Test
    void deleteAddress_ShouldReturn204NoContent() throws Exception {
        CreateAddressRequest req = new CreateAddressRequest(
                "To Delete St", null, "City", "State", "10001", "USA", AddressType.HOME, false);
        MvcResult res = mockMvc.perform(post("/api/users/{userId}/addresses", userId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        Long addressId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/users/{userId}/addresses/{addressId}", userId1, addressId))
                .andExpect(status().isNoContent());

        // Verify address list is empty
        mockMvc.perform(get("/api/users/{userId}/addresses", userId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
