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

    /**
     * Derives the correct status from a quantity and reorder threshold.
     * Called every time stock changes so status is always in sync with quantity.
     */
    public static ItemStatus fromQuantity(int quantity, int reorderThreshold) {
        if (quantity <= 0) {
            return OUT_OF_STOCK;
        }
        if (quantity <= reorderThreshold) {
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
