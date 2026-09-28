package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.model.Item;
import com.clothingstore.inventory.model.ItemStatus;
import com.clothingstore.inventory.model.StockSummary;
import com.clothingstore.inventory.service.InventoryService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class DashboardView extends BorderPane {

    private final InventoryService inventoryService;
    private final HBox summaryCards = new HBox(16);
    private final TableView<Item> lowStockTable = new TableView<>();

    public DashboardView(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
        setPadding(new Insets(20));

        Label title = new Label("Dashboard");
        title.setFont(Font.font(null, FontWeight.BOLD, 22));

        summaryCards.setPadding(new Insets(16, 0, 16, 0));

        buildLowStockTable();

        Label lowStockLabel = new Label("Needs Attention (Low / Out of Stock)");
        lowStockLabel.setFont(Font.font(null, FontWeight.SEMI_BOLD, 15));

        VBox center = new VBox(8, lowStockLabel, lowStockTable);
        VBox top = new VBox(4, title, summaryCards);

        setTop(top);
        setCenter(center);

        refresh();
    }

    /** Call this whenever the screen is shown so the numbers reflect the latest sales/restocks. */
    public void refresh() {
        StockSummary summary = inventoryService.getDashboardSummary();
        summaryCards.getChildren().setAll(
                card("\uD83D\uDCE6", "Total Items", String.valueOf(summary.totalItems())),
                card("\u26A0\uFE0F", "Low Stock", String.valueOf(summary.lowStockCount())),
                card("\u274C", "Out of Stock", String.valueOf(summary.outOfStockCount())),
                card("\uD83D\uDCB0", "Inventory Value", "\u20B1%,.2f".formatted(summary.totalInventoryValue()))
        );
        lowStockTable.setItems(FXCollections.observableArrayList(inventoryService.getLowStockItems()));
    }

    /** A single KPI card: icon glyph, big value, small caption label -- styled by styles.css .kpi-card. */
    private VBox card(String iconGlyph, String label, String value) {
        Label iconLabel = new Label(iconGlyph);
        iconLabel.getStyleClass().add("kpi-icon");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("kpi-value");

        Label captionLabel = new Label(label);
        captionLabel.getStyleClass().add("kpi-label");

        HBox topRow = new HBox(8, iconLabel, valueLabel);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(6, topRow, captionLabel);
        box.getStyleClass().add("kpi-card");
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPrefWidth(190);
        return box;
    }

    private void buildLowStockTable() {
        TableColumn<Item, String> nameCol = new TableColumn<>("Item");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Item, Number> qtyCol = new TableColumn<>("Qty Left");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<Item, ItemStatus> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new StatusBadgeCell<>());

        lowStockTable.getColumns().setAll(java.util.List.of(nameCol, qtyCol, statusCol));
        lowStockTable.setPrefHeight(260);
    }
}
