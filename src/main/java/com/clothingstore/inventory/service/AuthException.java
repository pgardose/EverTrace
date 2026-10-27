package com.clothingstore.inventory.service;

/** A login/registration problem whose message is safe to show directly to the user. */
public class AuthException extends RuntimeException {
    public AuthException(String message) {
        super(message);
    }
}
