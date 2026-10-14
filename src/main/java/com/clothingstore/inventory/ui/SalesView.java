package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.dao.SalesDao;
import com.clothingstore.inventory.model.DailySales;
import com.clothingstore.inventory.model.MonthlySales;
import com.clothingstore.inventory.model.SalesTotals;
import com.clothingstore.inventory.model.SoldLine;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Sales screen: total revenue / items sold, a calendar marking every date
 * something was sold (click a date to see what sold that day), and the
 * records of sale per month. Only completed orders count; cancelled ones don't.
 */
public class SalesView extends BorderPane {

    private static final String PESO = "₱%,.2f";
    private static final String[] WEEKDAYS = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

    private final SalesDao salesDao;

    private YearMonth shownMonth = YearMonth.now();
    private LocalDate selectedDate = LocalDate.now();

    private final FlowPane totalsCards = new FlowPane(16, 16);
    private final Label monthLabel = new Label();
    private final Label monthSummaryLabel = new Label();
    private final GridPane calendarGrid = new GridPane();
    private final Label dayTitle = new Label();
    private final Label daySummary = new Label();
    private final TableView<SoldLine> dayTable = new TableView<>();
    private final TableView<MonthlySales> monthTable = new TableView<>();

    public SalesView(SalesDao salesDao) {
        this.salesDao = salesDao;
        setPadding(new Insets(20));

        Label title = new Label("Sales");
        title.setFont(Font.font(null, FontWeight.BOLD, 22));
        totalsCards.setPadding(new Insets(12, 0, 12, 0));

        buildCalendarGrid();
        buildDayTable();
        buildMonthTable();

        Button prev = new Button("◀");
        prev.setOnAction(e -> { shownMonth = shownMonth.minusMonths(1); renderCalendar(); });
        Button next = new Button("▶");
        next.setOnAction(e -> { shownMonth = shownMonth.plusMonths(1); renderCalendar(); });
        Button today = new Button("Today");
        today.setOnAction(e -> {
            shownMonth = YearMonth.now();
            selectedDate = LocalDate.now();
            renderCalendar();
            loadDay();
        });
        monthLabel.setFont(Font.font(null, FontWeight.BOLD, 16));
        monthLabel.setMinWidth(160);
        monthLabel.setAlignment(Pos.CENTER);
        HBox calendarHeader = new HBox(10, prev, monthLabel, next, today);
        calendarHeader.setAlignment(Pos.CENTER_LEFT);

        VBox calendarBox = new VBox(10, calendarHeader, monthSummaryLabel, calendarGrid);
        calendarBox.setPrefWidth(620);

        dayTitle.setFont(Font.font(null, FontWeight.SEMI_BOLD, 15));
        VBox dayBox = new VBox(8, dayTitle, daySummary, dayTable);
        VBox.setVgrow(dayTable, Priority.ALWAYS);

        Label monthlyTitle = new Label("Records of Sale per Month (click a month to open it in the calendar)");
        monthlyTitle.setFont(Font.font(null, FontWeight.SEMI_BOLD, 15));
        VBox monthlyBox = new VBox(8, monthlyTitle, monthTable);

        HBox upper = new HBox(24, calendarBox, dayBox);
        HBox.setHgrow(dayBox, Priority.ALWAYS);

        VBox center = new VBox(18, upper, monthlyBox);
        ScrollPane scroll = new ScrollPane(center);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");

        setTop(new VBox(4, title, totalsCards));
        setCenter(scroll);

        refresh();
    }

    /** Reloads everything from the database -- call when the screen is shown or after a sale/cancellation. */
    public void refresh() {
        SalesTotals totals = salesDao.totals();
        totalsCards.getChildren().setAll(
                KpiCard.create("📈", "Total Revenue Earned", PESO.formatted(totals.revenue())),
                KpiCard.create("🛍️", "Total Items Sold", String.format("%,d", totals.itemsSold())),
                KpiCard.create("🧾", "Completed Orders", String.format("%,d", totals.orders()))
        );
        renderCalendar();
        loadDay();
        monthTable.setItems(FXCollections.observableArrayList(salesDao.monthlySales()));
    }

    // ---------------------------------------------------------------- calendar

