package com.shopsphere.inventory.service;

import com.shopsphere.inventory.dto.request.CreateInventoryRequest;
import com.shopsphere.inventory.dto.request.UpdateInventoryRequest;
import com.shopsphere.inventory.dto.response.AvailabilityResponse;
import com.shopsphere.inventory.dto.response.InventoryResponse;

public interface InventoryService {
    InventoryResponse createInventory(CreateInventoryRequest request);
    InventoryResponse getInventoryByProductId(Long productId);
    InventoryResponse updateInventory(Long productId, UpdateInventoryRequest request);
    AvailabilityResponse checkAvailability(Long productId);
}
