package com.shopsphere.product.service;

import com.shopsphere.product.dto.request.CreateCategoryRequest;
import com.shopsphere.product.dto.request.UpdateCategoryRequest;
import com.shopsphere.product.dto.response.CategoryResponse;
import com.shopsphere.product.exception.DuplicateResourceException;
import com.shopsphere.product.exception.ResourceNotFoundException;
import com.shopsphere.product.mapper.CategoryMapper;
import com.shopsphere.product.model.entity.Category;
import com.shopsphere.product.model.entity.CategoryStatus;
import com.shopsphere.product.repository.CategoryRepository;
import com.shopsphere.product.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CategoryServiceImplTest {

    private CategoryRepository categoryRepository;
    private CategoryMapper categoryMapper;
    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryRepository = mock(CategoryRepository.class);
        categoryMapper = new CategoryMapper();
        categoryService = new CategoryServiceImpl(categoryRepository, categoryMapper);
    }

    @Test
    void createCategory_Success() {
        CreateCategoryRequest request = new CreateCategoryRequest("Electronics", "Gadgets", CategoryStatus.ACTIVE);
        Category savedCategory = Category.builder().id(1L).name("Electronics").description("Gadgets").status(CategoryStatus.ACTIVE).createdAt(Instant.now()).updatedAt(Instant.now()).build();

        when(categoryRepository.existsByName("Electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        CategoryResponse result = categoryService.createCategory(request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Electronics", result.name());
    }

    @Test
    void createCategory_DuplicateName_ThrowsException() {
        CreateCategoryRequest request = new CreateCategoryRequest("Electronics", "Gadgets", CategoryStatus.ACTIVE);
        when(categoryRepository.existsByName("Electronics")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> categoryService.createCategory(request));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void getCategoryById_Success() {
        Category category = Category.builder().id(1L).name("Electronics").status(CategoryStatus.ACTIVE).createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        CategoryResponse result = categoryService.getCategoryById(1L);

        assertNotNull(result);
        assertEquals("Electronics", result.name());
    }

    @Test
    void getCategoryById_NotFound_ThrowsException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.getCategoryById(99L));
    }

    @Test
    void deleteCategory_DeactivatesCategory() {
        Category category = Category.builder().id(1L).name("Electronics").status(CategoryStatus.ACTIVE).build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        categoryService.deleteCategory(1L);

        assertEquals(CategoryStatus.INACTIVE, category.getStatus());
        verify(categoryRepository).save(category);
    }
}
