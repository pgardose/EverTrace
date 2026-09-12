package com.clothingstore.inventory.model;

/**
 * One line of a cart before checkout is confirmed.
 * Immutable by nature -- a requested quantity for a given item never
 * needs to mutate itself, only be replaced.
 */
public record OrderLineRequest(int itemId, String itemName, int quantity, double unitPrice) {

    public double lineTotal() {
        return quantity * unitPrice;
    }
}
