package com.clothingstore.inventory.model;

public class OrderItem {
    private int orderItemId;
    private int orderId;
    private int itemId;
    private String itemName;
    private int quantity;
    private double unitPrice;

    public OrderItem() {}

    public OrderItem(int itemId, String itemName, int quantity, double unitPrice) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public int getOrderItemId() { return orderItemId; }
    public void setOrderItemId(int v) { this.orderItemId = v; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int v) { this.orderId = v; }

    public int getItemId() { return itemId; }
    public void setItemId(int v) { this.itemId = v; }

    public String getItemName() { return itemName; }
    public void setItemName(String v) { this.itemName = v; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int v) { this.quantity = v; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double v) { this.unitPrice = v; }
}
