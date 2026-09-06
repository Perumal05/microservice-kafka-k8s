package com.shopsphere.inventory.service.impl;

import com.shopsphere.inventory.dto.request.CreateInventoryRequest;
import com.shopsphere.inventory.dto.request.UpdateInventoryRequest;
import com.shopsphere.inventory.dto.response.AvailabilityResponse;
import com.shopsphere.inventory.dto.response.InventoryResponse;
import com.shopsphere.inventory.exception.DuplicateResourceException;
import com.shopsphere.inventory.exception.ResourceNotFoundException;
import com.shopsphere.inventory.mapper.InventoryMapper;
import com.shopsphere.inventory.model.entity.Inventory;
import com.shopsphere.inventory.repository.InventoryRepository;
import com.shopsphere.inventory.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    public InventoryServiceImpl(InventoryRepository inventoryRepository, InventoryMapper inventoryMapper) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryMapper = inventoryMapper;
    }

    @Override
    public InventoryResponse createInventory(CreateInventoryRequest request) {
        if (inventoryRepository.existsByProductId(request.productId())) {
            throw new DuplicateResourceException("Inventory already exists for product ID: " + request.productId());
        }

        Inventory inventory = inventoryMapper.toEntity(request);
        Inventory savedInventory = inventoryRepository.save(inventory);

        return inventoryMapper.toResponse(savedInventory);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductId(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product ID: " + productId));
        return inventoryMapper.toResponse(inventory);
    }

    @Override
    public InventoryResponse updateInventory(Long productId, UpdateInventoryRequest request) {
        Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product ID: " + productId));

        inventoryMapper.updateEntityFromRequest(request, inventory);
        Inventory updatedInventory = inventoryRepository.save(inventory);

        return inventoryMapper.toResponse(updatedInventory);
    }

    @Override
    @Transactional(readOnly = true)
    public AvailabilityResponse checkAvailability(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product ID: " + productId));
        return inventoryMapper.toAvailabilityResponse(inventory);
    }
}
