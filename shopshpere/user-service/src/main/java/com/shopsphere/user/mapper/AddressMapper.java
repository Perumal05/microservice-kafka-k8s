package com.shopsphere.user.mapper;

import com.shopsphere.user.dto.request.CreateAddressRequest;
import com.shopsphere.user.dto.request.UpdateAddressRequest;
import com.shopsphere.user.dto.response.AddressResponse;
import com.shopsphere.user.model.entity.Address;
import com.shopsphere.user.model.entity.AddressType;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    public Address toEntity(CreateAddressRequest request, Long userId) {
        if (request == null) {
            return null;
        }
        return Address.builder()
                .userId(userId)
                .addressLine1(request.addressLine1())
                .addressLine2(request.addressLine2())
                .city(request.city())
                .state(request.state())
                .postalCode(request.postalCode())
                .country(request.country())
                .type(request.type() != null ? request.type() : AddressType.HOME)
                .isDefault(Boolean.TRUE.equals(request.isDefault()))
                .build();
    }

    public AddressResponse toResponse(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressResponse(
                address.getId(),
                address.getUserId(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry(),
                address.getType(),
                address.getIsDefault(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }

    public void updateEntityFromRequest(UpdateAddressRequest request, Address address) {
        if (request == null || address == null) {
            return;
        }
        address.setAddressLine1(request.addressLine1());
        address.setAddressLine2(request.addressLine2());
        address.setCity(request.city());
        address.setState(request.state());
        address.setPostalCode(request.postalCode());
        address.setCountry(request.country());
        if (request.type() != null) {
            address.setType(request.type());
        }
        if (request.isDefault() != null) {
            address.setIsDefault(request.isDefault());
        }
    }
}
