package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.model.Item;
import com.clothingstore.inventory.model.ItemStatus;
import com.clothingstore.inventory.model.Order;
import com.clothingstore.inventory.model.OrderLineRequest;
import com.clothingstore.inventory.service.InsufficientStockException;
import com.clothingstore.inventory.service.InventoryService;
import com.clothingstore.inventory.service.OrderService;
import com.clothingstore.inventory.util.Money;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;

import java.util.Comparator;
import java.util.List;

public class CheckoutView extends BorderPane {

    private final InventoryService inventoryService;
    private final OrderService orderService;

    private final TableView<Item> catalogTable = new TableView<>();
    private final ObservableList<Item> catalogData = FXCollections.observableArrayList();
    private final FilteredList<Item> catalogFiltered = new FilteredList<>(catalogData, i -> true);
    private final TextField catalogSearch = new TextField();
    private final Spinner<Integer> qtySpinner = new Spinner<>(1, 999, 1);

    private final TableView<CartLine> cartTable = new TableView<>();
    private final ObservableList<CartLine> cartData = FXCollections.observableArrayList();
    private final Label itemsValue = new Label("0");
    private final Label totalLabel = new Label(Money.peso(0));
    private final TextField customerNameField = new TextField();
    private final Button checkoutBtn = Ui.button("Confirm sale", "check-circle", "accent", "large");

    private final Runnable onCheckoutComplete;

    public CheckoutView(InventoryService inventoryService, OrderService orderService, Runnable onCheckoutComplete) {
        this.inventoryService = inventoryService;
        this.orderService = orderService;
        this.onCheckoutComplete = onCheckoutComplete;
        setPadding(new Insets(28, 32, 28, 32));

        buildCatalogTable();
        buildCartTable();

        // ---------------- left: products
        catalogSearch.setPromptText("Search products");
        catalogSearch.setAccessibleText("Search products by name or SKU");
        catalogSearch.textProperty().addListener((o, a, q) -> {
            String needle = q == null ? "" : q.trim().toLowerCase();
            catalogFiltered.setPredicate(i -> needle.isEmpty()
                    || i.getName().toLowerCase().contains(needle)
                    || i.getSku().toLowerCase().contains(needle));
        });
        // Enter in the search box adds the top match -- quick for barcode-style SKU entry.
        catalogSearch.setOnAction(e -> {
            e.consume();
            if (!catalogTable.getItems().isEmpty()) {
                catalogTable.getSelectionModel().selectFirst();
                onAddToCart();
            }
        });
        catalogSearch.setMaxWidth(Double.MAX_VALUE);

        qtySpinner.setEditable(true);
        qtySpinner.setPrefWidth(84);
        qtySpinner.setAccessibleText("Quantity to add");
        Label qtyLabel = new Label("Qty");
        qtyLabel.setLabelFor(qtySpinner);
        qtyLabel.getStyleClass().add("muted");

        Button addToCartBtn = Ui.button("Add to cart", "plus", "accent");
        addToCartBtn.setOnAction(e -> onAddToCart());
        addToCartBtn.disableProperty().bind(catalogTable.getSelectionModel().selectedItemProperty().isNull());

        Label addHint = new Label("Double-click a product to add one");
        addHint.getStyleClass().add("kbd-hint");
        Region s1 = new Region();
        HBox.setHgrow(s1, Priority.ALWAYS);
        HBox addRow = new HBox(8, addHint, s1, qtyLabel, qtySpinner, addToCartBtn);
        addRow.setAlignment(Pos.CENTER_LEFT);

        VBox catalogCard = Ui.card("Products", catalogSearch, catalogTable, addRow);
        VBox.setVgrow(catalogTable, Priority.ALWAYS);

        // ---------------- right: current sale
        Button removeLineBtn = Ui.button("Remove", "trash", "danger", "flat");
        removeLineBtn.setOnAction(e -> onRemoveLine());
        removeLineBtn.disableProperty().bind(cartTable.getSelectionModel().selectedItemProperty().isNull());
        Button clearBtn = Ui.button("Clear", null, "flat");
        clearBtn.setOnAction(e -> {
            cartData.clear();
            updateTotal();
        });
        clearBtn.disableProperty().bind(javafx.beans.binding.Bindings.isEmpty(cartData));

        Label cartTitle = new Label("Current sale");
        cartTitle.getStyleClass().add("card-title");
        Region s2 = new Region();
        HBox.setHgrow(s2, Priority.ALWAYS);
        HBox cartHeader = new HBox(4, cartTitle, s2, removeLineBtn, clearBtn);
        cartHeader.setAlignment(Pos.CENTER_LEFT);

        customerNameField.setPromptText("Walk-in");
        VBox customerBox = Ui.field("Customer name (optional)", customerNameField);

        Label itemsLabel = new Label("Items");
        Region s3 = new Region();
        HBox.setHgrow(s3, Priority.ALWAYS);
        HBox itemsRow = new HBox(itemsLabel, s3, itemsValue);
        itemsRow.getStyleClass().add("summary-row");

        Label totalCaption = new Label("Total");
        totalCaption.getStyleClass().add("total-label");
        totalLabel.getStyleClass().add("total-value");
        Region s4 = new Region();
        HBox.setHgrow(s4, Priority.ALWAYS);
        HBox totalRow = new HBox(totalCaption, s4, totalLabel);
        totalRow.setAlignment(Pos.BASELINE_LEFT);

        Region divider = new Region();
        divider.getStyleClass().add("divider");

        // Deliberately NOT the default button: Enter is used to search and add
        // products, and an accidental Enter shouldn't complete a sale.
        checkoutBtn.setMaxWidth(Double.MAX_VALUE);
        checkoutBtn.setOnAction(e -> onConfirmCheckout());
        checkoutBtn.disableProperty().bind(javafx.beans.binding.Bindings.isEmpty(cartData));

        VBox cartCard = Ui.card(null, cartHeader, cartTable, customerBox, divider, itemsRow, totalRow, checkoutBtn);
        VBox.setVgrow(cartTable, Priority.ALWAYS);
        cartCard.setMinWidth(340);
        cartCard.setPrefWidth(380);
        cartCard.setMaxWidth(420);

        HBox content = new HBox(18, catalogCard, cartCard);
        HBox.setHgrow(catalogCard, Priority.ALWAYS);

        Label subtitle = new Label("Pick products on the left, then confirm the sale.");
        setTop(Ui.pageHeader("Checkout", subtitle));
        setCenter(content);

        refreshCatalog();
    }

