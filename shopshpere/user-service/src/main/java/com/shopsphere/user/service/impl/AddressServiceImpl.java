package com.shopsphere.user.service.impl;

import com.shopsphere.user.dto.request.CreateAddressRequest;
import com.shopsphere.user.dto.request.UpdateAddressRequest;
import com.shopsphere.user.dto.response.AddressResponse;
import com.shopsphere.user.exception.BadRequestException;
import com.shopsphere.user.exception.ResourceNotFoundException;
import com.shopsphere.user.mapper.AddressMapper;
import com.shopsphere.user.model.entity.Address;
import com.shopsphere.user.repository.AddressRepository;
import com.shopsphere.user.repository.UserRepository;
import com.shopsphere.user.service.AddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;

    public AddressServiceImpl(AddressRepository addressRepository,
                               UserRepository userRepository,
                               AddressMapper addressMapper) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.addressMapper = addressMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddressesByUserId(Long userId) {
        ensureUserExists(userId);
        return addressRepository.findByUserId(userId).stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    @Override
    public AddressResponse createAddress(Long userId, CreateAddressRequest request) {
        ensureUserExists(userId);

        if (Boolean.TRUE.equals(request.isDefault())) {
            addressRepository.resetDefaultAddressesForUser(userId);
        }

        Address address = addressMapper.toEntity(request, userId);
        Address savedAddress = addressRepository.save(address);

        return addressMapper.toResponse(savedAddress);
    }

    @Override
    public AddressResponse updateAddress(Long userId, Long addressId, UpdateAddressRequest request) {
        ensureUserExists(userId);

        Address address = getAddressBelongingToUser(addressId, userId);

        if (Boolean.TRUE.equals(request.isDefault())) {
            addressRepository.resetDefaultAddressesForUser(userId);
        }

        addressMapper.updateEntityFromRequest(request, address);
        Address updatedAddress = addressRepository.save(address);

        return addressMapper.toResponse(updatedAddress);
    }

    @Override
    public void deleteAddress(Long userId, Long addressId) {
        ensureUserExists(userId);

        Address address = getAddressBelongingToUser(addressId, userId);
        addressRepository.delete(address);
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
    }

    private Address getAddressBelongingToUser(Long addressId, Long userId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseGet(() -> {
                    if (addressRepository.existsById(addressId)) {
                        throw new BadRequestException("Address with id: " + addressId + " does not belong to user id: " + userId);
                    }
                    throw new ResourceNotFoundException("Address not found with id: " + addressId);
                });
    }
}
