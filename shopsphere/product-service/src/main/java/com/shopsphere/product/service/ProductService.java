package com.shopsphere.product.service;

import com.shopsphere.product.dto.request.CreateProductRequest;
import com.shopsphere.product.dto.request.UpdateProductRequest;
import com.shopsphere.product.dto.response.PageResponse;
import com.shopsphere.product.dto.response.ProductResponse;
import com.shopsphere.product.model.entity.ProductStatus;

public interface ProductService {
    ProductResponse createProduct(CreateProductRequest request);
    ProductResponse getProductById(Long productId);
    PageResponse<ProductResponse> getProducts(int page, int size, String sort, String keyword, ProductStatus status, Long categoryId);
    ProductResponse updateProduct(Long productId, UpdateProductRequest request);
    ProductResponse deleteProduct(Long productId);
}
