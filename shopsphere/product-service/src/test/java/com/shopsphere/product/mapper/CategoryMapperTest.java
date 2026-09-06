package com.shopsphere.product.mapper;

import com.shopsphere.product.dto.request.CreateCategoryRequest;
import com.shopsphere.product.dto.request.UpdateCategoryRequest;
import com.shopsphere.product.dto.response.CategoryResponse;
import com.shopsphere.product.model.entity.Category;
import com.shopsphere.product.model.entity.CategoryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CategoryMapperTest {

    private CategoryMapper categoryMapper;

    @BeforeEach
    void setUp() {
        categoryMapper = new CategoryMapper();
    }

    @Test
    void toEntity_ShouldMapCreateCategoryRequestToCategory() {
        CreateCategoryRequest request = new CreateCategoryRequest("Electronics", "Gadgets and devices", CategoryStatus.ACTIVE);

        Category category = categoryMapper.toEntity(request);

        assertNotNull(category);
        assertEquals("Electronics", category.getName());
        assertEquals("Gadgets and devices", category.getDescription());
        assertEquals(CategoryStatus.ACTIVE, category.getStatus());
    }

    @Test
    void toResponse_ShouldMapCategoryToCategoryResponse() {
        Instant now = Instant.now();
        Category category = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Gadgets")
                .status(CategoryStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        CategoryResponse response = categoryMapper.toResponse(category);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Electronics", response.name());
        assertEquals("Gadgets", response.description());
        assertEquals(CategoryStatus.ACTIVE, response.status());
    }

    @Test
    void updateEntityFromRequest_ShouldUpdateFields() {
        Category category = Category.builder()
                .name("Old Name")
                .description("Old Desc")
                .status(CategoryStatus.ACTIVE)
                .build();

        UpdateCategoryRequest request = new UpdateCategoryRequest("New Name", "New Desc", CategoryStatus.INACTIVE);

        categoryMapper.updateEntityFromRequest(request, category);

        assertEquals("New Name", category.getName());
        assertEquals("New Desc", category.getDescription());
        assertEquals(CategoryStatus.INACTIVE, category.getStatus());
    }
}
