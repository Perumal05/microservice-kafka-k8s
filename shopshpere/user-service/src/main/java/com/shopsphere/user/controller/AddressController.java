package com.shopsphere.user.controller;

import com.shopsphere.user.dto.request.CreateAddressRequest;
import com.shopsphere.user.dto.request.UpdateAddressRequest;
import com.shopsphere.user.dto.response.AddressResponse;
import com.shopsphere.user.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAddressesByUserId(@PathVariable Long userId) {
        List<AddressResponse> responses = addressService.getAddressesByUserId(userId);
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<AddressResponse> createAddress(
            @PathVariable Long userId,
            @Valid @RequestBody CreateAddressRequest request) {
        AddressResponse response = addressService.createAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId,
            @Valid @RequestBody UpdateAddressRequest request) {
        AddressResponse response = addressService.updateAddress(userId, addressId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId) {
        addressService.deleteAddress(userId, addressId);
        return ResponseEntity.noContent().build();
    }
}
