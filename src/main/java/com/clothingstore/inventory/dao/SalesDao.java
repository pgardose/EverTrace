package com.clothingstore.inventory.dao;

import com.clothingstore.inventory.model.DailySales;
import com.clothingstore.inventory.model.MonthlySales;
import com.clothingstore.inventory.model.SalesTotals;
import com.clothingstore.inventory.model.SoldLine;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        }
        return result;
    }

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
        }
        return result;
    }

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
    }
}