    public void refreshCatalog() {
        List<Item> purchasable = inventoryService.getAllItems().stream()
                .filter(i -> i.getStatus() != ItemStatus.OUT_OF_STOCK && i.getStatus() != ItemStatus.DISCONTINUED)
                .sorted(Comparator.comparing(Item::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        catalogData.setAll(purchasable);
    }

    private void buildCatalogTable() {
        Ui.setupTable(catalogTable);

        TableColumn<Item, String> photoCol = new TableColumn<>("");
        photoCol.setCellValueFactory(new PropertyValueFactory<>("imagePath"));
        photoCol.setCellFactory(col -> new ThumbnailCell<>(36));
        photoCol.setSortable(false);
        photoCol.setMinWidth(58);
        photoCol.setMaxWidth(58);

        TableColumn<Item, Item> nameCol = new TableColumn<>("PRODUCT");
        nameCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        nameCol.setCellFactory(col -> Ui.twoLineCell(Item::getName,
                i -> join(i.getSize(), i.getColor(), i.getSku())));
        nameCol.setComparator(Comparator.comparing(Item::getName, String.CASE_INSENSITIVE_ORDER));
        nameCol.setMinWidth(180);

        TableColumn<Item, Number> priceCol = new TableColumn<>("PRICE");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setCellFactory(col -> Ui.moneyCell());
        priceCol.getStyleClass().add("numeric");
        priceCol.setMinWidth(96);

        TableColumn<Item, Number> qtyCol = new TableColumn<>("IN STOCK");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setCellFactory(col -> Ui.numericCell());
        qtyCol.getStyleClass().add("numeric");
        qtyCol.setMinWidth(76);
        qtyCol.setMaxWidth(96);

        catalogTable.getColumns().setAll(List.of(photoCol, nameCol, priceCol, qtyCol));
        catalogTable.setRowFactory(tv -> {
            TableRow<Item> row = new TableRow<>();
            row.setPrefHeight(52);
            row.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !row.isEmpty()) {
                    addToCart(row.getItem(), 1);
                }
            });
            return row;
        });
        catalogTable.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                e.consume();
                onAddToCart();
            }
        });
        catalogTable.setItems(catalogFiltered);
        catalogTable.setPlaceholder(Ui.emptyState("box", "Nothing to sell",
                "Only items that are in stock appear here. Restock items from Inventory."));
    }

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (sb.length() > 0) sb.append("  ·  ");
            sb.append(p);
        }
        return sb.toString();
    }

    private void buildCartTable() {
        Ui.setupTable(cartTable);

        TableColumn<CartLine, String> nameCol = new TableColumn<>("ITEM");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        nameCol.setMinWidth(120);

        TableColumn<CartLine, Integer> qtyCol = new TableColumn<>("QTY");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.IntegerStringConverter()));
        qtyCol.setMinWidth(56);
        qtyCol.setMaxWidth(70);
        qtyCol.setOnEditCommit(evt -> {
            CartLine line = evt.getRowValue();
            Integer newValue = evt.getNewValue();
            int newQty = newValue == null ? 0 : newValue;
            if (newQty <= 0) {
                Ui.alert(Alert.AlertType.WARNING, "Quantity must be at least 1.").showAndWait();
                cartTable.refresh();
                return;
            }
            if (newQty > line.getItem().getQuantity()) {
                Ui.alert(Alert.AlertType.WARNING,
                        "Only %d in stock for \"%s\".".formatted(line.getItem().getQuantity(), line.getItemName()))
                        .showAndWait();
                cartTable.refresh();
                return;
            }
            line.setQuantity(newQty);
            updateTotal();
            cartTable.refresh();
        });

        TableColumn<CartLine, Number> lineTotalCol = new TableColumn<>("TOTAL");
        lineTotalCol.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getLineTotal()));
        lineTotalCol.setCellFactory(col -> Ui.moneyCell());
        lineTotalCol.getStyleClass().add("numeric");
        lineTotalCol.setMinWidth(96);

        cartTable.setEditable(true);
        cartTable.getColumns().setAll(List.of(nameCol, qtyCol, lineTotalCol));
        cartTable.setItems(cartData);
        cartTable.setRowFactory(tv -> {
            TableRow<CartLine> row = new TableRow<>();
            row.setPrefHeight(40);
            return row;
        });
        cartTable.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) onRemoveLine();
        });
        cartTable.setPlaceholder(Ui.emptyState("cart", "Cart is empty",
                "Double-click a product, or select it and press Add to cart."));
        cartTable.setMinHeight(160);
    }

    private void onAddToCart() {
        Item selected = catalogTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Ui.alert(Alert.AlertType.INFORMATION, "Select a product first.").showAndWait();
            return;
        }
        int qty;
        try {
            // The spinner only commits typed text on Enter; read the editor directly.
            qty = Integer.parseInt(qtySpinner.getEditor().getText().trim());
        } catch (NumberFormatException ex) {
            Ui.alert(Alert.AlertType.WARNING, "Enter a whole number for the quantity.").showAndWait();
            return;
        }
        if (addToCart(selected, qty)) {
            qtySpinner.getValueFactory().setValue(1);
        }
    }

    /** Adds qty of an item, checking live stock minus what's already in the cart. Returns true if added. */
    private boolean addToCart(Item selected, int qty) {
        if (qty <= 0) {
            Ui.alert(Alert.AlertType.WARNING, "Quantity must be at least 1.").showAndWait();
            return false;
        }

        // Check against what's already in the cart too, not just raw stock --
        // otherwise you could add 5+5 of a 6-in-stock item and only find out at checkout.
        int alreadyInCart = cartData.stream()
                .filter(line -> line.getItem().getItemId() == selected.getItemId())
                .mapToInt(CartLine::getQuantity)
                .sum();
        if (alreadyInCart + qty > selected.getQuantity()) {
            Ui.alert(Alert.AlertType.WARNING,
                    "Only %d of “%s” in stock (%d already in this sale)."
                            .formatted(selected.getQuantity(), selected.getName(), alreadyInCart))
                    .showAndWait();
            return false;
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
        return true;
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
        int units = cartData.stream().mapToInt(CartLine::getQuantity).sum();
        totalLabel.setText(Money.peso(total));
        itemsValue.setText(String.valueOf(units));
        checkoutBtn.setText(cartData.isEmpty() ? "Confirm sale" : "Confirm sale · " + Money.peso(total));
    }

    private void onConfirmCheckout() {
        if (cartData.isEmpty()) {
            Ui.alert(Alert.AlertType.WARNING, "Cart is empty.").showAndWait();
            return;
        }

        List<OrderLineRequest> requestLines = cartData.stream().map(CartLine::toRequest).toList();
        String customerName = customerNameField.getText().isBlank() ? "Walk-in" : customerNameField.getText().trim();

        try {
            Order order = orderService.checkout(customerName, requestLines);
            Alert done = Ui.alert(Alert.AlertType.INFORMATION,
                    "Sale #%d completed.\nTotal: %s".formatted(order.getOrderId(), Money.peso(order.getTotalAmount())));
            done.setTitle("Sale completed");
            done.showAndWait();

            cartData.clear();
            customerNameField.clear();
            catalogSearch.clear();
            updateTotal();
            refreshCatalog();
            onCheckoutComplete.run(); // lets App refresh Dashboard/Inventory screens too
        } catch (InsufficientStockException ex) {
            // Exactly the scenario this whole system exists to prevent: someone else
            // bought the last unit between browsing and confirming. Refresh and tell them.
            Ui.alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
            refreshCatalog();
        } catch (com.clothingstore.inventory.dao.DataAccessException ex) {
            Ui.alert(Alert.AlertType.ERROR, "Checkout failed and was rolled back. No stock was changed.").showAndWait();
        }
    }
}
