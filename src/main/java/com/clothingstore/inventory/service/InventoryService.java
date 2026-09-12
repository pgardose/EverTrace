package com.clothingstore.inventory.service;

import com.clothingstore.inventory.dao.ItemDao;
import com.clothingstore.inventory.model.Item;
import com.clothingstore.inventory.model.ItemStatus;
import com.clothingstore.inventory.model.StockSummary;

import java.util.List;
import java.util.Optional;

public class InventoryService {

    private final ItemDao itemDao;

    public InventoryService(ItemDao itemDao) {
        this.itemDao = itemDao;
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
}
