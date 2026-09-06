package com.shopsphere.product.mapper;

import com.shopsphere.product.dto.request.CreateProductRequest;
import com.shopsphere.product.dto.request.UpdateProductRequest;
import com.shopsphere.product.dto.response.CategoryResponse;
import com.shopsphere.product.dto.response.ProductResponse;
import com.shopsphere.product.model.entity.Category;
import com.shopsphere.product.model.entity.Product;
import com.shopsphere.product.model.entity.ProductStatus;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ProductMapper {

    private final CategoryMapper categoryMapper;

    public ProductMapper(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public Product toEntity(CreateProductRequest request, Set<Category> categories) {
        if (request == null) {
            return null;
        }
        return Product.builder()
                .sku(request.sku())
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .currency(request.currency() != null ? request.currency() : "USD")
                .status(request.status() != null ? request.status() : ProductStatus.ACTIVE)
                .categories(categories)
                .build();
    }

    public ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }
        Set<CategoryResponse> categoryResponses = product.getCategories() != null
                ? product.getCategories().stream().map(categoryMapper::toResponse).collect(Collectors.toSet())
                : Set.of();

        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCurrency(),
                product.getStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                categoryResponses
        );
    }

    public void updateEntityFromRequest(UpdateProductRequest request, Product product, Set<Category> categories) {
        if (request == null || product == null) {
            return;
        }
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setCurrency(request.currency());
        if (request.status() != null) {
            product.setStatus(request.status());
        }
        if (categories != null) {
            product.setCategories(categories);
        }
    }
}
