package com.clothingstore.inventory.dao;

import com.clothingstore.inventory.model.Item;
import com.clothingstore.inventory.model.ItemStatus;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemDao implements GenericDao<Item, Integer> {

    @Override
    public Optional<Item> findById(Integer id) {
        String sql = """
            SELECT i.*, c.name AS category_name FROM items i
            LEFT JOIN categories c ON i.category_id = c.category_id
            WHERE i.item_id = ?
            """;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find item " + id, e);
        }
    }

    @Override
    public List<Item> findAll() {
        String sql = """
            SELECT i.*, c.name AS category_name FROM items i
            LEFT JOIN categories c ON i.category_id = c.category_id
            WHERE i.archived = 0
            ORDER BY i.name
            """;
        List<Item> items = new ArrayList<>();
        try (Statement stmt = DatabaseManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                items.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load items", e);
        }
        return items;
    }

    /** Items at or below their reorder threshold -- feeds the dashboard's low-stock panel. */
    public List<Item> findLowStock() {
        return findAll().stream()
                .filter(i -> i.getStatus() == ItemStatus.LOW_STOCK || i.getStatus() == ItemStatus.OUT_OF_STOCK)
                .toList();
    }

    @Override
    public Item save(Item item) {
        return item.getItemId() == 0 ? insert(item) : update(item);
    }

    private Item insert(Item item) {
        String sql = """
            INSERT INTO items (sku, name, category_id, size, color, price, quantity,
                                reorder_threshold, status, date_added, image_path)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = DatabaseManager.getConnection()
                .prepareStatement(sql)) {
            bindItem(ps, item);
            ps.executeUpdate();
            item.setItemId(DatabaseManager.lastInsertId());
            return item;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert item " + item.getName(), e);
        }
    }

    private Item update(Item item) {
        String sql = """
            UPDATE items SET sku=?, name=?, category_id=?, size=?, color=?, price=?,
                              quantity=?, reorder_threshold=?, status=?, date_added=?, image_path=?
            WHERE item_id=?
            """;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            bindItem(ps, item);
            ps.setInt(12, item.getItemId());
            ps.executeUpdate();
            return item;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update item " + item.getName(), e);
        }
    }

    /**
     * Persists a stock quantity change and logs the reason, in a single
     * statement group. Called by OrderService inside its own transaction
     * so the quantity update and the audit row never drift apart.
     */
    public void updateQuantityAndStatus(Connection conn, Item item) throws SQLException {
        String sql = "UPDATE items SET quantity = ?, status = ? WHERE item_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, item.getQuantity());
            ps.setString(2, item.getStatus().name());
            ps.setInt(3, item.getItemId());
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteById(Integer id) {
        String sql = "DELETE FROM items WHERE item_id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete item " + id, e);
        }
    }

    /**
     * True if any order line or stock-history row points at this item. Such an
     * item can't be hard-deleted (the foreign keys protect the sales records),
     * so the service archives it instead.
     */
    public boolean hasHistory(int itemId) {
        String sql = """
            SELECT (SELECT COUNT(*) FROM order_items WHERE item_id = ?)
                 + (SELECT COUNT(*) FROM stock_transactions WHERE item_id = ?)
            """;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.setInt(2, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to check history for item " + itemId, e);
        }
    }

    /** Hides an item from inventory/checkout/dashboard without touching its sales history. */
    public void archive(int itemId) {
        String sql = "UPDATE items SET archived = 1 WHERE item_id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to archive item " + itemId, e);
        }
    }

    private void bindItem(PreparedStatement ps, Item item) throws SQLException {
        ps.setString(1, item.getSku());
        ps.setString(2, item.getName());
        ps.setInt(3, item.getCategoryId());
        ps.setString(4, item.getSize());
        ps.setString(5, item.getColor());
        ps.setDouble(6, item.getPrice());
        ps.setInt(7, item.getQuantity());
        ps.setInt(8, item.getReorderThreshold());
        ps.setString(9, item.getStatus().name());
        ps.setString(10, item.getDateAdded().toString());
        ps.setString(11, item.getImagePath());
    }

    private Item mapRow(ResultSet rs) throws SQLException {
        Item item = new Item();
        item.setItemId(rs.getInt("item_id"));
        item.setSku(rs.getString("sku"));
        item.setName(rs.getString("name"));
        item.setCategoryId(rs.getInt("category_id"));
        item.setCategoryName(rs.getString("category_name"));
        item.setSize(rs.getString("size"));
        item.setColor(rs.getString("color"));
        item.setPrice(rs.getDouble("price"));
        item.setReorderThreshold(rs.getInt("reorder_threshold"));
        item.setQuantity(rs.getInt("quantity"));
        item.setStatus(ItemStatus.valueOf(rs.getString("status")));
        item.setImagePath(rs.getString("image_path"));
        String dateStr = rs.getString("date_added");
        item.setDateAdded(dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now());
        return item;
    }
}
