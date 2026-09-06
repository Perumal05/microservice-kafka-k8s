package com.shopsphere.cart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.cart.dto.request.CreateCartItemRequest;
import com.shopsphere.cart.dto.request.UpdateCartItemRequest;
import com.shopsphere.cart.repository.CartItemRepository;
import com.shopsphere.cart.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    private static final String USER_HEADER = "X-User-Id";
    private static final long USER_ID = 42L;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
    }

    // ── GET /api/cart ───────────────────────────────────────────────

    @Test
    void getCart_NewUser_ShouldCreateAndReturnEmptyCart() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .header(USER_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is((int) USER_ID)))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalAmount", is(0)));
    }

    @Test
    void getCart_MissingUserIdHeader_ShouldReturn400() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("MISSING_HEADER")));
    }

    // ── POST /api/cart/items ────────────────────────────────────────

    @Test
    void addItem_ValidRequest_ShouldReturn200WithItem() throws Exception {
        CreateCartItemRequest request = new CreateCartItemRequest(101L, 2, new BigDecimal("75.00"));

        mockMvc.perform(post("/api/cart/items")
                        .header(USER_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productId", is(101)))
                .andExpect(jsonPath("$.items[0].quantity", is(2)))
                .andExpect(jsonPath("$.items[0].unitPrice", is(75.00)))
                .andExpect(jsonPath("$.items[0].totalPrice", is(150.00)))
                .andExpect(jsonPath("$.totalAmount", is(150.00)));
    }

    @Test
    void addItem_SameProductTwice_ShouldAccumulateQuantity() throws Exception {
        CreateCartItemRequest req = new CreateCartItemRequest(101L, 2, new BigDecimal("50.00"));

        // First add
        mockMvc.perform(post("/api/cart/items")
                        .header(USER_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Second add of same product
        mockMvc.perform(post("/api/cart/items")
                        .header(USER_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity", is(4)));
    }

    @Test
    void addItem_ValidationError_MissingProductId_ShouldReturn400() throws Exception {
        String invalidJson = """
                {"quantity": 1, "unitPrice": 10.00}
                """;

        mockMvc.perform(post("/api/cart/items")
                        .header(USER_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    void addItem_ValidationError_ZeroQuantity_ShouldReturn400() throws Exception {
        CreateCartItemRequest request = new CreateCartItemRequest(101L, 0, new BigDecimal("10.00"));

        mockMvc.perform(post("/api/cart/items")
                        .header(USER_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    // ── PUT /api/cart/items/{itemId} ───────────────────────────────

    @Test
    void updateItem_ValidRequest_ShouldUpdateQuantity() throws Exception {
        // Add an item first
        CreateCartItemRequest addReq = new CreateCartItemRequest(101L, 2, new BigDecimal("50.00"));
        MvcResult result = mockMvc.perform(post("/api/cart/items")
                        .header(USER_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk())
                .andReturn();

        Long itemId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("items").get(0).get("itemId").asLong();

        UpdateCartItemRequest updateReq = new UpdateCartItemRequest(10);
        mockMvc.perform(put("/api/cart/items/{itemId}", itemId)
                        .header(USER_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity", is(10)))
                .andExpect(jsonPath("$.totalAmount", is(500.00)));
    }

    @Test
    void updateItem_NotFound_ShouldReturn404() throws Exception {
        // Ensure cart exists first (so we get past getOrCreate)
        mockMvc.perform(get("/api/cart").header(USER_HEADER, USER_ID));

        UpdateCartItemRequest updateReq = new UpdateCartItemRequest(5);
        mockMvc.perform(put("/api/cart/items/9999")
                        .header(USER_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("RESOURCE_NOT_FOUND")));
    }

    // ── DELETE /api/cart/items/{itemId} ───────────────────────────

    @Test
    void removeItem_ValidItem_ShouldRemoveAndReturnEmptyCart() throws Exception {
        CreateCartItemRequest addReq = new CreateCartItemRequest(101L, 1, new BigDecimal("99.00"));
        MvcResult result = mockMvc.perform(post("/api/cart/items")
                        .header(USER_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk())
                .andReturn();

        Long itemId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("items").get(0).get("itemId").asLong();

        mockMvc.perform(delete("/api/cart/items/{itemId}", itemId)
                        .header(USER_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalAmount", is(0)));
    }

    @Test
    void removeItem_NotFound_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/cart").header(USER_HEADER, USER_ID));

        mockMvc.perform(delete("/api/cart/items/9999")
                        .header(USER_HEADER, USER_ID))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/cart ───────────────────────────────────────────

    @Test
    void clearCart_ShouldReturn204NoContent() throws Exception {
        // Add an item so there's a real active cart to clear
        CreateCartItemRequest addReq = new CreateCartItemRequest(101L, 1, new BigDecimal("10.00"));
        mockMvc.perform(post("/api/cart/items")
                        .header(USER_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/cart")
                        .header(USER_HEADER, USER_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void clearCart_NoCartExists_ShouldReturn204NoContent() throws Exception {
        // No prior cart for this user — should still return 204 gracefully
        mockMvc.perform(delete("/api/cart")
                        .header(USER_HEADER, 999L))
                .andExpect(status().isNoContent());
    }
}
