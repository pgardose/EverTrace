package com.clothingstore.inventory.dao;

/**
 * Wraps checked SQLException so callers (services, controllers) don't need
 * to litter their code with try/catch SQLException everywhere -- they
 * only need to handle this when they actually want to react to a failure.
 */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
