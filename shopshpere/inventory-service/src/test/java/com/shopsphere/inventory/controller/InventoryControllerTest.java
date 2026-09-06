package com.shopsphere.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.inventory.dto.request.CreateInventoryRequest;
import com.shopsphere.inventory.dto.request.ReserveInventoryRequest;
import com.shopsphere.inventory.dto.request.UpdateInventoryRequest;
import com.shopsphere.inventory.repository.InventoryRepository;
import com.shopsphere.inventory.repository.InventoryReservationRepository;
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
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        inventoryRepository.deleteAll();
    }

    @Test
    void createInventory_ShouldReturn201Created() throws Exception {
        CreateInventoryRequest request = new CreateInventoryRequest(10L, "SKU-LAPTOP-01", 50, 5);

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.productId", is(10)))
                .andExpect(jsonPath("$.availableQuantity", is(50)))
                .andExpect(jsonPath("$.salableQuantity", is(50)));
    }

    @Test
    void getInventoryByProductId_ShouldReturnInventory() throws Exception {
        CreateInventoryRequest request = new CreateInventoryRequest(10L, "SKU-LAPTOP-01", 50, 5);
        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/inventory/{productId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is(10)))
                .andExpect(jsonPath("$.sku", is("SKU-LAPTOP-01")));
    }

    @Test
    void updateInventory_ShouldReturnUpdatedStock() throws Exception {
        CreateInventoryRequest request = new CreateInventoryRequest(10L, "SKU-LAPTOP-01", 50, 5);
        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        UpdateInventoryRequest updateReq = new UpdateInventoryRequest(100, 10);

        mockMvc.perform(put("/api/inventory/{productId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity", is(100)))
                .andExpect(jsonPath("$.reorderLevel", is(10)));
    }

    @Test
    void reserveInventory_ShouldReserveStock() throws Exception {
        CreateInventoryRequest request = new CreateInventoryRequest(10L, "SKU-LAPTOP-01", 50, 5);
        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        ReserveInventoryRequest reserveReq = new ReserveInventoryRequest(1000L, 5);

        mockMvc.perform(post("/api/inventory/{productId}/reserve", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reserveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.orderId", is(1000)))
                .andExpect(jsonPath("$.quantity", is(5)))
                .andExpect(jsonPath("$.status", is("RESERVED")));
    }

    @Test
    void reserveInventory_InsufficientStock_ShouldReturn400BadRequest() throws Exception {
        CreateInventoryRequest request = new CreateInventoryRequest(10L, "SKU-LAPTOP-01", 3, 5);
        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        ReserveInventoryRequest reserveReq = new ReserveInventoryRequest(1000L, 10);

        mockMvc.perform(post("/api/inventory/{productId}/reserve", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reserveReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INSUFFICIENT_INVENTORY")));
    }

    @Test
    void releaseAndConfirmReservation_ShouldProcessStateTransitions() throws Exception {
        CreateInventoryRequest request = new CreateInventoryRequest(10L, "SKU-LAPTOP-01", 50, 5);
        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        ReserveInventoryRequest reserveReq = new ReserveInventoryRequest(1000L, 5);
        MvcResult res = mockMvc.perform(post("/api/inventory/{productId}/reserve", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reserveReq)))
                .andExpect(status().isOk())
                .andReturn();

        Long reservationId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        // Release reservation
        mockMvc.perform(post("/api/inventory/reservations/{reservationId}/release", reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RELEASED")));
    }
}
