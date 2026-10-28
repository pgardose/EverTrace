package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.model.Item;
import com.clothingstore.inventory.model.ItemStatus;
import com.clothingstore.inventory.model.Order;
import com.clothingstore.inventory.model.OrderLineRequest;
import com.clothingstore.inventory.service.InsufficientStockException;
import com.clothingstore.inventory.service.InventoryService;
import com.clothingstore.inventory.service.OrderService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

public class CheckoutView extends BorderPane {

    private final InventoryService inventoryService;
    private final OrderService orderService;

    private final TableView<Item> catalogTable = new TableView<>();
    private final TableView<CartLine> cartTable = new TableView<>();
    private final ObservableList<CartLine> cartData = FXCollections.observableArrayList();
    private final Label totalLabel = new Label("Total: ₱0.00");
    private final TextField customerNameField = new TextField();

    private final Runnable onCheckoutComplete;

    public CheckoutView(InventoryService inventoryService, OrderService orderService, Runnable onCheckoutComplete) {
        this.inventoryService = inventoryService;
        this.orderService = orderService;
        this.onCheckoutComplete = onCheckoutComplete;
        setPadding(new Insets(20));

        Label title = new Label("Checkout");
        title.setFont(Font.font(null, FontWeight.BOLD, 22));

        buildCatalogTable();
        buildCartTable();

        Button addToCartBtn = new Button("Add to Cart →");
        addToCartBtn.setOnAction(e -> onAddToCart());

        VBox catalogBox = new VBox(8, new Label("Available Items"), catalogTable, addToCartBtn);

        customerNameField.setPromptText("Customer name (optional)");
        Button checkoutBtn = new Button("Confirm Sale");
        checkoutBtn.setDefaultButton(true);
        checkoutBtn.setOnAction(e -> onConfirmCheckout());

        Button removeLineBtn = new Button("Remove Selected");
        removeLineBtn.setOnAction(e -> onRemoveLine());

        HBox cartActions = new HBox(10, removeLineBtn);
        VBox cartBox = new VBox(8,
                new Label("Cart"), cartTable, cartActions,
                customerNameField, totalLabel, checkoutBtn);

        HBox content = new HBox(20, catalogBox, cartBox);
        HBox.setHgrow(catalogBox, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(cartBox, javafx.scene.layout.Priority.ALWAYS);

        setTop(title);
        setCenter(content);

        refreshCatalog();
    }

    public void refreshCatalog() {
        List<Item> purchasable = inventoryService.getAllItems().stream()
                .filter(i -> i.getStatus() != ItemStatus.OUT_OF_STOCK && i.getStatus() != ItemStatus.DISCONTINUED)
                .toList();
        catalogTable.setItems(FXCollections.observableArrayList(purchasable));
    }

    private void buildCatalogTable() {
        TableColumn<Item, String> photoCol = new TableColumn<>("Photo");
        photoCol.setCellValueFactory(new PropertyValueFactory<>("imagePath"));
        photoCol.setCellFactory(col -> new ThumbnailCell<>());
        photoCol.setSortable(false);
        photoCol.setPrefWidth(60);

        TableColumn<Item, String> nameCol = new TableColumn<>("Item");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<Item, Number> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        TableColumn<Item, Number> qtyCol = new TableColumn<>("In Stock");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        catalogTable.getColumns().setAll(List.of(photoCol, nameCol, priceCol, qtyCol));
        catalogTable.setPrefHeight(400);
        catalogTable.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Item> row = new javafx.scene.control.TableRow<>();
            row.setPrefHeight(48);
            return row;
        });
    }

    private void buildCartTable() {
        TableColumn<CartLine, String> nameCol = new TableColumn<>("Item");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));

        TableColumn<CartLine, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.IntegerStringConverter()));
        qtyCol.setOnEditCommit(evt -> {
            CartLine line = evt.getRowValue();
            int newQty = evt.getNewValue();
            if (newQty <= 0) {
                new Alert(Alert.AlertType.WARNING, "Quantity must be at least 1.").showAndWait();
                cartTable.refresh();
                return;
            }
            if (newQty > line.getItem().getQuantity()) {
                new Alert(Alert.AlertType.WARNING,
                        "Only %d in stock for \"%s\".".formatted(line.getItem().getQuantity(), line.getItemName()))
                        .showAndWait();
                cartTable.refresh();
                return;
            }
            line.setQuantity(newQty);
            updateTotal();
            cartTable.refresh();
        });

        TableColumn<CartLine, Number> lineTotalCol = new TableColumn<>("Line Total");
        lineTotalCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleDoubleProperty(cell.getValue().getLineTotal()));

        cartTable.setEditable(true);
        cartTable.getColumns().setAll(List.of(nameCol, qtyCol, lineTotalCol));
        cartTable.setItems(cartData);
        cartTable.setPrefHeight(360);
    }

    private void onAddToCart() {
        Item selected = catalogTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.INFORMATION, "Select an item from the catalog first.").showAndWait();
            return;
        }

        TextInputDialog qtyDialog = new TextInputDialog("1");
        qtyDialog.setTitle("Add to Cart");
        qtyDialog.setHeaderText(null);
        qtyDialog.setContentText("Quantity of \"" + selected.getName() + "\" (in stock: " + selected.getQuantity() + "):");

        qtyDialog.showAndWait().ifPresent(text -> {
            int qty;
            try {
                qty = Integer.parseInt(text.trim());
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.WARNING, "Enter a whole number.").showAndWait();
                return;
            }
            if (qty <= 0) {
                new Alert(Alert.AlertType.WARNING, "Quantity must be at least 1.").showAndWait();
                return;
            }

            // Check against what's already in the cart too, not just raw stock --
            // otherwise you could add 5+5 of a 6-in-stock item and only find out at checkout.
            int alreadyInCart = cartData.stream()
                    .filter(line -> line.getItem().getItemId() == selected.getItemId())
                    .mapToInt(CartLine::getQuantity)
                    .sum();
            if (alreadyInCart + qty > selected.getQuantity()) {
                new Alert(Alert.AlertType.WARNING,
                        "Only %d in stock (%d already in your cart).".formatted(selected.getQuantity(), alreadyInCart))
                        .showAndWait();
                return;
            }

            cartData.stream()
                    .filter(line -> line.getItem().getItemId() == selected.getItemId())
                    .findFirst()
                    .ifPresentOrElse(
                            existing -> existing.setQuantity(existing.getQuantity() + qty),
                            () -> {
                                CartLine newLine = new CartLine(selected);
                                newLine.setQuantity(qty);
                                cartData.add(newLine);
                            }
                    );
            updateTotal();
            cartTable.refresh();
        });
    }

    private void onRemoveLine() {
        CartLine selected = cartTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            cartData.remove(selected);
            updateTotal();
        }
    }

    private void updateTotal() {
        double total = cartData.stream().mapToDouble(CartLine::getLineTotal).sum();
        totalLabel.setText("Total: ₱%,.2f".formatted(total));
    }

    private void onConfirmCheckout() {
        if (cartData.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Cart is empty.").showAndWait();
            return;
        }

        List<OrderLineRequest> requestLines = cartData.stream().map(CartLine::toRequest).toList();
        String customerName = customerNameField.getText().isBlank() ? "Walk-in" : customerNameField.getText().trim();

        try {
            Order order = orderService.checkout(customerName, requestLines);
            new Alert(Alert.AlertType.INFORMATION,
                    "Sale #%d completed. Total: ₱%,.2f".formatted(order.getOrderId(), order.getTotalAmount()))
                    .showAndWait();

            cartData.clear();
            customerNameField.clear();
            updateTotal();
            refreshCatalog();
            onCheckoutComplete.run(); // lets App refresh Dashboard/Inventory screens too
        } catch (InsufficientStockException ex) {
            // Exactly the scenario this whole system exists to prevent: someone else
            // bought the last unit between browsing and confirming. Refresh and tell them.
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
            refreshCatalog();
        } catch (com.clothingstore.inventory.dao.DataAccessException ex) {
            new Alert(Alert.AlertType.ERROR, "Checkout failed and was rolled back. No stock was changed.").showAndWait();
        }
    }
}
