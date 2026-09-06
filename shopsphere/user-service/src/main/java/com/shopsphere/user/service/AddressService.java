package com.shopsphere.user.service;

import com.shopsphere.user.dto.request.CreateAddressRequest;
import com.shopsphere.user.dto.request.UpdateAddressRequest;
import com.shopsphere.user.dto.response.AddressResponse;

import java.util.List;

public interface AddressService {
    List<AddressResponse> getAddressesByUserId(Long userId);
    AddressResponse createAddress(Long userId, CreateAddressRequest request);
    AddressResponse updateAddress(Long userId, Long addressId, UpdateAddressRequest request);
    void deleteAddress(Long userId, Long addressId);
}
