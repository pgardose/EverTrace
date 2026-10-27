package com.clothingstore.inventory.model;

/** Totals over completed (not cancelled) orders: how many orders, units sold and revenue earned. */
public record SalesTotals(int orders, int itemsSold, double revenue) {
    public static final SalesTotals EMPTY = new SalesTotals(0, 0, 0);
}
