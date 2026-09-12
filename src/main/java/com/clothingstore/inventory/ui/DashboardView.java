package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.model.Item;
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
import javafx.scene.paint.Color;
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
                card("Total Items", String.valueOf(summary.totalItems()), Color.web("#2563eb")),
                card("Low Stock", String.valueOf(summary.lowStockCount()), Color.web("#d97706")),
                card("Out of Stock", String.valueOf(summary.outOfStockCount()), Color.web("#dc2626")),
                card("Inventory Value", "₱%,.2f".formatted(summary.totalInventoryValue()), Color.web("#16a34a"))
        );
        lowStockTable.setItems(FXCollections.observableArrayList(inventoryService.getLowStockItems()));
    }

    private VBox card(String label, String value, Color accent) {
        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font(null, FontWeight.BOLD, 20));
        valueLabel.setTextFill(accent);

        Label captionLabel = new Label(label);
        captionLabel.setFont(Font.font(12));

        VBox box = new VBox(6, valueLabel, captionLabel);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(14));
        box.setPrefWidth(180);
        box.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        return box;
    }

    private void buildLowStockTable() {
        TableColumn<Item, String> nameCol = new TableColumn<>("Item");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Item, Number> qtyCol = new TableColumn<>("Qty Left");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<Item, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().getStatus().displayName()));

        lowStockTable.getColumns().setAll(java.util.List.of(nameCol, qtyCol, statusCol));
        lowStockTable.setPrefHeight(260);
    }
}
