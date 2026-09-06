package com.shopsphere.cart.exception;

public class MissingHeaderException extends RuntimeException {
    public MissingHeaderException(String headerName) {
        super("Required header '" + headerName + "' is missing");
    }
}
