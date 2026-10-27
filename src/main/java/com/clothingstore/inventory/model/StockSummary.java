package com.clothingstore.inventory.model;

/**
 * Immutable snapshot of inventory health for the dashboard.
 * A record fits here because this is pure read-only data -- it's built
 * fresh from a stream calculation every time and never mutated.
<<<<<<< HEAD
 */
public record StockSummary(
=======
 *
 * @param productCount        number of distinct products (rows) in inventory
 * @param totalItems          total units in stock across all products
 */
public record StockSummary(
        int productCount,
>>>>>>> 1fad3874ce4da0e34b3cc67029bd87899e5d4823
        int totalItems,
        int lowStockCount,
        int outOfStockCount,
        double totalInventoryValue
) {}
