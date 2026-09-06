package com.shopsphere.product.service;

import com.shopsphere.product.dto.request.CreateProductRequest;
import com.shopsphere.product.dto.request.UpdateProductRequest;
import com.shopsphere.product.dto.response.PageResponse;
import com.shopsphere.product.dto.response.ProductResponse;
import com.shopsphere.product.exception.BadRequestException;
import com.shopsphere.product.exception.DuplicateResourceException;
import com.shopsphere.product.exception.ResourceNotFoundException;
import com.shopsphere.product.mapper.CategoryMapper;
import com.shopsphere.product.mapper.ProductMapper;
import com.shopsphere.product.model.entity.Category;
import com.shopsphere.product.model.entity.Product;
import com.shopsphere.product.model.entity.ProductStatus;
import com.shopsphere.product.repository.CategoryRepository;
import com.shopsphere.product.repository.ProductRepository;
import com.shopsphere.product.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductServiceImplTest {

    private ProductRepository productRepository;
    private CategoryRepository categoryRepository;
    private ProductMapper productMapper;
    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        CategoryMapper categoryMapper = new CategoryMapper();
        productMapper = new ProductMapper(categoryMapper);
        productService = new ProductServiceImpl(productRepository, categoryRepository, productMapper);
    }

    @Test
    void createProduct_Success() {
        CreateProductRequest request = new CreateProductRequest(
                "SKU-1", "Laptop", "High end", new BigDecimal("1000.00"), "USD", ProductStatus.ACTIVE, Set.of(1L));

        Category category = Category.builder().id(1L).name("Tech").build();
        Product savedProduct = Product.builder()
                .id(10L).sku("SKU-1").name("Laptop").price(new BigDecimal("1000.00")).currency("USD").status(ProductStatus.ACTIVE)
                .createdAt(Instant.now()).updatedAt(Instant.now()).categories(Set.of(category)).build();

        when(productRepository.existsBySku("SKU-1")).thenReturn(false);
        when(categoryRepository.findAllByIdIn(Set.of(1L))).thenReturn(List.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        ProductResponse result = productService.createProduct(request);

        assertNotNull(result);
        assertEquals(10L, result.id());
        assertEquals("SKU-1", result.sku());
    }

    @Test
    void createProduct_DuplicateSku_ShouldThrowException() {
        CreateProductRequest request = new CreateProductRequest(
                "SKU-1", "Laptop", "High end", new BigDecimal("1000.00"), "USD", ProductStatus.ACTIVE, Set.of(1L));

        when(productRepository.existsBySku("SKU-1")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> productService.createProduct(request));
    }

    @Test
    void createProduct_InvalidCategory_ShouldThrowBadRequestException() {
        CreateProductRequest request = new CreateProductRequest(
                "SKU-1", "Laptop", "High end", new BigDecimal("1000.00"), "USD", ProductStatus.ACTIVE, Set.of(99L));

        when(productRepository.existsBySku("SKU-1")).thenReturn(false);
        when(categoryRepository.findAllByIdIn(Set.of(99L))).thenReturn(List.of());

        assertThrows(BadRequestException.class, () -> productService.createProduct(request));
    }

    @Test
    void getProductById_Success() {
        Product product = Product.builder().id(10L).sku("SKU-1").name("Laptop").price(new BigDecimal("1000.00")).currency("USD").status(ProductStatus.ACTIVE).createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        ProductResponse result = productService.getProductById(10L);

        assertNotNull(result);
        assertEquals("Laptop", result.name());
    }

    @Test
    void getProducts_Paginated_Success() {
        Product product = Product.builder().id(10L).sku("SKU-1").name("Laptop").price(new BigDecimal("1000.00")).currency("USD").status(ProductStatus.ACTIVE).createdAt(Instant.now()).updatedAt(Instant.now()).build();
        Page<Product> page = new PageImpl<>(List.of(product));

        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<ProductResponse> result = productService.getProducts(0, 10, "name,asc", "laptop", ProductStatus.ACTIVE, 1L);

        assertEquals(1, result.totalElements());
        assertEquals("Laptop", result.content().get(0).name());
    }

    @Test
    void deleteProduct_DeactivatesProduct() {
        Product product = Product.builder().id(10L).sku("SKU-1").status(ProductStatus.ACTIVE).createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        ProductResponse response = productService.deleteProduct(10L);

        assertEquals(ProductStatus.INACTIVE, response.status());
    }
}
