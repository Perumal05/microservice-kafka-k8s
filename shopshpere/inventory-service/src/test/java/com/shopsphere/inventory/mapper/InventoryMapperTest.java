package com.shopsphere.inventory.mapper;

import com.shopsphere.inventory.dto.request.CreateInventoryRequest;
import com.shopsphere.inventory.dto.request.UpdateInventoryRequest;
import com.shopsphere.inventory.dto.response.AvailabilityResponse;
import com.shopsphere.inventory.dto.response.InventoryResponse;
import com.shopsphere.inventory.model.entity.Inventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class InventoryMapperTest {

    private InventoryMapper inventoryMapper;

    @BeforeEach
    void setUp() {
        inventoryMapper = new InventoryMapper();
    }

    @Test
    void toEntity_ShouldMapCreateInventoryRequestToInventory() {
        CreateInventoryRequest request = new CreateInventoryRequest(10L, "SKU-100", 50, 10);

        Inventory inventory = inventoryMapper.toEntity(request);

        assertNotNull(inventory);
        assertEquals(10L, inventory.getProductId());
        assertEquals("SKU-100", inventory.getSku());
        assertEquals(50, inventory.getAvailableQuantity());
        assertEquals(0, inventory.getReservedQuantity());
        assertEquals(10, inventory.getReorderLevel());
    }

    @Test
    void toResponse_ShouldMapInventoryToInventoryResponse() {
        Instant now = Instant.now();
        Inventory inventory = Inventory.builder()
                .id(1L)
                .productId(10L)
                .sku("SKU-100")
                .availableQuantity(50)
                .reservedQuantity(10)
                .reorderLevel(5)
                .createdAt(now)
                .updatedAt(now)
                .build();

        InventoryResponse response = inventoryMapper.toResponse(inventory);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(50, response.availableQuantity());
        assertEquals(10, response.reservedQuantity());
        assertEquals(40, response.salableQuantity());
    }

    @Test
    void toAvailabilityResponse_ShouldCalculateSalableAndAvailability() {
        Inventory inventory = Inventory.builder()
                .productId(10L)
                .sku("SKU-100")
                .availableQuantity(50)
                .reservedQuantity(10)
                .build();

        AvailabilityResponse response = inventoryMapper.toAvailabilityResponse(inventory);

        assertNotNull(response);
        assertEquals(40, response.salableQuantity());
        assertTrue(response.isAvailable());
    }

    @Test
    void updateEntityFromRequest_ShouldUpdateFields() {
        Inventory inventory = Inventory.builder()
                .availableQuantity(50)
                .reorderLevel(5)
                .build();
        UpdateInventoryRequest request = new UpdateInventoryRequest(100, 15);

        inventoryMapper.updateEntityFromRequest(request, inventory);

        assertEquals(100, inventory.getAvailableQuantity());
        assertEquals(15, inventory.getReorderLevel());
    }
}
