package com.clothingstore.inventory.model;

import java.time.YearMonth;

/** Sales for one calendar month -- one row of the "records of sale per month" table. */
public record MonthlySales(YearMonth month, int orders, int itemsSold, double revenue) {}
