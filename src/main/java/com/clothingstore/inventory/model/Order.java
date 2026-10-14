package com.clothingstore.inventory.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order {
    public enum OrderStatus { PENDING, COMPLETED, CANCELLED }

    private int orderId;
    private String customerName;
    private LocalDateTime orderDate;
    private double totalAmount;
    private OrderStatus status = OrderStatus.PENDING;
    private final List<OrderItem> lines = new ArrayList<>();

    public Order() {}

    public Order(String customerName) {
        this.customerName = customerName;
        this.orderDate = LocalDateTime.now();
    }

    public void addLine(OrderItem line) {
        lines.add(line);
        recalculateTotal();
    }

    private void recalculateTotal() {
        this.totalAmount = lines.stream()
                .mapToDouble(l -> l.getQuantity() * l.getUnitPrice())
                .sum();
    }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public List<OrderItem> getLines() { return lines; }
}
