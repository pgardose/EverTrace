package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.model.Item;
import com.clothingstore.inventory.model.OrderLineRequest;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * A cart entry while the user is still building the order. Unlike
 * OrderLineRequest (a record, immutable) this needs to change quantity
 * while sitting in the cart table, so it stays a small mutable class.
 */
public class CartLine {
    private final Item item;
    private final IntegerProperty quantity = new SimpleIntegerProperty(1);

    public CartLine(Item item) {
        this.item = item;
    }

    public Item getItem() { return item; }
    public String getItemName() { return item.getName(); }
    public double getUnitPrice() { return item.getPrice(); }

    public int getQuantity() { return quantity.get(); }
    public void setQuantity(int qty) { quantity.set(qty); }
    public IntegerProperty quantityProperty() { return quantity; }

    public double getLineTotal() { return item.getPrice() * quantity.get(); }

    public OrderLineRequest toRequest() {
        return new OrderLineRequest(item.getItemId(), item.getName(), quantity.get(), item.getPrice());
    }
}
