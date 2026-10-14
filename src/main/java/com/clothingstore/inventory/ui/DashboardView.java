package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.dao.SalesDao;
import com.clothingstore.inventory.model.Item;
import com.clothingstore.inventory.model.ItemStatus;
import com.clothingstore.inventory.model.SalesTotals;
import com.clothingstore.inventory.model.StockSummary;
import com.clothingstore.inventory.service.InventoryService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class DashboardView extends BorderPane {

    private final InventoryService inventoryService;
    private final SalesDao salesDao;
    private final FlowPane summaryCards = new FlowPane(16, 16);
    private final TableView<Item> lowStockTable = new TableView<>();

    public DashboardView(InventoryService inventoryService, SalesDao salesDao) {
        this.inventoryService = inventoryService;
        this.salesDao = salesDao;
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
        SalesTotals sales = salesDao.totals();
        summaryCards.getChildren().setAll(
                KpiCard.create("📦", "Total Items in stock (" + summary.productCount() + " products)",
                        String.format("%,d", summary.totalItems())),
                KpiCard.create("💰", "Inventory Value",
                        "₱%,.2f".formatted(summary.totalInventoryValue())),
                KpiCard.create("⚠️", "Low Stock", String.valueOf(summary.lowStockCount())),
                KpiCard.create("❌", "Out of Stock", String.valueOf(summary.outOfStockCount())),
                KpiCard.create("📈", "Total Revenue Earned",
                        "₱%,.2f".formatted(sales.revenue())),
                KpiCard.create("🛍️", "Total Items Sold (" + sales.orders() + " orders)",
                        String.format("%,d", sales.itemsSold()))
        );
        lowStockTable.setItems(FXCollections.observableArrayList(inventoryService.getLowStockItems()));
    }

    private void buildLowStockTable() {
        TableColumn<Item, String> photoCol = new TableColumn<>("Photo");
        photoCol.setCellValueFactory(new PropertyValueFactory<>("imagePath"));
        photoCol.setCellFactory(col -> new ThumbnailCell<>());
        photoCol.setSortable(false);
        photoCol.setPrefWidth(70);

        TableColumn<Item, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(new PropertyValueFactory<>("sku"));

        TableColumn<Item, String> brandCol = new TableColumn<>("Brand");
        brandCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Item, Number> qtyCol = new TableColumn<>("Qty Left");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<Item, ItemStatus> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new StatusBadgeCell<>());

        lowStockTable.getColumns().setAll(java.util.List.of(photoCol, codeCol, brandCol, qtyCol, statusCol));
        lowStockTable.setPrefHeight(300);
        lowStockTable.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Item> row = new javafx.scene.control.TableRow<>();
            row.setPrefHeight(56);
            return row;
        });
    }
}
