package com.shopsphere.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.product.dto.request.CreateCategoryRequest;
import com.shopsphere.product.dto.request.CreateProductRequest;
import com.shopsphere.product.dto.request.UpdateProductRequest;
import com.shopsphere.product.model.entity.CategoryStatus;
import com.shopsphere.product.model.entity.ProductStatus;
import com.shopsphere.product.repository.CategoryRepository;
import com.shopsphere.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Long categoryId;

    @BeforeEach
    void setUp() throws Exception {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        CreateCategoryRequest catReq = new CreateCategoryRequest("Electronics", "Tech gadgets", CategoryStatus.ACTIVE);
        MvcResult res = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catReq)))
                .andExpect(status().isCreated())
                .andReturn();

        categoryId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void createProduct_ShouldReturn201Created() throws Exception {
        CreateProductRequest request = new CreateProductRequest(
                "SKU-PROD-1",
                "Ultrabook Pro",
                "15 inch lightweight laptop",
                new BigDecimal("1499.99"),
                "USD",
                ProductStatus.ACTIVE,
                Set.of(categoryId)
        );

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.sku", is("SKU-PROD-1")))
                .andExpect(jsonPath("$.name", is("Ultrabook Pro")))
                .andExpect(jsonPath("$.price", is(1499.99)))
                .andExpect(jsonPath("$.categories", hasSize(1)));
    }

    @Test
    void createProduct_DuplicateSku_ShouldReturn409Conflict() throws Exception {
        CreateProductRequest request = new CreateProductRequest(
                "SKU-PROD-1", "Ultrabook Pro", "Desc", new BigDecimal("1499.99"), "USD", ProductStatus.ACTIVE, Set.of(categoryId));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("DUPLICATE_RESOURCE")));
    }

    @Test
    void createProduct_InvalidPrice_ShouldReturn400BadRequest() throws Exception {
        CreateProductRequest request = new CreateProductRequest(
                "SKU-PROD-ZERO", "Free Product", "Desc", new BigDecimal("0.00"), "USD", ProductStatus.ACTIVE, Set.of(categoryId));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    void getProducts_WithPaginationAndKeywordSearch() throws Exception {
        CreateProductRequest req1 = new CreateProductRequest(
                "SKU-LAPTOP", "Gaming Laptop RTX", "Powerful specs", new BigDecimal("1999.99"), "USD", ProductStatus.ACTIVE, Set.of(categoryId));
        CreateProductRequest req2 = new CreateProductRequest(
                "SKU-PHONE", "Smart Phone X", "Mobile device", new BigDecimal("799.99"), "USD", ProductStatus.ACTIVE, Set.of(categoryId));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "10")
                        .param("keyword", "laptop")
                        .param("categoryId", categoryId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Gaming Laptop RTX")))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    void updateProduct_ShouldReturnUpdatedProduct() throws Exception {
        CreateProductRequest req = new CreateProductRequest(
                "SKU-PROD-2", "Old Laptop", "Desc", new BigDecimal("500.00"), "USD", ProductStatus.ACTIVE, Set.of(categoryId));

        MvcResult res = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long productId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        UpdateProductRequest updateReq = new UpdateProductRequest(
                "Updated Laptop", "New Desc", new BigDecimal("650.00"), "USD", ProductStatus.ACTIVE, Set.of(categoryId));

        mockMvc.perform(put("/api/products/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Laptop")))
                .andExpect(jsonPath("$.price", is(650.00)));
    }

    @Test
    void deleteProduct_ShouldSoftDeleteProduct() throws Exception {
        CreateProductRequest req = new CreateProductRequest(
                "SKU-PROD-3", "To Deactivate", "Desc", new BigDecimal("100.00"), "USD", ProductStatus.ACTIVE, Set.of(categoryId));

        MvcResult res = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long productId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("INACTIVE")));
    }
}
