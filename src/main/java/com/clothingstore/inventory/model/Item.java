package com.clothingstore.inventory.model;

import javafx.beans.property.*;

import java.time.LocalDate;

/**
 * Core inventory entity. Fields are JavaFX Properties (not plain fields) so
 * TableViews bound to an Item update on screen the instant its state
 * changes -- e.g. the moment a checkout deducts stock, every open view
 * reflects it with no manual refresh call anywhere.
 */
public class Item implements Sellable {

    private final IntegerProperty itemId = new SimpleIntegerProperty();
    private final StringProperty sku = new SimpleStringProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final IntegerProperty categoryId = new SimpleIntegerProperty();
    private final StringProperty categoryName = new SimpleStringProperty();
    private final StringProperty size = new SimpleStringProperty();
    private final StringProperty color = new SimpleStringProperty();
    private final DoubleProperty price = new SimpleDoubleProperty();
    private final IntegerProperty quantity = new SimpleIntegerProperty();
    private final IntegerProperty reorderThreshold = new SimpleIntegerProperty(5);
    private final ObjectProperty<ItemStatus> status = new SimpleObjectProperty<>(ItemStatus.OUT_OF_STOCK);
    private final ObjectProperty<LocalDate> dateAdded = new SimpleObjectProperty<>(LocalDate.now());

    public Item() {}

    public Item(String sku, String name, int categoryId, String size, String color,
                double price, int quantity, int reorderThreshold) {
        this.sku.set(sku);
        this.name.set(name);
        this.categoryId.set(categoryId);
        this.size.set(size);
        this.color.set(color);
        this.price.set(price);
        this.quantity.set(quantity);
        this.reorderThreshold.set(reorderThreshold);
        this.status.set(ItemStatus.fromQuantity(quantity, reorderThreshold));
    }

    @Override
    public boolean isPurchasable(int requestedQty) {
        return status.get() != ItemStatus.DISCONTINUED
                && requestedQty > 0
                && quantity.get() >= requestedQty;
    }

    @Override
    public void deductStock(int qty) {
        if (qty > quantity.get()) {
            throw new IllegalArgumentException("Cannot deduct more than available stock for " + name.get());
        }
        quantity.set(quantity.get() - qty);
        refreshStatus();
    }

    @Override
    public void restock(int qty) {
        quantity.set(quantity.get() + qty);
        refreshStatus();
    }

    private void refreshStatus() {
        if (status.get() != ItemStatus.DISCONTINUED) {
            status.set(ItemStatus.fromQuantity(quantity.get(), reorderThreshold.get()));
        }
    }

    // --- Standard getters/setters + Property accessors (JavaFX bean convention) ---

    public int getItemId() { return itemId.get(); }
    public void setItemId(int v) { itemId.set(v); }
    public IntegerProperty itemIdProperty() { return itemId; }

    public String getSku() { return sku.get(); }
    public void setSku(String v) { sku.set(v); }
    public StringProperty skuProperty() { return sku; }

    public String getName() { return name.get(); }
    public void setName(String v) { name.set(v); }
    public StringProperty nameProperty() { return name; }

    public int getCategoryId() { return categoryId.get(); }
    public void setCategoryId(int v) { categoryId.set(v); }
    public IntegerProperty categoryIdProperty() { return categoryId; }

    public String getCategoryName() { return categoryName.get(); }
    public void setCategoryName(String v) { categoryName.set(v); }
    public StringProperty categoryNameProperty() { return categoryName; }

    public String getSize() { return size.get(); }
    public void setSize(String v) { size.set(v); }
    public StringProperty sizeProperty() { return size; }

    public String getColor() { return color.get(); }
    public void setColor(String v) { color.set(v); }
    public StringProperty colorProperty() { return color; }

    public double getPrice() { return price.get(); }
    public void setPrice(double v) { price.set(v); }
    public DoubleProperty priceProperty() { return price; }

    public int getQuantity() { return quantity.get(); }
    public void setQuantity(int v) { quantity.set(v); refreshStatus(); }
    public IntegerProperty quantityProperty() { return quantity; }

    public int getReorderThreshold() { return reorderThreshold.get(); }
    public void setReorderThreshold(int v) { reorderThreshold.set(v); refreshStatus(); }
    public IntegerProperty reorderThresholdProperty() { return reorderThreshold; }

    public ItemStatus getStatus() { return status.get(); }
    public void setStatus(ItemStatus v) { status.set(v); }
    public ObjectProperty<ItemStatus> statusProperty() { return status; }

    public LocalDate getDateAdded() { return dateAdded.get(); }
    public void setDateAdded(LocalDate v) { dateAdded.set(v); }
    public ObjectProperty<LocalDate> dateAddedProperty() { return dateAdded; }
}
