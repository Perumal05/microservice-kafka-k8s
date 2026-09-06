package com.shopsphere.user.service;

import com.shopsphere.user.dto.request.CreateAddressRequest;
import com.shopsphere.user.dto.request.UpdateAddressRequest;
import com.shopsphere.user.dto.response.AddressResponse;
import com.shopsphere.user.exception.BadRequestException;
import com.shopsphere.user.exception.ResourceNotFoundException;
import com.shopsphere.user.mapper.AddressMapper;
import com.shopsphere.user.model.entity.Address;
import com.shopsphere.user.model.entity.AddressType;
import com.shopsphere.user.repository.AddressRepository;
import com.shopsphere.user.repository.UserRepository;
import com.shopsphere.user.service.impl.AddressServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AddressServiceImplTest {

    private AddressRepository addressRepository;
    private UserRepository userRepository;
    private AddressMapper addressMapper;
    private AddressServiceImpl addressService;

    @BeforeEach
    void setUp() {
        addressRepository = mock(AddressRepository.class);
        userRepository = mock(UserRepository.class);
        addressMapper = new AddressMapper();
        addressService = new AddressServiceImpl(addressRepository, userRepository, addressMapper);
    }

    @Test
    void getAddressesByUserId_Success() {
        Long userId = 1L;
        Address address = Address.builder()
                .id(10L)
                .userId(userId)
                .addressLine1("123 Main St")
                .city("City")
                .state("State")
                .postalCode("10001")
                .country("USA")
                .type(AddressType.HOME)
                .isDefault(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(addressRepository.findByUserId(userId)).thenReturn(List.of(address));

        List<AddressResponse> results = addressService.getAddressesByUserId(userId);

        assertEquals(1, results.size());
        assertEquals("123 Main St", results.get(0).addressLine1());
    }

    @Test
    void createAddress_WithDefault_ShouldResetPreviousDefaults() {
        Long userId = 1L;
        CreateAddressRequest request = new CreateAddressRequest(
                "123 Main St", null, "City", "State", "10001", "USA", AddressType.HOME, true);

        Address savedAddress = Address.builder()
                .id(10L)
                .userId(userId)
                .addressLine1("123 Main St")
                .city("City")
                .state("State")
                .postalCode("10001")
                .country("USA")
                .type(AddressType.HOME)
                .isDefault(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(addressRepository.save(any(Address.class))).thenReturn(savedAddress);

        AddressResponse result = addressService.createAddress(userId, request);

        assertNotNull(result);
        assertEquals("123 Main St", result.addressLine1());
        verify(addressRepository).resetDefaultAddressesForUser(userId);
        verify(addressRepository).save(any(Address.class));
    }

    @Test
    void updateAddress_CrossUserAccess_ShouldThrowBadRequestException() {
        Long userId = 1L;
        Long otherUserIdAddressId = 99L;
        UpdateAddressRequest request = new UpdateAddressRequest(
                "123 Main St", null, "City", "State", "10001", "USA", AddressType.HOME, false);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(addressRepository.findByIdAndUserId(otherUserIdAddressId, userId)).thenReturn(Optional.empty());
        when(addressRepository.existsById(otherUserIdAddressId)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> addressService.updateAddress(userId, otherUserIdAddressId, request));
    }

    @Test
    void deleteAddress_Success() {
        Long userId = 1L;
        Long addressId = 10L;
        Address address = Address.builder().id(addressId).userId(userId).build();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.of(address));

        addressService.deleteAddress(userId, addressId);

        verify(addressRepository).delete(address);
    }
}
