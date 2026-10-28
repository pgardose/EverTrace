package com.clothingstore.inventory.dao;

import com.clothingstore.inventory.model.Order;
import com.clothingstore.inventory.model.PeriodSales;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Read-only sales queries. Only COMPLETED orders count as sales, so a
 * cancelled order drops out of every total automatically.
 * <p>
 * orders.order_date is stored as an ISO local date-time ("2026-10-27T19:58:12"),
 * so the first 10 characters are the date and characters 12-13 are the hour,
 * and plain string comparison sorts dates correctly.
 */
public class SalesDao {

    public PeriodSales total(LocalDate fromInclusive, LocalDate toExclusive) {
        String sql = """
            SELECT COALESCE(SUM(total_amount), 0), COUNT(*) FROM orders
            WHERE status = 'COMPLETED'
              AND substr(order_date, 1, 10) >= ? AND substr(order_date, 1, 10) < ?
            """;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, fromInclusive.toString());
            ps.setString(2, toExclusive.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new PeriodSales(rs.getDouble(1), rs.getInt(2)) : PeriodSales.ZERO;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to total sales", e);
        }
    }

    /** One entry per day that had at least one completed order; days with no sales are absent. */
    public Map<LocalDate, PeriodSales> dailyTotals(LocalDate fromInclusive, LocalDate toExclusive) {
        String sql = """
            SELECT substr(order_date, 1, 10) AS day, SUM(total_amount), COUNT(*) FROM orders
            WHERE status = 'COMPLETED'
              AND substr(order_date, 1, 10) >= ? AND substr(order_date, 1, 10) < ?
            GROUP BY day
            """;
        Map<LocalDate, PeriodSales> result = new TreeMap<>();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, fromInclusive.toString());
            ps.setString(2, toExclusive.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(LocalDate.parse(rs.getString(1)), new PeriodSales(rs.getDouble(2), rs.getInt(3)));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load daily sales", e);
        }
        return result;
    }

    /** Sales per hour of the given day (key = hour 0-23); hours with no sales are absent. */
    public Map<Integer, PeriodSales> hourlyTotals(LocalDate day) {
        String sql = """
            SELECT CAST(substr(order_date, 12, 2) AS INTEGER) AS hour, SUM(total_amount), COUNT(*) FROM orders
            WHERE status = 'COMPLETED' AND substr(order_date, 1, 10) = ?
            GROUP BY hour
            """;
        Map<Integer, PeriodSales> result = new TreeMap<>();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, day.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getInt(1), new PeriodSales(rs.getDouble(2), rs.getInt(3)));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load hourly sales", e);
        }
        return result;
    }

    /** Every order placed on the day (all statuses, so cancelled ones are visible too), oldest first. */
    public List<Order> ordersOn(LocalDate day) {
        String sql = "SELECT * FROM orders WHERE substr(order_date, 1, 10) = ? ORDER BY order_date";
        List<Order> orders = new ArrayList<>();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, day.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Order order = new Order(rs.getString("customer_name"));
                    order.setOrderId(rs.getInt("order_id"));
                    order.setOrderDate(LocalDateTime.parse(rs.getString("order_date")));
                    order.setTotalAmount(rs.getDouble("total_amount"));
                    order.setStatus(Order.OrderStatus.valueOf(rs.getString("status")));
                    orders.add(order);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load orders for " + day, e);
        }
        return orders;
    }
}
