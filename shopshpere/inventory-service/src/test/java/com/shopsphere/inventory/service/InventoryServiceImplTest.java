package com.shopsphere.inventory.service;

import com.shopsphere.inventory.dto.request.CreateInventoryRequest;
import com.shopsphere.inventory.dto.request.UpdateInventoryRequest;
import com.shopsphere.inventory.dto.response.AvailabilityResponse;
import com.shopsphere.inventory.dto.response.InventoryResponse;
import com.shopsphere.inventory.exception.DuplicateResourceException;
import com.shopsphere.inventory.exception.ResourceNotFoundException;
import com.shopsphere.inventory.mapper.InventoryMapper;
import com.shopsphere.inventory.model.entity.Inventory;
import com.shopsphere.inventory.repository.InventoryRepository;
import com.shopsphere.inventory.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InventoryServiceImplTest {

    private InventoryRepository inventoryRepository;
    private InventoryMapper inventoryMapper;
    private InventoryServiceImpl inventoryService;

    @BeforeEach
    void setUp() {
        inventoryRepository = mock(InventoryRepository.class);
        inventoryMapper = new InventoryMapper();
        inventoryService = new InventoryServiceImpl(inventoryRepository, inventoryMapper);
    }

    @Test
    void createInventory_Success() {
        CreateInventoryRequest request = new CreateInventoryRequest(10L, "SKU-100", 50, 5);

        when(inventoryRepository.existsByProductId(10L)).thenReturn(false);
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> {
            Inventory item = inv.getArgument(0);
            item.setId(1L);
            item.setCreatedAt(Instant.now());
            item.setUpdatedAt(Instant.now());
            return item;
        });

        InventoryResponse response = inventoryService.createInventory(request);

        assertNotNull(response);
        assertEquals(10L, response.productId());
        assertEquals(50, response.availableQuantity());
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void createInventory_DuplicateProductId_ThrowsDuplicateResourceException() {
        CreateInventoryRequest request = new CreateInventoryRequest(10L, "SKU-100", 50, 5);
        when(inventoryRepository.existsByProductId(10L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> inventoryService.createInventory(request));
    }

    @Test
    void getInventoryByProductId_Success() {
        Inventory inventory = Inventory.builder()
                .id(1L).productId(10L).sku("SKU-100").availableQuantity(50).reservedQuantity(5).reorderLevel(2)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));

        InventoryResponse response = inventoryService.getInventoryByProductId(10L);

        assertNotNull(response);
        assertEquals("SKU-100", response.sku());
        assertEquals(45, response.salableQuantity());
    }

    @Test
    void getInventoryByProductId_NotFound_ThrowsResourceNotFoundException() {
        when(inventoryRepository.findByProductId(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> inventoryService.getInventoryByProductId(99L));
    }

    @Test
    void updateInventory_Success() {
        Inventory inventory = Inventory.builder()
                .id(1L).productId(10L).sku("SKU-100").availableQuantity(50).reservedQuantity(5).reorderLevel(2)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        UpdateInventoryRequest request = new UpdateInventoryRequest(100, 10);

        when(inventoryRepository.findByProductIdForUpdate(10L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(inventory)).thenReturn(inventory);

        InventoryResponse response = inventoryService.updateInventory(10L, request);

        assertEquals(100, response.availableQuantity());
        assertEquals(10, response.reorderLevel());
    }

    @Test
    void checkAvailability_Success() {
        Inventory inventory = Inventory.builder()
                .productId(10L).sku("SKU-100").availableQuantity(50).reservedQuantity(10)
                .build();
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));

        AvailabilityResponse response = inventoryService.checkAvailability(10L);

        assertTrue(response.isAvailable());
        assertEquals(40, response.salableQuantity());
    }
}
