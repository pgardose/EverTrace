package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.dao.DataAccessException;
import com.clothingstore.inventory.dao.OrderDao;
import com.clothingstore.inventory.model.Order;
import com.clothingstore.inventory.model.OrderItem;
import com.clothingstore.inventory.service.OrderService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.Optional;

public class OrdersView extends BorderPane {

    private final OrderDao orderDao;
    private final OrderService orderService;
    private final Runnable onOrderChanged;

    private final TableView<Order> ordersTable = new TableView<>();
    private final TableView<OrderItem> linesTable = new TableView<>();

    public OrdersView(OrderDao orderDao, OrderService orderService, Runnable onOrderChanged) {
        this.orderDao = orderDao;
        this.orderService = orderService;
        this.onOrderChanged = onOrderChanged;
        setPadding(new Insets(20));

        Label title = new Label("Orders");
        title.setFont(Font.font(null, FontWeight.BOLD, 22));

        buildOrdersTable();
        buildLinesTable();

        ordersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                linesTable.setItems(FXCollections.observableArrayList(orderDao.findLinesForOrder(newSel.getOrderId())));
            } else {
                linesTable.getItems().clear();
            }
        });

        Button cancelBtn = new Button("Cancel Selected Order (restocks items)");
        cancelBtn.setOnAction(e -> onCancelOrder());

        VBox ordersBox = new VBox(8, new Label("Order History"), ordersTable, cancelBtn);
        VBox linesBox = new VBox(8, new Label("Order Details"), linesTable);

        HBox content = new HBox(20, ordersBox, linesBox);
        HBox.setHgrow(ordersBox, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(linesBox, javafx.scene.layout.Priority.ALWAYS);

        setTop(title);
        setCenter(content);

        refresh();
    }

    public void refresh() {
        ordersTable.setItems(FXCollections.observableArrayList(orderDao.findAll()));
        linesTable.getItems().clear();
    }

    private void buildOrdersTable() {
        TableColumn<Order, Number> idCol = new TableColumn<>("Order #");
        idCol.setCellValueFactory(new PropertyValueFactory<>("orderId"));

        TableColumn<Order, String> customerCol = new TableColumn<>("Customer");
        customerCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));

        TableColumn<Order, Number> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));

        TableColumn<Order, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().getStatus().name()));

        ordersTable.getColumns().setAll(List.of(idCol, customerCol, totalCol, statusCol));
        ordersTable.setPrefSize(500, 450);
    }

    private void buildLinesTable() {
        TableColumn<OrderItem, String> nameCol = new TableColumn<>("Item");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));

        TableColumn<OrderItem, Number> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<OrderItem, Number> priceCol = new TableColumn<>("Unit Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));

        linesTable.getColumns().setAll(List.of(nameCol, qtyCol, priceCol));
        linesTable.setPrefSize(400, 450);
    }

    private void onCancelOrder() {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.INFORMATION, "Select an order to cancel first.").showAndWait();
            return;
        }
        if (selected.getStatus() != Order.OrderStatus.COMPLETED) {
            new Alert(Alert.AlertType.WARNING, "Only completed orders can be cancelled.").showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Cancel order #" + selected.getOrderId() + "? This restocks every item in it.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        try {
            orderService.cancelOrder(selected.getOrderId());
            refresh();
            onOrderChanged.run(); // dashboard/inventory need to reflect the restock too
        } catch (DataAccessException ex) {
            new Alert(Alert.AlertType.ERROR, "Cancellation failed: " + ex.getMessage()).showAndWait();
        }
    }
}
