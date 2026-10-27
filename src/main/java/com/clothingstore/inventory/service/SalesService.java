package com.clothingstore.inventory.service;

import com.clothingstore.inventory.dao.SalesDao;
import com.clothingstore.inventory.model.Order;
import com.clothingstore.inventory.model.PeriodSales;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

/** Sales reporting for the Sales calendar and the Dashboard cards. Weeks run Sunday to Saturday. */
public class SalesService {

    private final SalesDao salesDao;

    public SalesService(SalesDao salesDao) {
        this.salesDao = salesDao;
    }

    public static LocalDate weekStart(LocalDate anyDayInWeek) {
        return anyDayInWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    }

    public static LocalDate monthStart(LocalDate anyDayInMonth) {
        return anyDayInMonth.withDayOfMonth(1);
    }

    public PeriodSales forDay(LocalDate day) {
        return salesDao.total(day, day.plusDays(1));
    }

    public PeriodSales forWeekOf(LocalDate anyDayInWeek) {
        LocalDate start = weekStart(anyDayInWeek);
        return salesDao.total(start, start.plusDays(7));
    }

    public PeriodSales forMonthOf(LocalDate anyDayInMonth) {
        LocalDate start = monthStart(anyDayInMonth);
        return salesDao.total(start, start.plusMonths(1));
    }

    public Map<LocalDate, PeriodSales> daily(LocalDate fromInclusive, LocalDate toExclusive) {
        return salesDao.dailyTotals(fromInclusive, toExclusive);
    }

    public Map<Integer, PeriodSales> hourly(LocalDate day) {
        return salesDao.hourlyTotals(day);
    }

    public List<Order> ordersOn(LocalDate day) {
        return salesDao.ordersOn(day);
    }
}
