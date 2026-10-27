package com.clothingstore.inventory.util;

/** One place for money formatting so every screen shows amounts the same way. */
public final class Money {

    private Money() {}

    /** e.g. 1234.5 -> "₱1,234.50" */
    public static String peso(double amount) {
        return "₱%,.2f".formatted(amount);
    }
}
