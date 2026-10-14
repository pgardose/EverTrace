package com.clothingstore.inventory.model;

/**
 * Contract for anything that can be checked out. Keeping this as an
 * interface (rather than hardcoding checkout logic against Item) means
 * the checkout flow could support other sellable types later without
 * changing OrderService.
 */
public interface Sellable {
    boolean isPurchasable(int requestedQty);
    void deductStock(int qty);
    void restock(int qty);
}
