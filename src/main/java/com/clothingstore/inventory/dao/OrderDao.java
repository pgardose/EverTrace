package com.clothingstore.inventory.dao;

import com.clothingstore.inventory.model.Order;
import com.clothingstore.inventory.model.OrderItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDao {

    /** Inserts the order header + all its lines using the SAME connection/transaction as the caller. */
    public Order save(Connection conn, Order order) throws SQLException {
        String orderSql = "INSERT INTO orders (customer_name, order_date, total_amount, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, order.getCustomerName());
            ps.setString(2, order.getOrderDate().toString());
            ps.setDouble(3, order.getTotalAmount());
            ps.setString(4, order.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) order.setOrderId(keys.getInt(1));
            }
        }

        String lineSql = "INSERT INTO order_items (order_id, item_id, quantity, unit_price) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(lineSql)) {
            for (OrderItem line : order.getLines()) {
                ps.setInt(1, order.getOrderId());
                ps.setInt(2, line.getItemId());
                ps.setInt(3, line.getQuantity());
                ps.setDouble(4, line.getUnitPrice());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return order;
    }

    public List<Order> findAll() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders ORDER BY order_date DESC";
        try (Statement stmt = DatabaseManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Order order = new Order(rs.getString("customer_name"));
                order.setOrderId(rs.getInt("order_id"));
                order.setTotalAmount(rs.getDouble("total_amount"));
                order.setStatus(Order.OrderStatus.valueOf(rs.getString("status")));
                orders.add(order);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load orders", e);
        }
        return orders;
    }

    /** Loads the line items for one order -- used by the Orders screen's detail view and by cancellation. */
    public List<OrderItem> findLinesForOrder(int orderId) {
        List<OrderItem> lines = new ArrayList<>();
        String sql = """
            SELECT oi.*, i.name AS item_name FROM order_items oi
            LEFT JOIN items i ON oi.item_id = i.item_id
            WHERE oi.order_id = ?
            """;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderItem line = new OrderItem(
                            rs.getInt("item_id"),
                            rs.getString("item_name"),
                            rs.getInt("quantity"),
                            rs.getDouble("unit_price"));
                    line.setOrderItemId(rs.getInt("order_item_id"));
                    line.setOrderId(orderId);
                    lines.add(line);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load order lines for order " + orderId, e);
        }
        return lines;
    }

    /** Updates order status within the caller's transaction (used when cancelling an order). */
    public void updateStatus(Connection conn, int orderId, Order.OrderStatus status) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }
}
