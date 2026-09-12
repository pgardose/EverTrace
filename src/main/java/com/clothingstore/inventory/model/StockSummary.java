package com.clothingstore.inventory.model;

/**
 * Immutable snapshot of inventory health for the dashboard.
 * A record fits here because this is pure read-only data -- it's built
 * fresh from a stream calculation every time and never mutated.
 */
public record StockSummary(
        int totalItems,
        int lowStockCount,
        int outOfStockCount,
        double totalInventoryValue
) {}
