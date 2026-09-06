package com.shopsphere.product.service.impl;

import com.shopsphere.product.dto.request.CreateProductRequest;
import com.shopsphere.product.dto.request.UpdateProductRequest;
import com.shopsphere.product.dto.response.PageResponse;
import com.shopsphere.product.dto.response.ProductResponse;
import com.shopsphere.product.exception.BadRequestException;
import com.shopsphere.product.exception.DuplicateResourceException;
import com.shopsphere.product.exception.ResourceNotFoundException;
import com.shopsphere.product.mapper.ProductMapper;
import com.shopsphere.product.model.entity.Category;
import com.shopsphere.product.model.entity.Product;
import com.shopsphere.product.model.entity.ProductStatus;
import com.shopsphere.product.repository.CategoryRepository;
import com.shopsphere.product.repository.ProductRepository;
import com.shopsphere.product.service.ProductService;
import jakarta.persistence.criteria.Join;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    public ProductServiceImpl(ProductRepository productRepository,
                              CategoryRepository categoryRepository,
                              ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productMapper = productMapper;
    }

    @Override
    public ProductResponse createProduct(CreateProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateResourceException("Product already exists with SKU: " + request.sku());
        }

        Set<Category> categories = validateAndGetCategories(request.categoryIds());

        Product product = productMapper.toEntity(request, categories);
        Product savedProduct = productRepository.save(product);

        return productMapper.toResponse(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProducts(int page, int size, String sort, String keyword, ProductStatus status, Long categoryId) {
        Pageable pageable = createPageable(page, size, sort);

        Specification<Product> spec = Specification.where(null);

        // Deactivated / discontinued products shouldn't appear in default search unless status explicitly requested
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        } else {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), ProductStatus.ACTIVE));
        }

        if (keyword != null && !keyword.isBlank()) {
            String searchPattern = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), searchPattern),
                    cb.like(cb.lower(root.get("description")), searchPattern)
            ));
        }

        if (categoryId != null) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                Join<Product, Category> categoryJoin = root.join("categories");
                return cb.equal(categoryJoin.get("id"), categoryId);
            });
        }

        Page<ProductResponse> productPage = productRepository.findAll(spec, pageable)
                .map(productMapper::toResponse);

        return PageResponse.from(productPage);
    }

    @Override
    public ProductResponse updateProduct(Long productId, UpdateProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        Set<Category> categories = validateAndGetCategories(request.categoryIds());

        productMapper.updateEntityFromRequest(request, product, categories);
        Product updatedProduct = productRepository.save(product);

        return productMapper.toResponse(updatedProduct);
    }

    @Override
    public ProductResponse deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        product.setStatus(ProductStatus.INACTIVE);
        Product savedProduct = productRepository.save(product);

        return productMapper.toResponse(savedProduct);
    }

    private Set<Category> validateAndGetCategories(Set<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new BadRequestException("Product must belong to at least one category");
        }
        List<Category> categoryList = categoryRepository.findAllByIdIn(categoryIds);
        if (categoryList.size() != categoryIds.size()) {
            throw new BadRequestException("One or more category IDs are invalid");
        }
        return new HashSet<>(categoryList);
    }

    private Pageable createPageable(int page, int size, String sort) {
        if (sort == null || sort.isBlank()) {
            return PageRequest.of(page, size, Sort.by("name").ascending());
        }
        String[] sortParams = sort.split(",");
        String property = sortParams[0].trim();
        Sort.Direction direction = (sortParams.length > 1 && sortParams[1].trim().equalsIgnoreCase("desc"))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, property));
    }
}
