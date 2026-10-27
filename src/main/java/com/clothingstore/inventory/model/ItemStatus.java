package com.clothingstore.inventory.model;

/**
 * Represents the availability state of an inventory item.
 * Kept as an enum (not a raw String) so invalid statuses are impossible
 * and status logic lives in exactly one place.
 */
public enum ItemStatus {
    AVAILABLE,
    LOW_STOCK,
    OUT_OF_STOCK,
    DISCONTINUED;

<<<<<<< HEAD
    /**
     * Derives the correct status from a quantity and reorder threshold.
     * Called every time stock changes so status is always in sync with quantity.
     */
    public static ItemStatus fromQuantity(int quantity, int reorderThreshold) {
        if (quantity <= 0) {
            return OUT_OF_STOCK;
        }
        if (quantity <= reorderThreshold) {
=======
    /** Quantity at or below which an item is flagged Low Stock (the per-item reorder threshold was removed). */
    public static final int LOW_STOCK_LEVEL = 5;

    /**
     * Derives the correct status from a quantity.
     * Called every time stock changes so status is always in sync with quantity.
     */
    public static ItemStatus fromQuantity(int quantity) {
        if (quantity <= 0) {
            return OUT_OF_STOCK;
        }
        if (quantity <= LOW_STOCK_LEVEL) {
>>>>>>> 1fad3874ce4da0e34b3cc67029bd87899e5d4823
            return LOW_STOCK;
        }
        return AVAILABLE;
    }

    public String displayName() {
        return switch (this) {
            case AVAILABLE -> "Available";
            case LOW_STOCK -> "Low Stock";
            case OUT_OF_STOCK -> "Out of Stock";
            case DISCONTINUED -> "Discontinued";
        };
    }
}
