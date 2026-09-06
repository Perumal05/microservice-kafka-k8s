package com.shopsphere.inventory.mapper;

import com.shopsphere.inventory.dto.request.CreateInventoryRequest;
import com.shopsphere.inventory.dto.request.UpdateInventoryRequest;
import com.shopsphere.inventory.dto.response.AvailabilityResponse;
import com.shopsphere.inventory.dto.response.InventoryResponse;
import com.shopsphere.inventory.model.entity.Inventory;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

    public Inventory toEntity(CreateInventoryRequest request) {
        if (request == null) {
            return null;
        }
        return Inventory.builder()
                .productId(request.productId())
                .sku(request.sku())
                .availableQuantity(request.availableQuantity())
                .reservedQuantity(0)
                .reorderLevel(request.reorderLevel() != null ? request.reorderLevel() : 0)
                .build();
    }

    public InventoryResponse toResponse(Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        return new InventoryResponse(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getSku(),
                inventory.getAvailableQuantity(),
                inventory.getReservedQuantity(),
                inventory.getReorderLevel(),
                inventory.getSalableQuantity(),
                inventory.getCreatedAt(),
                inventory.getUpdatedAt()
        );
    }

    public AvailabilityResponse toAvailabilityResponse(Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        int salable = inventory.getSalableQuantity();
        return new AvailabilityResponse(
                inventory.getProductId(),
                inventory.getSku(),
                inventory.getAvailableQuantity(),
                inventory.getReservedQuantity(),
                salable,
                salable > 0
        );
    }

    public void updateEntityFromRequest(UpdateInventoryRequest request, Inventory inventory) {
        if (request == null || inventory == null) {
            return;
        }
        inventory.setAvailableQuantity(request.availableQuantity());
        if (request.reorderLevel() != null) {
            inventory.setReorderLevel(request.reorderLevel());
        }
    }
}
