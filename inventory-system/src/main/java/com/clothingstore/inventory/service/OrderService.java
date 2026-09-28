package com.clothingstore.inventory.service;

import com.clothingstore.inventory.dao.DataAccessException;
import com.clothingstore.inventory.dao.DatabaseManager;
import com.clothingstore.inventory.dao.ItemDao;
import com.clothingstore.inventory.dao.OrderDao;
import com.clothingstore.inventory.dao.StockTransactionDao;
import com.clothingstore.inventory.model.Item;
import com.clothingstore.inventory.model.Order;
import com.clothingstore.inventory.model.OrderItem;
import com.clothingstore.inventory.model.OrderLineRequest;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Owns the checkout flow. This is the class that directly solves the
 * client's original problem: an item can never be sold twice, because
 * every checkout re-checks live stock and commits the deduction in the
 * same database transaction as the order itself.
 */
public class OrderService {

    private final ItemDao itemDao;
    private final OrderDao orderDao;
    private final StockTransactionDao stockTransactionDao;

    public OrderService(ItemDao itemDao, OrderDao orderDao, StockTransactionDao stockTransactionDao) {
        this.itemDao = itemDao;
        this.orderDao = orderDao;
        this.stockTransactionDao = stockTransactionDao;
    }

    public Order checkout(String customerName, List<OrderLineRequest> requestedLines) {
        // 1. Look up every item fresh from the DB and validate BEFORE writing anything.
        //    Fail fast so a bad line never leaves a half-completed order behind.
        List<Item> liveItems = requestedLines.stream()
                .map(line -> itemDao.findById(line.itemId())
                        .orElseThrow(() -> new NoSuchElementException("Item not found: " + line.itemId())))
                .toList();

        for (int i = 0; i < liveItems.size(); i++) {
            Item item = liveItems.get(i);
            int requestedQty = requestedLines.get(i).quantity();
            if (!item.isPurchasable(requestedQty)) {
                throw new InsufficientStockException(item.getName(), requestedQty, item.getQuantity());
            }
        }

        // 2. Everything validated -- commit the sale as one atomic database transaction.
        Order order = new Order(customerName);
        Connection conn = DatabaseManager.getConnection();
        try {
            conn.setAutoCommit(false);

            for (int i = 0; i < liveItems.size(); i++) {
                Item item = liveItems.get(i);
                int qty = requestedLines.get(i).quantity();

                item.deductStock(qty);                                  // updates in-memory + bound UI instantly
                itemDao.updateQuantityAndStatus(conn, item);            // persists the new quantity/status
                stockTransactionDao.logChange(conn, item.getItemId(), -qty, "SALE"); // audit row

                order.addLine(new OrderItem(item.getItemId(), item.getName(), qty, item.getPrice()));
            }

            order.setStatus(Order.OrderStatus.COMPLETED);
            orderDao.save(conn, order);

            conn.commit();
            return order;

        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new DataAccessException("Checkout failed and was rolled back", e);
        } finally {
            restoreAutoCommit(conn);
        }
    }

    /**
     * Cancels a completed order: restocks every item it contained, logs a
     * CANCELLATION audit row for each, and flips the order's status --
     * all as one transaction so a partial cancellation can never happen.
     */
    public void cancelOrder(int orderId) {
        List<OrderItem> lines = orderDao.findLinesForOrder(orderId);
        Connection conn = DatabaseManager.getConnection();
        try {
            conn.setAutoCommit(false);

            for (OrderItem line : lines) {
                Item item = itemDao.findById(line.getItemId())
                        .orElseThrow(() -> new NoSuchElementException("Item not found: " + line.getItemId()));
                item.restock(line.getQuantity());
                itemDao.updateQuantityAndStatus(conn, item);
                stockTransactionDao.logChange(conn, item.getItemId(), line.getQuantity(), "CANCELLATION");
            }

            orderDao.updateStatus(conn, orderId, Order.OrderStatus.CANCELLED);
            conn.commit();

        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new DataAccessException("Cancellation failed and was rolled back", e);
        } finally {
            restoreAutoCommit(conn);
        }
    }

    private void rollbackQuietly(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException ignored) {
            // Best effort: connection may already be unusable if we got here.
        }
    }

    private void restoreAutoCommit(Connection conn) {
        try {
            conn.setAutoCommit(true);
        } catch (SQLException ignored) {
        }
    }
}
