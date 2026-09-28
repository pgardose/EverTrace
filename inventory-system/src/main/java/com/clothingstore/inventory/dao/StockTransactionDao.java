package com.clothingstore.inventory.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Writes an audit row every time stock changes. This table is what lets
 * you answer "why is this item's quantity what it is" after the fact --
 * every sale, restock, and manual adjustment is logged with a reason.
 */
public class StockTransactionDao {

    /** Takes the caller's Connection so this write joins the caller's transaction (see OrderService). */
    public void logChange(Connection conn, int itemId, int changeAmount, String reason) throws SQLException {
        String sql = "INSERT INTO stock_transactions (item_id, change_amount, reason, timestamp) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.setInt(2, changeAmount);
            ps.setString(3, reason);
            ps.setString(4, LocalDateTime.now().toString());
            ps.executeUpdate();
        }
    }
}
