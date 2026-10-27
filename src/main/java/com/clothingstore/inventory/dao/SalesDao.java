package com.clothingstore.inventory.dao;

<<<<<<< HEAD
import com.clothingstore.inventory.model.Order;
import com.clothingstore.inventory.model.PeriodSales;
=======
import com.clothingstore.inventory.model.DailySales;
import com.clothingstore.inventory.model.MonthlySales;
import com.clothingstore.inventory.model.SalesTotals;
import com.clothingstore.inventory.model.SoldLine;
>>>>>>> 1fad3874ce4da0e34b3cc67029bd87899e5d4823

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
<<<<<<< HEAD
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
=======
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only sales reporting. Only COMPLETED orders count -- a cancelled
 * order was refunded and restocked, so it must not show up as revenue.
 * order_date is stored as an ISO string (e.g. 2026-10-13T14:05:09), so
 * substr(order_date, 1, 10) is the date and substr(order_date, 1, 7) the month.
 */
public class SalesDao {

    private static final String LINE_AGGREGATES = """
            COUNT(DISTINCT o.order_id) AS orders,
            COALESCE(SUM(oi.quantity), 0) AS items_sold,
            COALESCE(SUM(oi.quantity * oi.unit_price), 0) AS revenue
            """;

    /** Total revenue earned and total items sold, across all time. */
    public SalesTotals totals() {
        String sql = "SELECT " + LINE_AGGREGATES + """
                FROM orders o JOIN order_items oi ON oi.order_id = o.order_id
                WHERE o.status = 'COMPLETED'
                """;
        try (Statement stmt = DatabaseManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next()
                    ? new SalesTotals(rs.getInt("orders"), rs.getInt("items_sold"), rs.getDouble("revenue"))
                    : SalesTotals.EMPTY;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load sales totals", e);
        }
    }

    /** One row per day that had at least one sale in the given month (days with no sales are absent). */
    public List<DailySales> dailySales(YearMonth month) {
        String sql = "SELECT substr(o.order_date, 1, 10) AS day, " + LINE_AGGREGATES + """
                FROM orders o JOIN order_items oi ON oi.order_id = o.order_id
                WHERE o.status = 'COMPLETED' AND substr(o.order_date, 1, 7) = ?
                GROUP BY day ORDER BY day
                """;
        List<DailySales> result = new ArrayList<>();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, month.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new DailySales(LocalDate.parse(rs.getString("day")),
                            rs.getInt("orders"), rs.getInt("items_sold"), rs.getDouble("revenue")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load daily sales for " + month, e);
>>>>>>> 1fad3874ce4da0e34b3cc67029bd87899e5d4823
        }
        return result;
    }

<<<<<<< HEAD
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
=======
    /** Records of sale per month, newest month first. */
    public List<MonthlySales> monthlySales() {
        String sql = "SELECT substr(o.order_date, 1, 7) AS month, " + LINE_AGGREGATES + """
                FROM orders o JOIN order_items oi ON oi.order_id = o.order_id
                WHERE o.status = 'COMPLETED'
                GROUP BY month ORDER BY month DESC
                """;
        List<MonthlySales> result = new ArrayList<>();
        try (Statement stmt = DatabaseManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(new MonthlySales(YearMonth.parse(rs.getString("month")),
                        rs.getInt("orders"), rs.getInt("items_sold"), rs.getDouble("revenue")));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load monthly sales", e);
>>>>>>> 1fad3874ce4da0e34b3cc67029bd87899e5d4823
        }
        return result;
    }

<<<<<<< HEAD
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
=======
    /** Every item line sold on one date -- what the calendar shows when a day is clicked. */
    public List<SoldLine> linesSoldOn(LocalDate date) {
        String sql = """
                SELECT o.order_id, o.order_date, i.name AS brand, i.sku AS code, oi.quantity, oi.unit_price
                FROM orders o
                JOIN order_items oi ON oi.order_id = o.order_id
                LEFT JOIN items i ON i.item_id = oi.item_id
                WHERE o.status = 'COMPLETED' AND substr(o.order_date, 1, 10) = ?
                ORDER BY o.order_date, o.order_id, oi.order_item_id
                """;
        List<SoldLine> result = new ArrayList<>();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new SoldLine(rs.getInt("order_id"),
                            LocalDateTime.parse(rs.getString("order_date")),
                            rs.getString("brand"), rs.getString("code"),
                            rs.getInt("quantity"), rs.getDouble("unit_price")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load sales for " + date, e);
        }
        return result;
>>>>>>> 1fad3874ce4da0e34b3cc67029bd87899e5d4823
    }
}
