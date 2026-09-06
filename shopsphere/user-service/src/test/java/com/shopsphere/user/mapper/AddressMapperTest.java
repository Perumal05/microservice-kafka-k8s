package com.shopsphere.user.mapper;

import com.shopsphere.user.dto.request.CreateAddressRequest;
import com.shopsphere.user.dto.request.UpdateAddressRequest;
import com.shopsphere.user.dto.response.AddressResponse;
import com.shopsphere.user.model.entity.Address;
import com.shopsphere.user.model.entity.AddressType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AddressMapperTest {

    private AddressMapper addressMapper;

    @BeforeEach
    void setUp() {
        addressMapper = new AddressMapper();
    }

    @Test
    void toEntity_ShouldMapCreateAddressRequestToAddress() {
        CreateAddressRequest request = new CreateAddressRequest(
                "123 Main St",
                "Apt 4B",
                "Metropolis",
                "NY",
                "10001",
                "USA",
                AddressType.WORK,
                true
        );

        Address address = addressMapper.toEntity(request, 10L);

        assertNotNull(address);
        assertEquals(10L, address.getUserId());
        assertEquals("123 Main St", address.getAddressLine1());
        assertEquals("Apt 4B", address.getAddressLine2());
        assertEquals("Metropolis", address.getCity());
        assertEquals("NY", address.getState());
        assertEquals("10001", address.getPostalCode());
        assertEquals("USA", address.getCountry());
        assertEquals(AddressType.WORK, address.getType());
        assertTrue(address.getIsDefault());
    }

    @Test
    void toResponse_ShouldMapAddressToAddressResponse() {
        Instant now = Instant.now();
        Address address = Address.builder()
                .id(5L)
                .userId(10L)
                .addressLine1("123 Main St")
                .addressLine2("Apt 4B")
                .city("Metropolis")
                .state("NY")
                .postalCode("10001")
                .country("USA")
                .type(AddressType.HOME)
                .isDefault(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        AddressResponse response = addressMapper.toResponse(address);

        assertNotNull(response);
        assertEquals(5L, response.id());
        assertEquals(10L, response.userId());
        assertEquals("123 Main St", response.addressLine1());
        assertEquals("Metropolis", response.city());
        assertFalse(response.isDefault());
    }

    @Test
    void updateEntityFromRequest_ShouldUpdateFields() {
        Address address = Address.builder()
                .addressLine1("Old Line 1")
                .city("Old City")
                .type(AddressType.HOME)
                .isDefault(false)
                .build();

        UpdateAddressRequest request = new UpdateAddressRequest(
                "New Line 1",
                "New Line 2",
                "New City",
                "New State",
                "20002",
                "USA",
                AddressType.WORK,
                true
        );

        addressMapper.updateEntityFromRequest(request, address);

        assertEquals("New Line 1", address.getAddressLine1());
        assertEquals("New City", address.getCity());
        assertEquals(AddressType.WORK, address.getType());
        assertTrue(address.getIsDefault());
    }
}
