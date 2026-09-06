package com.shopsphere.product.service;

import com.shopsphere.product.dto.request.CreateCategoryRequest;
import com.shopsphere.product.dto.request.UpdateCategoryRequest;
import com.shopsphere.product.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CreateCategoryRequest request);
    List<CategoryResponse> getAllCategories();
    CategoryResponse getCategoryById(Long categoryId);
    CategoryResponse updateCategory(Long categoryId, UpdateCategoryRequest request);
    void deleteCategory(Long categoryId);
}