    private void buildCalendarGrid() {
        calendarGrid.setHgap(4);
        calendarGrid.setVgap(4);
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7);
            calendarGrid.getColumnConstraints().add(cc);
        }
    }

    private void renderCalendar() {
        calendarGrid.getChildren().clear();
        monthLabel.setText(shownMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " " + shownMonth.getYear());

        for (int i = 0; i < 7; i++) {
            Label head = new Label(WEEKDAYS[i]);
            head.getStyleClass().add("cal-head");
            head.setMaxWidth(Double.MAX_VALUE);
            head.setAlignment(Pos.CENTER);
            calendarGrid.add(head, i, 0);
        }

        Map<LocalDate, DailySales> salesByDay = new HashMap<>();
        int monthItems = 0;
        int monthOrders = 0;
        double monthRevenue = 0;
        for (DailySales d : salesDao.dailySales(shownMonth)) {
            salesByDay.put(d.date(), d);
            monthItems += d.itemsSold();
            monthOrders += d.orders();
            monthRevenue += d.revenue();
        }
        monthSummaryLabel.setText("This month: %,d items sold · %s revenue · %d orders"
                .formatted(monthItems, PESO.formatted(monthRevenue), monthOrders));

        int offset = shownMonth.atDay(1).getDayOfWeek().getValue() % 7; // Sunday = 0
        for (int day = 1; day <= shownMonth.lengthOfMonth(); day++) {
            LocalDate date = shownMonth.atDay(day);
            int slot = offset + day - 1;
            calendarGrid.add(dayCell(date, salesByDay.get(date)), slot % 7, 1 + slot / 7);
        }
    }

    private VBox dayCell(LocalDate date, DailySales sales) {
        Label number = new Label(String.valueOf(date.getDayOfMonth()));
        number.getStyleClass().add("cal-day-number");
        VBox cell = new VBox(2, number);
        cell.getStyleClass().add("cal-day");
        cell.setMinHeight(66);
        cell.setMaxWidth(Double.MAX_VALUE);

        if (sales != null) {
            cell.getStyleClass().add("has-sales");
            Label sold = new Label(sales.itemsSold() + (sales.itemsSold() == 1 ? " item" : " items"));
            sold.getStyleClass().add("cal-day-sold");
            Label revenue = new Label(PESO.formatted(sales.revenue()));
            revenue.getStyleClass().add("cal-day-revenue");
            cell.getChildren().addAll(sold, revenue);
        }
        if (date.equals(LocalDate.now())) {
            cell.getStyleClass().add("today");
        }
        if (date.equals(selectedDate)) {
            cell.getStyleClass().add("selected");
        }
        cell.setOnMouseClicked(e -> {
            selectedDate = date;
            renderCalendar();
            loadDay();
        });
        return cell;
    }

    // --------------------------------------------------------------- day detail

    private void buildDayTable() {
        TableColumn<SoldLine, Integer> orderCol = new TableColumn<>("Order #");
        orderCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().orderId()));

        TableColumn<SoldLine, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().soldAt().format(DateTimeFormatter.ofPattern("h:mm a"))));

        TableColumn<SoldLine, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(c -> new SimpleStringProperty(nullToDash(c.getValue().code())));

        TableColumn<SoldLine, String> brandCol = new TableColumn<>("Brand");
        brandCol.setCellValueFactory(c -> new SimpleStringProperty(nullToDash(c.getValue().brand())));

        TableColumn<SoldLine, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().quantity()));

        TableColumn<SoldLine, String> totalCol = new TableColumn<>("Line Total");
        totalCol.setCellValueFactory(c -> new SimpleStringProperty(PESO.formatted(c.getValue().lineTotal())));

        dayTable.getColumns().setAll(List.of(orderCol, timeCol, codeCol, brandCol, qtyCol, totalCol));
        dayTable.setPlaceholder(new Label("Nothing was sold on this date."));
        dayTable.setPrefHeight(330);
    }

    private void loadDay() {
        List<SoldLine> lines = salesDao.linesSoldOn(selectedDate);
        dayTable.setItems(FXCollections.observableArrayList(lines));
        int items = lines.stream().mapToInt(SoldLine::quantity).sum();
        double revenue = lines.stream().mapToDouble(SoldLine::lineTotal).sum();
        dayTitle.setText("Sold on " + selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH)));
        daySummary.setText("%,d items sold · %s revenue".formatted(items, PESO.formatted(revenue)));
    }

    // ----------------------------------------------------------- monthly records

    private void buildMonthTable() {
        TableColumn<MonthlySales, String> monthCol = new TableColumn<>("Month");
        monthCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().month().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                        + " " + c.getValue().month().getYear()));
        monthCol.setPrefWidth(180);

        TableColumn<MonthlySales, Integer> ordersCol = new TableColumn<>("Orders");
        ordersCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().orders()));

        TableColumn<MonthlySales, Integer> itemsCol = new TableColumn<>("Items Sold");
        itemsCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().itemsSold()));

        TableColumn<MonthlySales, String> revenueCol = new TableColumn<>("Revenue");
        revenueCol.setCellValueFactory(c -> new SimpleStringProperty(PESO.formatted(c.getValue().revenue())));
        revenueCol.setPrefWidth(160);

        monthTable.getColumns().setAll(List.of(monthCol, ordersCol, itemsCol, revenueCol));
        monthTable.setPlaceholder(new Label("No sales recorded yet."));
        monthTable.setPrefHeight(240);
        monthTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null && !newSel.month().equals(shownMonth)) {
                shownMonth = newSel.month();
                renderCalendar();
            }
        });
    }

    private static String nullToDash(String s) {
        return s == null ? "-" : s;
    }
}
