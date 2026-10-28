package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.dao.DataAccessException;
import com.clothingstore.inventory.dao.OrderDao;
import com.clothingstore.inventory.model.Order;
import com.clothingstore.inventory.model.OrderItem;
import com.clothingstore.inventory.service.OrderService;
import com.clothingstore.inventory.util.Money;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class OrdersView extends BorderPane {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("MMM d, yyyy  h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_THIS_YEAR =
            DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_OTHER_YEAR =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    /** Compact date for the list: the year only when it isn't this year. */
    private static String shortDate(LocalDateTime d) {
        return d.getYear() == java.time.LocalDate.now().getYear()
                ? d.format(SHORT_THIS_YEAR) : d.format(SHORT_OTHER_YEAR);
    }

    private final OrderDao orderDao;
    private final OrderService orderService;
    private final Runnable onOrderChanged;

    private final TableView<Order> ordersTable = new TableView<>();
    private final ObservableList<Order> orders = FXCollections.observableArrayList();
    private final FilteredList<Order> filtered = new FilteredList<>(orders, o -> true);
    private final TextField searchField = new TextField();
    private final ToggleGroup statusGroup = new ToggleGroup();
    private final TableView<OrderItem> linesTable = new TableView<>();
    private final Label subtitle = new Label();

    // detail panel
    private final Label detailTitle = new Label("Order details");
    private final Label detailMeta = new Label();
    private final Label detailBadge = new Label();
    private final Label detailTotal = new Label();
    private final Button cancelBtn = Ui.button("Cancel order", "ban", "danger");
    private final VBox detailBody = new VBox(12);
    private final StackPane detailArea = new StackPane();
    private final javafx.scene.Node detailEmpty =
            Ui.emptyState("receipt", "No order selected", "Pick an order on the left to see what was sold.");

    public OrdersView(OrderDao orderDao, OrderService orderService, Runnable onOrderChanged) {
        this.orderDao = orderDao;
        this.orderService = orderService;
        this.onOrderChanged = onOrderChanged;
        setPadding(new Insets(28, 32, 28, 32));

        buildOrdersTable();
        buildLinesTable();

        // ---- left: order list with search + status filter
        searchField.setPromptText("Search customer or order #");
        searchField.setAccessibleText("Search orders by customer or order number");
        searchField.textProperty().addListener((o, a, b) -> applyFilter());
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.setMaxWidth(Double.MAX_VALUE);

        HBox segmented = new HBox(2);
        segmented.getStyleClass().add("segmented");
        for (String label : List.of("All", "Completed", "Cancelled")) {
            ToggleButton t = new ToggleButton(label);
            t.setToggleGroup(statusGroup);
            t.setUserData(label);
            segmented.getChildren().add(t);
            // Keep exactly one selected (a ToggleGroup otherwise allows none).
            t.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> { if (t.isSelected()) e.consume(); });
        }
        ((ToggleButton) segmented.getChildren().get(0)).setSelected(true);
        statusGroup.selectedToggleProperty().addListener((o, a, b) -> applyFilter());

        HBox toolbar = new HBox(10, searchField, segmented);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        VBox listCard = Ui.card(null, toolbar, ordersTable);
        VBox.setVgrow(ordersTable, Priority.ALWAYS);

        // ---- right: details
        detailTitle.getStyleClass().add("card-title");
        detailMeta.getStyleClass().add("muted");
        detailBadge.getStyleClass().add("status-badge");
        detailBadge.setMinWidth(Region.USE_PREF_SIZE);
        Region s1 = new Region();
        HBox.setHgrow(s1, Priority.ALWAYS);
        HBox titleRow = new HBox(8, detailTitle, s1, detailBadge);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label totalCaption = new Label("Total");
        totalCaption.getStyleClass().add("total-label");
        detailTotal.getStyleClass().add("total-value");
        Region s2 = new Region();
        HBox.setHgrow(s2, Priority.ALWAYS);
        HBox totalRow = new HBox(totalCaption, s2, detailTotal);
        totalRow.setAlignment(Pos.BASELINE_LEFT);

        Region divider = new Region();
        divider.getStyleClass().add("divider");

        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setOnAction(e -> onCancelOrder());
        Label cancelHint = new Label("Cancelling puts every item back into stock.");
        cancelHint.getStyleClass().add("kbd-hint");

        detailBody.getChildren().addAll(new VBox(2, titleRow, detailMeta), linesTable, divider, totalRow, cancelBtn, cancelHint);
        VBox.setVgrow(linesTable, Priority.ALWAYS);
        detailArea.getChildren().addAll(detailBody, detailEmpty);
        VBox detailCard = Ui.card(null, detailArea);
        VBox.setVgrow(detailArea, Priority.ALWAYS);
        detailCard.setMinWidth(330);
        detailCard.setPrefWidth(360);
        detailCard.setMaxWidth(440);

        ordersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> showDetails(newSel));
        showDetails(null);

        HBox content = new HBox(18, listCard, detailCard);
        HBox.setHgrow(listCard, Priority.ALWAYS);

        setTop(Ui.pageHeader("Orders", subtitle));
        setCenter(content);

        refresh();
    }

    public void refresh() {
        orders.setAll(orderDao.findAll());
        applyFilter();
        long completed = orders.stream().filter(o -> o.getStatus() == Order.OrderStatus.COMPLETED).count();
        subtitle.setText(orders.size() + (orders.size() == 1 ? " order" : " orders")
                + "  ·  " + completed + " completed  ·  " + (orders.size() - completed) + " cancelled or pending");
        showDetails(ordersTable.getSelectionModel().getSelectedItem());
    }

    private void applyFilter() {
        String needle = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase().replace("#", "");
        String status = statusGroup.getSelectedToggle() == null ? "All" : (String) statusGroup.getSelectedToggle().getUserData();
        filtered.setPredicate(o ->
                (needle.isEmpty()
                        || String.valueOf(o.getOrderId()).equals(needle)
                        || (o.getCustomerName() != null && o.getCustomerName().toLowerCase().contains(needle)))
                && switch (status) {
                    case "Completed" -> o.getStatus() == Order.OrderStatus.COMPLETED;
                    case "Cancelled" -> o.getStatus() == Order.OrderStatus.CANCELLED;
                    default -> true;
                });
    }

    private void showDetails(Order order) {
        boolean has = order != null;
        detailBody.setVisible(has);
        detailEmpty.setVisible(!has);
        if (!has) {
            linesTable.getItems().clear();
            return;
        }
        detailTitle.setText("Order #" + order.getOrderId());
        String when = order.getOrderDate() == null ? "" : order.getOrderDate().format(DATE_TIME);
        detailMeta.setText((order.getCustomerName() == null ? "Walk-in" : order.getCustomerName())
                + (when.isEmpty() ? "" : "  ·  " + when));
        Ui.applyOrderStatus(detailBadge, order.getStatus());
        detailTotal.setText(Money.peso(order.getTotalAmount()));
        linesTable.setItems(FXCollections.observableArrayList(orderDao.findLinesForOrder(order.getOrderId())));
        boolean cancellable = order.getStatus() == Order.OrderStatus.COMPLETED;
        cancelBtn.setDisable(!cancellable);
        cancelBtn.setText(cancellable ? "Cancel order" : "Order " + order.getStatus().name().toLowerCase());
    }

    private void buildOrdersTable() {
        Ui.setupTable(ordersTable);

        TableColumn<Order, Number> idCol = new TableColumn<>("ORDER");
        idCol.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        idCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number id, boolean empty) {
                super.updateItem(id, empty);
                setText(empty || id == null ? null : "#" + id);
            }
        });
        idCol.setMinWidth(62);
        idCol.setMaxWidth(80);

        TableColumn<Order, LocalDateTime> dateCol = new TableColumn<>("DATE");
        dateCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getOrderDate()));
        dateCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : shortDate(d));
            }
        });
        dateCol.setComparator(Comparator.nullsLast(Comparator.naturalOrder()));
        dateCol.setMinWidth(142);

        TableColumn<Order, String> customerCol = new TableColumn<>("CUSTOMER");
        customerCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        customerCol.setMinWidth(90);

        TableColumn<Order, Number> totalCol = new TableColumn<>("TOTAL");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        totalCol.setCellFactory(col -> Ui.moneyCell());
        totalCol.getStyleClass().add("numeric");
        totalCol.setMinWidth(96);

        TableColumn<Order, Order.OrderStatus> statusCol = new TableColumn<>("STATUS");
        statusCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getStatus()));
        statusCol.setCellFactory(col -> Ui.orderStatusCell());
        statusCol.setMinWidth(112);
        statusCol.setMaxWidth(130);

        ordersTable.getColumns().setAll(List.of(idCol, dateCol, customerCol, totalCol, statusCol));
        ordersTable.setRowFactory(tv -> {
            TableRow<Order> row = new TableRow<>();
            row.setPrefHeight(44);
            return row;
        });
        SortedList<Order> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(ordersTable.comparatorProperty());
        ordersTable.setItems(sorted);
        ordersTable.setPlaceholder(Ui.emptyState("receipt", "No orders",
                "Completed sales from Checkout will be listed here."));
    }

    private void buildLinesTable() {
        Ui.setupTable(linesTable);

        TableColumn<OrderItem, String> nameCol = new TableColumn<>("ITEM");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        nameCol.setMinWidth(90);

        TableColumn<OrderItem, Number> qtyCol = new TableColumn<>("QTY");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setCellFactory(col -> Ui.numericCell());
        qtyCol.getStyleClass().add("numeric");
        qtyCol.setMinWidth(44);
        qtyCol.setMaxWidth(56);

        TableColumn<OrderItem, Number> priceCol = new TableColumn<>("PRICE");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        priceCol.setCellFactory(col -> Ui.moneyCell());
        priceCol.getStyleClass().add("numeric");
        priceCol.setMinWidth(84);

        TableColumn<OrderItem, Number> lineCol = new TableColumn<>("TOTAL");
        lineCol.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getUnitPrice() * c.getValue().getQuantity()));
        lineCol.setCellFactory(col -> Ui.moneyCell());
        lineCol.getStyleClass().add("numeric");
        lineCol.setMinWidth(90);

        linesTable.getColumns().setAll(List.of(nameCol, qtyCol, priceCol, lineCol));
        linesTable.setPlaceholder(Ui.muted("No line items."));
        linesTable.setRowFactory(tv -> {
            TableRow<OrderItem> row = new TableRow<>();
            row.setPrefHeight(38);
            return row;
        });
        linesTable.setMinHeight(140);
    }

    private void onCancelOrder() {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Ui.alert(Alert.AlertType.INFORMATION, "Select an order to cancel first.").showAndWait();
            return;
        }
        if (selected.getStatus() != Order.OrderStatus.COMPLETED) {
            Ui.alert(Alert.AlertType.WARNING, "Only completed orders can be cancelled.").showAndWait();
            return;
        }

        Alert confirm = Ui.alert(Alert.AlertType.CONFIRMATION,
                "Cancel order #" + selected.getOrderId() + " (" + Money.peso(selected.getTotalAmount()) + ")?\n\n"
                        + "Every item in it goes back into stock and the sale is removed from your totals.");
        confirm.setTitle("Cancel order");
        ButtonType cancelOrder = new ButtonType("Cancel order", ButtonBar.ButtonData.OK_DONE);
        ButtonType keep = new ButtonType("Keep order", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(keep, cancelOrder);
        Button danger = (Button) confirm.getDialogPane().lookupButton(cancelOrder);
        danger.getStyleClass().add("danger");
        danger.setDefaultButton(false);
        ((Button) confirm.getDialogPane().lookupButton(keep)).setDefaultButton(true);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != cancelOrder) return;

        try {
            int id = selected.getOrderId();
            orderService.cancelOrder(id);
            refresh();
            onOrderChanged.run(); // dashboard/inventory need to reflect the restock too
            // keep the same order selected so the user sees its new status
            ordersTable.getItems().stream().filter(o -> o.getOrderId() == id).findFirst()
                    .ifPresent(o -> ordersTable.getSelectionModel().select(o));
        } catch (DataAccessException ex) {
            Ui.alert(Alert.AlertType.ERROR, "Cancellation failed: " + ex.getMessage()).showAndWait();
        }
    }
}
