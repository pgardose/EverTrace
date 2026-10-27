package com.clothingstore.inventory.ui;

<<<<<<< HEAD
import com.clothingstore.inventory.model.Item;
import com.clothingstore.inventory.model.ItemStatus;
import com.clothingstore.inventory.model.PeriodSales;
import com.clothingstore.inventory.model.StockSummary;
import com.clothingstore.inventory.service.InventoryService;
import com.clothingstore.inventory.service.SalesService;
import com.clothingstore.inventory.util.Money;
=======
import com.clothingstore.inventory.dao.SalesDao;
import com.clothingstore.inventory.model.Item;
import com.clothingstore.inventory.model.ItemStatus;
import com.clothingstore.inventory.model.SalesTotals;
import com.clothingstore.inventory.model.StockSummary;
import com.clothingstore.inventory.service.InventoryService;
>>>>>>> 1fad3874ce4da0e34b3cc67029bd87899e5d4823
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
<<<<<<< HEAD
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;

public class DashboardView extends BorderPane {

    private final InventoryService inventoryService;
    private final SalesService salesService;
    private final HBox inventoryCards = new HBox(16);
    private final HBox salesCards = new HBox(16);
    private final TableView<Item> lowStockTable = new TableView<>();

    public DashboardView(InventoryService inventoryService, SalesService salesService) {
        this.inventoryService = inventoryService;
        this.salesService = salesService;
=======
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
>>>>>>> 1fad3874ce4da0e34b3cc67029bd87899e5d4823
        setPadding(new Insets(20));

        Label title = new Label("Dashboard");
        title.setFont(Font.font(null, FontWeight.BOLD, 22));

<<<<<<< HEAD
        inventoryCards.setPadding(new Insets(0, 0, 8, 0));
        salesCards.setPadding(new Insets(0, 0, 16, 0));
=======
        summaryCards.setPadding(new Insets(16, 0, 16, 0));
>>>>>>> 1fad3874ce4da0e34b3cc67029bd87899e5d4823

        buildLowStockTable();

        Label lowStockLabel = new Label("Needs Attention (Low / Out of Stock)");
        lowStockLabel.setFont(Font.font(null, FontWeight.SEMI_BOLD, 15));

        VBox center = new VBox(8, lowStockLabel, lowStockTable);
<<<<<<< HEAD
        VBox top = new VBox(6, title,
                sectionLabel("Inventory"), inventoryCards,
                sectionLabel("Sales"), salesCards);
=======
        VBox top = new VBox(4, title, summaryCards);
>>>>>>> 1fad3874ce4da0e34b3cc67029bd87899e5d4823

        setTop(top);
        setCenter(center);

        refresh();
    }

    /** Call this whenever the screen is shown so the numbers reflect the latest sales/restocks. */
    public void refresh() {
        StockSummary summary = inventoryService.getDashboardSummary();
<<<<<<< HEAD
        inventoryCards.getChildren().setAll(
                KpiCard.create("📦", "Total Items", String.valueOf(summary.totalItems())),
                KpiCard.create("⚠️", "Low Stock", String.valueOf(summary.lowStockCount())),
                KpiCard.create("❌", "Out of Stock", String.valueOf(summary.outOfStockCount())),
                KpiCard.create("💰", "Inventory Value", Money.peso(summary.totalInventoryValue()))
        );

        LocalDate today = LocalDate.now();
        PeriodSales day = salesService.forDay(today);
        PeriodSales week = salesService.forWeekOf(today);
        PeriodSales month = salesService.forMonthOf(today);
        salesCards.getChildren().setAll(
                KpiCard.create("🛒", "Sales today · " + orderCount(day), Money.peso(day.total())),
                KpiCard.create("📅", "This week · " + orderCount(week), Money.peso(week.total())),
                KpiCard.create("📈", "This month · " + orderCount(month), Money.peso(month.total()))
        );

        lowStockTable.setItems(FXCollections.observableArrayList(inventoryService.getLowStockItems()));
    }

    private static String orderCount(PeriodSales sales) {
        return sales.orders() + (sales.orders() == 1 ? " order" : " orders");
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font(null, FontWeight.SEMI_BOLD, 13));
        label.setOpacity(0.7);
        return label;
    }

    private void buildLowStockTable() {
        TableColumn<Item, String> nameCol = new TableColumn<>("Item");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
=======
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
>>>>>>> 1fad3874ce4da0e34b3cc67029bd87899e5d4823

        TableColumn<Item, Number> qtyCol = new TableColumn<>("Qty Left");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<Item, ItemStatus> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new StatusBadgeCell<>());

<<<<<<< HEAD
        lowStockTable.getColumns().setAll(java.util.List.of(nameCol, qtyCol, statusCol));
        lowStockTable.setPrefHeight(260);
=======
        lowStockTable.getColumns().setAll(java.util.List.of(photoCol, codeCol, brandCol, qtyCol, statusCol));
        lowStockTable.setPrefHeight(300);
        lowStockTable.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Item> row = new javafx.scene.control.TableRow<>();
            row.setPrefHeight(56);
            return row;
        });
>>>>>>> 1fad3874ce4da0e34b3cc67029bd87899e5d4823
    }
}
