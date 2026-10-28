package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.model.Item;
import com.clothingstore.inventory.model.ItemStatus;
import com.clothingstore.inventory.model.PeriodSales;
import com.clothingstore.inventory.model.StockSummary;
import com.clothingstore.inventory.service.InventoryService;
import com.clothingstore.inventory.service.SalesService;
import com.clothingstore.inventory.util.Money;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class DashboardView extends BorderPane {

    private final InventoryService inventoryService;
    private final SalesService salesService;
    private final HBox inventoryCards = new HBox(14);
    private final HBox salesCards = new HBox(14);
    private final TableView<Item> lowStockTable = new TableView<>();
    private final Label subtitle = new Label();
    private final Label attentionCount = new Label();

    public DashboardView(InventoryService inventoryService, SalesService salesService) {
        this.inventoryService = inventoryService;
        this.salesService = salesService;
        setPadding(new Insets(28, 32, 28, 32));

        buildLowStockTable();

        attentionCount.getStyleClass().add("muted");
        VBox attentionCard = Ui.card(null,
                new VBox(2, cardTitle("Needs attention"), attentionCount),
                lowStockTable);
        VBox.setVgrow(lowStockTable, Priority.ALWAYS);

        VBox top = new VBox(8,
                Ui.pageHeader("Dashboard", subtitle),
                Ui.sectionLabel("SALES"), salesCards,
                spacer(8),
                Ui.sectionLabel("INVENTORY"), inventoryCards,
                spacer(14));

        setTop(top);
        setCenter(attentionCard);

        refresh();
    }

    /** Call this whenever the screen is shown so the numbers reflect the latest sales/restocks. */
    public void refresh() {
        LocalDate today = LocalDate.now();
        subtitle.setText(greeting() + " — " + today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)));

        StockSummary summary = inventoryService.getDashboardSummary();
        inventoryCards.getChildren().setAll(
                KpiCard.create("box", "accent", "Items in catalog", String.valueOf(summary.totalItems()), null),
                KpiCard.create("alert", "warning", "Low stock", String.valueOf(summary.lowStockCount()),
                        summary.lowStockCount() == 0 ? "All good" : "At or below reorder level"),
                KpiCard.create("x-circle", "danger", "Out of stock", String.valueOf(summary.outOfStockCount()),
                        summary.outOfStockCount() == 0 ? "All good" : "Can't be sold right now"),
                KpiCard.create("tag", "success", "Inventory value", Money.peso(summary.totalInventoryValue()),
                        "At selling price")
        );

        PeriodSales day = salesService.forDay(today);
        PeriodSales week = salesService.forWeekOf(today);
        PeriodSales month = salesService.forMonthOf(today);
        salesCards.getChildren().setAll(
                KpiCard.create("coins", "accent", "Sales today", Money.peso(day.total()), orderCount(day)),
                KpiCard.create("calendar", "accent", "This week", Money.peso(week.total()), orderCount(week)),
                KpiCard.create("trend", "accent", "This month", Money.peso(month.total()), orderCount(month))
        );

        List<Item> low = inventoryService.getLowStockItems();
        lowStockTable.setItems(FXCollections.observableArrayList(low));
        attentionCount.setText(low.isEmpty()
                ? "Nothing to restock right now."
                : low.size() + (low.size() == 1 ? " item is" : " items are") + " low or out of stock. Restock from the Inventory screen.");
    }

    private static String greeting() {
        int h = LocalTime.now().getHour();
        return h < 12 ? "Good morning" : h < 18 ? "Good afternoon" : "Good evening";
    }

    private static String orderCount(PeriodSales sales) {
        return sales.orders() + (sales.orders() == 1 ? " order" : " orders");
    }

    private static Label cardTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("card-title");
        return l;
    }

    private static javafx.scene.layout.Region spacer(double h) {
        javafx.scene.layout.Region r = new javafx.scene.layout.Region();
        r.setMinHeight(h);
        return r;
    }

    private void buildLowStockTable() {
        Ui.setupTable(lowStockTable);

        TableColumn<Item, String> photoCol = new TableColumn<>("");
        photoCol.setCellValueFactory(new PropertyValueFactory<>("imagePath"));
        photoCol.setCellFactory(col -> new ThumbnailCell<>(32));
        photoCol.setSortable(false);
        photoCol.setMinWidth(56);
        photoCol.setMaxWidth(56);

        TableColumn<Item, Item> nameCol = new TableColumn<>("ITEM");
        nameCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        nameCol.setCellFactory(col -> Ui.twoLineCell(Item::getName, Item::getSku));
        nameCol.setComparator(java.util.Comparator.comparing(Item::getName, String.CASE_INSENSITIVE_ORDER));

        TableColumn<Item, String> categoryCol = new TableColumn<>("CATEGORY");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("categoryName"));

        TableColumn<Item, Number> qtyCol = new TableColumn<>("QTY LEFT");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setCellFactory(col -> Ui.numericCell());
        qtyCol.getStyleClass().add("numeric");

        TableColumn<Item, Number> thresholdCol = new TableColumn<>("REORDER AT");
        thresholdCol.setCellValueFactory(new PropertyValueFactory<>("reorderThreshold"));
        thresholdCol.setCellFactory(col -> Ui.numericCell());
        thresholdCol.getStyleClass().add("numeric");

        TableColumn<Item, ItemStatus> statusCol = new TableColumn<>("STATUS");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new StatusBadgeCell<>());

        lowStockTable.getColumns().setAll(List.of(photoCol, nameCol, categoryCol, qtyCol, thresholdCol, statusCol));
        lowStockTable.setRowFactory(tv -> {
            TableRow<Item> row = new TableRow<>();
            row.setPrefHeight(48);
            return row;
        });
        lowStockTable.setPlaceholder(Ui.emptyState("check-circle", "Everything is in stock",
                "Items that drop to their reorder level will show up here."));
        lowStockTable.setMinHeight(180);
    }
}
