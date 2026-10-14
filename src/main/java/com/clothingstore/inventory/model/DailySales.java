package com.clothingstore.inventory.model;

import java.time.LocalDate;

/** Sales for one calendar date -- drives the calendar cells. */
public record DailySales(LocalDate date, int orders, int itemsSold, double revenue) {}
