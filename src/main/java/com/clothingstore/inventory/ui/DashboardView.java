package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.model.Item;
import com.clothingstore.inventory.model.ItemStatus;
import com.clothingstore.inventory.model.PeriodSales;
import com.clothingstore.inventory.model.StockSummary;
import com.clothingstore.inventory.service.InventoryService;
import com.clothingstore.inventory.service.SalesService;
import com.clothingstore.inventory.util.Money;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
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
        setPadding(new Insets(20));

        Label title = new Label("Dashboard");
        title.setFont(Font.font(null, FontWeight.BOLD, 22));

        inventoryCards.setPadding(new Insets(0, 0, 8, 0));
        salesCards.setPadding(new Insets(0, 0, 16, 0));

        buildLowStockTable();

        Label lowStockLabel = new Label("Needs Attention (Low / Out of Stock)");
        lowStockLabel.setFont(Font.font(null, FontWeight.SEMI_BOLD, 15));

        VBox center = new VBox(8, lowStockLabel, lowStockTable);
        VBox top = new VBox(6, title,
                sectionLabel("Inventory"), inventoryCards,
                sectionLabel("Sales"), salesCards);

        setTop(top);
        setCenter(center);

        refresh();
    }

    /** Call this whenever the screen is shown so the numbers reflect the latest sales/restocks. */
    public void refresh() {
        StockSummary summary = inventoryService.getDashboardSummary();
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

        TableColumn<Item, Number> qtyCol = new TableColumn<>("Qty Left");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<Item, ItemStatus> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new StatusBadgeCell<>());

        lowStockTable.getColumns().setAll(java.util.List.of(nameCol, qtyCol, statusCol));
        lowStockTable.setPrefHeight(260);
    }
}
