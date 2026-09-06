package com.shopsphere.product.mapper;

import com.shopsphere.product.dto.request.CreateProductRequest;
import com.shopsphere.product.dto.request.UpdateProductRequest;
import com.shopsphere.product.dto.response.ProductResponse;
import com.shopsphere.product.model.entity.Category;
import com.shopsphere.product.model.entity.CategoryStatus;
import com.shopsphere.product.model.entity.Product;
import com.shopsphere.product.model.entity.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProductMapperTest {

    private CategoryMapper categoryMapper;
    private ProductMapper productMapper;

    @BeforeEach
    void setUp() {
        categoryMapper = new CategoryMapper();
        productMapper = new ProductMapper(categoryMapper);
    }

    @Test
    void toEntity_ShouldMapCreateProductRequestToProduct() {
        Category category = Category.builder().id(1L).name("Laptops").build();
        CreateProductRequest request = new CreateProductRequest(
                "SKU-100", "Gaming Laptop", "High performance", new BigDecimal("1299.99"), "USD", ProductStatus.ACTIVE, Set.of(1L));

        Product product = productMapper.toEntity(request, Set.of(category));

        assertNotNull(product);
        assertEquals("SKU-100", product.getSku());
        assertEquals("Gaming Laptop", product.getName());
        assertEquals(new BigDecimal("1299.99"), product.getPrice());
        assertEquals(1, product.getCategories().size());
    }

    @Test
    void toResponse_ShouldMapProductToProductResponse() {
        Instant now = Instant.now();
        Category category = Category.builder().id(1L).name("Laptops").status(CategoryStatus.ACTIVE).createdAt(now).updatedAt(now).build();
        Product product = Product.builder()
                .id(10L)
                .sku("SKU-100")
                .name("Gaming Laptop")
                .description("High performance")
                .price(new BigDecimal("1299.99"))
                .currency("USD")
                .status(ProductStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .categories(Set.of(category))
                .build();

        ProductResponse response = productMapper.toResponse(product);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("SKU-100", response.sku());
        assertEquals(1, response.categories().size());
    }

    @Test
    void updateEntityFromRequest_ShouldUpdateFields() {
        Category category = Category.builder().id(2L).name("Gadgets").build();
        Product product = Product.builder()
                .name("Old Name")
                .price(new BigDecimal("100.00"))
                .build();

        UpdateProductRequest request = new UpdateProductRequest(
                "New Name", "New Desc", new BigDecimal("150.00"), "USD", ProductStatus.ACTIVE, Set.of(2L));

        productMapper.updateEntityFromRequest(request, product, Set.of(category));

        assertEquals("New Name", product.getName());
        assertEquals(new BigDecimal("150.00"), product.getPrice());
        assertEquals(1, product.getCategories().size());
    }
}
