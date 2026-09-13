package com.clothingstore.inventory.service;

import com.clothingstore.inventory.dao.DataAccessException;
import com.clothingstore.inventory.dao.DatabaseManager;
import com.clothingstore.inventory.dao.ItemDao;
import com.clothingstore.inventory.dao.StockTransactionDao;
import com.clothingstore.inventory.model.Item;
import com.clothingstore.inventory.model.ItemStatus;
import com.clothingstore.inventory.model.StockSummary;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class InventoryService {

    private final ItemDao itemDao;
    private final StockTransactionDao stockTransactionDao;

    public InventoryService(ItemDao itemDao, StockTransactionDao stockTransactionDao) {
        this.itemDao = itemDao;
        this.stockTransactionDao = stockTransactionDao;
    }

    public List<Item> getAllItems() {
        return itemDao.findAll();
    }

    public List<Item> getLowStockItems() {
        return itemDao.findLowStock();
    }

    public Optional<Item> getItem(int id) {
        return itemDao.findById(id);
    }

    public Item addItem(Item item) {
        item.setStatus(ItemStatus.fromQuantity(item.getQuantity(), item.getReorderThreshold()));
        return itemDao.save(item);
    }

    public Item updateItem(Item item) {
        return itemDao.save(item);
    }

    public void removeItem(int id) {
        itemDao.deleteById(id);
    }

    /**
     * Adds stock to an existing item and logs the change as an audit row,
     * in one transaction -- so every quantity change (sale OR restock) is
     * always explainable later by looking at stock_transactions.
     */
    public void restockItem(Item item, int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("Restock quantity must be positive");
        }
        Connection conn = DatabaseManager.getConnection();
        try {
            conn.setAutoCommit(false);
            item.restock(qty);
            itemDao.updateQuantityAndStatus(conn, item);
            stockTransactionDao.logChange(conn, item.getItemId(), qty, "RESTOCK");
            conn.commit();
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new DataAccessException("Restock failed and was rolled back", e);
        } finally {
            restoreAutoCommit(conn);
        }
    }

    /** Builds the dashboard's summary card data with a single pass over the list via streams. */
    public StockSummary getDashboardSummary() {
        List<Item> items = itemDao.findAll();
        return new StockSummary(
                items.size(),
                (int) items.stream().filter(i -> i.getStatus() == ItemStatus.LOW_STOCK).count(),
                (int) items.stream().filter(i -> i.getStatus() == ItemStatus.OUT_OF_STOCK).count(),
                items.stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum()
        );
    }

    private void rollbackQuietly(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException ignored) {
        }
    }

    private void restoreAutoCommit(Connection conn) {
        try {
            conn.setAutoCommit(true);
        } catch (SQLException ignored) {
        }
    }
}
