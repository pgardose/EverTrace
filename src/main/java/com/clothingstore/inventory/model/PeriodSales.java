package com.clothingstore.inventory.model;

/** Sales total and number of completed orders for some period (an hour, a day, a week, a month). */
public record PeriodSales(double total, int orders) {

    public static final PeriodSales ZERO = new PeriodSales(0, 0);

    public double average() {
        return orders == 0 ? 0 : total / orders;
    }
}
