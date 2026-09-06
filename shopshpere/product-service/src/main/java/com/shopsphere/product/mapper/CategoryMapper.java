package com.shopsphere.product.mapper;

import com.shopsphere.product.dto.request.CreateCategoryRequest;
import com.shopsphere.product.dto.request.UpdateCategoryRequest;
import com.shopsphere.product.dto.response.CategoryResponse;
import com.shopsphere.product.model.entity.Category;
import com.shopsphere.product.model.entity.CategoryStatus;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public Category toEntity(CreateCategoryRequest request) {
        if (request == null) {
            return null;
        }
        return Category.builder()
                .name(request.name())
                .description(request.description())
                .status(request.status() != null ? request.status() : CategoryStatus.ACTIVE)
                .build();
    }

    public CategoryResponse toResponse(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getStatus(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    public void updateEntityFromRequest(UpdateCategoryRequest request, Category category) {
        if (request == null || category == null) {
            return;
        }
        category.setName(request.name());
        category.setDescription(request.description());
        if (request.status() != null) {
            category.setStatus(request.status());
        }
    }
}
