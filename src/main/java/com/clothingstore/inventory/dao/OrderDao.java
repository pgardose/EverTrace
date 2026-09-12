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
}
