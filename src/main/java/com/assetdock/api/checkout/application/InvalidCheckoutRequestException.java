package com.assetdock.api.checkout.application;

public class InvalidCheckoutRequestException extends RuntimeException {
    public InvalidCheckoutRequestException(String message) {
        super(message);
    }
}
