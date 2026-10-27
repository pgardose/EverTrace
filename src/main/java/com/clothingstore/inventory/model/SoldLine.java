package com.clothingstore.inventory.model;

import java.time.LocalDateTime;

/** One item line of a completed sale, with the exact time it was sold. */
public record SoldLine(int orderId, LocalDateTime soldAt, String brand, String code,
                       int quantity, double unitPrice) {
    public double lineTotal() {
        return quantity * unitPrice;
    }
}
