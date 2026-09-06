package com.shopsphere.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.product.dto.request.CreateCategoryRequest;
import com.shopsphere.product.dto.request.UpdateCategoryRequest;
import com.shopsphere.product.model.entity.CategoryStatus;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void createCategory_ShouldReturn201Created() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("Smartphones", "Mobile devices", CategoryStatus.ACTIVE);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Smartphones")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    void createCategory_DuplicateName_ShouldReturn409Conflict() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("Smartphones", "Mobile devices", CategoryStatus.ACTIVE);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("DUPLICATE_RESOURCE")));
    }

    @Test
    void getCategoryById_ShouldReturnCategory() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("Laptops", "Portable PCs", CategoryStatus.ACTIVE);

        MvcResult res = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long categoryId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/categories/{categoryId}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(categoryId.intValue())))
                .andExpect(jsonPath("$.name", is("Laptops")));
    }

    @Test
    void updateCategory_ShouldReturnUpdatedCategory() throws Exception {
        CreateCategoryRequest req = new CreateCategoryRequest("Old Tech", "Desc", CategoryStatus.ACTIVE);

        MvcResult res = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long categoryId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        UpdateCategoryRequest updateReq = new UpdateCategoryRequest("New Tech", "Updated Desc", CategoryStatus.ACTIVE);

        mockMvc.perform(put("/api/categories/{categoryId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("New Tech")))
                .andExpect(jsonPath("$.description", is("Updated Desc")));
    }

    @Test
    void deleteCategory_ShouldDeactivateCategory() throws Exception {
        CreateCategoryRequest req = new CreateCategoryRequest("To Delete", "Desc", CategoryStatus.ACTIVE);

        MvcResult res = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long categoryId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/categories/{categoryId}", categoryId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/categories/{categoryId}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("INACTIVE")));
    }
}
