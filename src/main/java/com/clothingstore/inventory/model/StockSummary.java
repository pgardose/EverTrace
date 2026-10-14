package com.clothingstore.inventory.model;

/**
 * Immutable snapshot of inventory health for the dashboard.
 * A record fits here because this is pure read-only data -- it's built
 * fresh from a stream calculation every time and never mutated.
 *
 * @param productCount        number of distinct products (rows) in inventory
 * @param totalItems          total units in stock across all products
 */
public record StockSummary(
        int productCount,
        int totalItems,
        int lowStockCount,
        int outOfStockCount,
        double totalInventoryValue
) {}
