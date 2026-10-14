package com.clothingstore.inventory.service;

/** Thrown when a checkout is attempted for more units than are currently in stock. */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String itemName, int requested, int available) {
        super("Not enough stock for \"%s\": requested %d, only %d available"
                .formatted(itemName, requested, available));
    }
}
