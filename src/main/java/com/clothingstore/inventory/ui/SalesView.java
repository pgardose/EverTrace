package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.model.Order;
import com.clothingstore.inventory.model.PeriodSales;
import com.clothingstore.inventory.service.SalesService;
import com.clothingstore.inventory.util.Money;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Sales calendar. Month view is a calendar grid with each day's sales in its cell
 * (busier days are shaded darker); Week view is seven day cards plus a bar chart;
 * Day view is sales by hour plus that day's orders. Click any day to drill into it.
 * Only completed orders count -- cancelled orders never appear in the totals.
 */
public class SalesView extends BorderPane {

    private enum Mode { DAY, WEEK, MONTH }

    private static final String[] WEEKDAYS = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

    private final SalesService salesService;
    private Mode mode = Mode.MONTH;
    private LocalDate anchor = LocalDate.now();
    private boolean updatingPicker;

    private final ToggleGroup modeGroup = new ToggleGroup();
    private final ToggleButton dayToggle = new ToggleButton("Day");
    private final ToggleButton weekToggle = new ToggleButton("Week");
    private final ToggleButton monthToggle = new ToggleButton("Month");
    private final Label periodLabel = new Label();
    private final DatePicker datePicker = new DatePicker();
    private final HBox statTiles = new HBox(16);
    private final StackPane body = new StackPane();

    public SalesView(SalesService salesService) {
        this.salesService = salesService;
        setPadding(new Insets(28, 32, 28, 32));

        dayToggle.setToggleGroup(modeGroup);
        weekToggle.setToggleGroup(modeGroup);
        monthToggle.setToggleGroup(modeGroup);
        dayToggle.setUserData(Mode.DAY);
        weekToggle.setUserData(Mode.WEEK);
        monthToggle.setUserData(Mode.MONTH);
        monthToggle.setSelected(true);
        modeGroup.selectedToggleProperty().addListener((obs, old, selected) -> {
            if (selected == null) return;
            mode = (Mode) selected.getUserData();
            render();
        });
        // A ToggleGroup lets the selected button be clicked again to deselect it; undo that.
        for (ToggleButton t : List.of(dayToggle, weekToggle, monthToggle)) {
            t.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
                if (t.isSelected()) e.consume();
            });
        }
        HBox toggles = new HBox(2, dayToggle, weekToggle, monthToggle);
        toggles.getStyleClass().add("segmented");

        Button prev = Ui.iconButton("chevron-left", "Previous period");
        prev.setOnAction(e -> shift(-1));
        Button next = Ui.iconButton("chevron-right", "Next period");
        next.setOnAction(e -> shift(1));
        Button today = new Button("Today");
        today.setOnAction(e -> {
            anchor = LocalDate.now();
            render();
        });

        periodLabel.getStyleClass().add("card-title");
        periodLabel.setMinWidth(250);
        periodLabel.setAlignment(Pos.CENTER);

        datePicker.setEditable(false);
        datePicker.setAccessibleText("Jump to date");
        datePicker.setPrefWidth(150);
        datePicker.valueProperty().addListener((obs, old, picked) -> {
            if (!updatingPicker && picked != null) {
                anchor = picked;
                render();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox controls = new HBox(8, prev, periodLabel, next, today, spacer, datePicker, toggles);
        controls.setAlignment(Pos.CENTER_LEFT);

        statTiles.setPadding(new Insets(0, 0, 16, 0));

        VBox bodyCard = Ui.card(null, controls, body);
        ScrollPane scroll = new ScrollPane(new VBox(statTiles, bodyCard));
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");

        Label subtitle = new Label("Completed orders only. Click any day to see its orders.");
        setTop(Ui.pageHeader("Sales", subtitle));
        setCenter(scroll);

        render();
    }

    /** Call when the screen is shown so it reflects sales made or cancelled since last time. */
    public void refresh() {
        render();
    }

    private void shift(int direction) {
        anchor = switch (mode) {
            case DAY -> anchor.plusDays(direction);
            case WEEK -> anchor.plusWeeks(direction);
            case MONTH -> anchor.plusMonths(direction);
        };
        render();
    }

    private void render() {
        updatingPicker = true;
        datePicker.setValue(anchor);
        updatingPicker = false;

        PeriodSales total;
        switch (mode) {
            case DAY -> {
                total = salesService.forDay(anchor);
                periodLabel.setText(anchor.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH)));
            }
            case WEEK -> {
                total = salesService.forWeekOf(anchor);
                LocalDate start = SalesService.weekStart(anchor);
                periodLabel.setText(start.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)) + " – "
                        + start.plusDays(6).format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)));
            }
            default -> {
                total = salesService.forMonthOf(anchor);
                periodLabel.setText(anchor.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)));
            }
        }

        statTiles.getChildren().setAll(
                KpiCard.create("coins", "accent", "Total sales", Money.peso(total.total()), null),
                KpiCard.create("receipt", "accent", "Orders", String.valueOf(total.orders()), null),
                KpiCard.create("chart", "accent", "Average order", Money.peso(total.average()), null)
        );

        Node view = switch (mode) {
            case DAY -> buildDay();
            case WEEK -> buildWeek();
            case MONTH -> buildMonth();
        };
        body.getChildren().setAll(view);
    }

    // ---------------------------------------------------------------- month

    private Node buildMonth() {
        LocalDate first = SalesService.monthStart(anchor);
        LocalDate gridStart = SalesService.weekStart(first);
        int leading = (int) ChronoUnit.DAYS.between(gridStart, first);
        int weeks = (int) Math.ceil((leading + first.lengthOfMonth()) / 7.0);
        LocalDate gridEnd = gridStart.plusDays(weeks * 7L);

        Map<LocalDate, PeriodSales> daily = salesService.daily(gridStart, gridEnd);
        YearMonth shown = YearMonth.from(first);
        double max = daily.entrySet().stream()
                .filter(e -> YearMonth.from(e.getKey()).equals(shown))
                .mapToDouble(e -> e.getValue().total())
                .max().orElse(0);

        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        for (int c = 0; c < 7; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);

            Label header = new Label(WEEKDAYS[c]);
            header.getStyleClass().add("cal-header");
            header.setMaxWidth(Double.MAX_VALUE);
            header.setAlignment(Pos.CENTER);
            grid.add(header, c, 0);
        }
        for (int i = 0; i < weeks * 7; i++) {
            LocalDate date = gridStart.plusDays(i);
            boolean inMonth = YearMonth.from(date).equals(shown);
            VBox cell = dayCell(String.valueOf(date.getDayOfMonth()), date,
                    daily.getOrDefault(date, PeriodSales.ZERO), inMonth, max);
            grid.add(cell, i % 7, 1 + i / 7);
        }
        return grid;
    }

    // ----------------------------------------------------------------- week

    private Node buildWeek() {
        LocalDate start = SalesService.weekStart(anchor);
        Map<LocalDate, PeriodSales> daily = salesService.daily(start, start.plusDays(7));
        double max = daily.values().stream().mapToDouble(PeriodSales::total).max().orElse(0);

        HBox row = new HBox(8);
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = start.plusDays(i);
            PeriodSales ps = daily.getOrDefault(date, PeriodSales.ZERO);
            String header = WEEKDAYS[i] + " " + date.getDayOfMonth();
            VBox cell = dayCell(header, date, ps, true, max);
            cell.setMinHeight(110);
            HBox.setHgrow(cell, Priority.ALWAYS);
            row.getChildren().add(cell);
            labels.add(header);
            values.add(ps.total());
        }

        Label chartTitle = sectionTitle("Sales by day");
        return new VBox(16, row, chartTitle, chart(labels, values));
    }

    // ------------------------------------------------------------------ day

    private Node buildDay() {
        Map<Integer, PeriodSales> hourly = salesService.hourly(anchor);
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            labels.add(hourLabel(h));
            values.add(hourly.getOrDefault(h, PeriodSales.ZERO).total());
        }

        TableView<Order> table = new TableView<>();
        Ui.setupTable(table);
        TableColumn<Order, String> idCol = new TableColumn<>("ORDER");
        idCol.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getOrderId()));
        TableColumn<Order, String> timeCol = new TableColumn<>("TIME");
        timeCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getOrderDate().format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))));
        TableColumn<Order, String> customerCol = new TableColumn<>("CUSTOMER");
        customerCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCustomerName()));
        customerCol.setPrefWidth(200);
        TableColumn<Order, Number> totalCol = new TableColumn<>("TOTAL");
        totalCol.setCellValueFactory(c -> new javafx.beans.property.SimpleDoubleProperty(c.getValue().getTotalAmount()));
        totalCol.setCellFactory(col -> Ui.moneyCell());
        totalCol.getStyleClass().add("numeric");
        totalCol.setPrefWidth(140);
        TableColumn<Order, Order.OrderStatus> statusCol = new TableColumn<>("STATUS");
        statusCol.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getStatus()));
        statusCol.setCellFactory(col -> Ui.orderStatusCell());
        statusCol.setPrefWidth(130);
        table.getColumns().setAll(List.of(idCol, timeCol, customerCol, totalCol, statusCol));
        table.setItems(FXCollections.observableArrayList(salesService.ordersOn(anchor)));
        table.setPlaceholder(Ui.emptyState("receipt", "No sales on this day", "Orders completed on this date will show here."));
        table.setPrefHeight(240);

        return new VBox(12, sectionTitle("Sales by hour"), chart(labels, values),
                sectionTitle("Orders"), table);
    }

    // -------------------------------------------------------------- helpers

    /** One calendar cell (used by the month grid and the week row). Clicking it opens that day. */
    private VBox dayCell(String header, LocalDate date, PeriodSales sales, boolean inMonth, double max) {
        Label head = new Label(header);
        head.getStyleClass().add("cal-day-number");
        VBox cell = new VBox(4, head);
        cell.getStyleClass().add("cal-cell");

        if (sales.orders() > 0) {
            Label amount = new Label(Money.peso(sales.total()));
            amount.getStyleClass().add("cal-amount");
            Label orders = new Label(sales.orders() + (sales.orders() == 1 ? " order" : " orders"));
            orders.getStyleClass().add("cal-orders");
            cell.getChildren().addAll(amount, orders);
        }
        if (date.equals(LocalDate.now())) cell.getStyleClass().add("cal-cell-today");
        if (!inMonth) cell.getStyleClass().add("cal-cell-outside");
        if (max > 0 && sales.total() > 0) {
            // Busier days get a stronger accent tint; capped so the text stays readable.
            double alpha = 0.06 + 0.26 * (sales.total() / max);
            cell.setStyle("-fx-background-color: rgba(79, 70, 229, " + String.format(Locale.ROOT, "%.2f", alpha) + ");");
        }
        cell.setMinHeight(84);
        cell.setMaxWidth(Double.MAX_VALUE);
        Runnable open = () -> {
            anchor = date;
            if (dayToggle.isSelected()) render();
            else dayToggle.setSelected(true); // the toggle listener switches to Day mode and re-renders
        };
        cell.setOnMouseClicked(e -> open.run());
        // Keyboard: Tab to a day, Enter/Space opens it.
        cell.setFocusTraversable(true);
        cell.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER || e.getCode() == javafx.scene.input.KeyCode.SPACE) {
                e.consume();
                open.run();
            }
        });
        cell.setAccessibleRole(javafx.scene.AccessibleRole.BUTTON);
        cell.setAccessibleText(date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)) + ": "
                + (sales.orders() == 0 ? "no sales" : Money.peso(sales.total()) + ", " + sales.orders()
                + (sales.orders() == 1 ? " order" : " orders")));
        return cell;
    }

    private BarChart<String, Number> chart(List<String> labels, List<Double> values) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Sales (₱)");
        xAxis.setAnimated(false);
        yAxis.setAnimated(false);

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCategoryGap(6);
        chart.setMinHeight(220);
        chart.setPrefHeight(260);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int i = 0; i < labels.size(); i++) {
            series.getData().add(new XYChart.Data<>(labels.get(i), values.get(i)));
        }
        chart.getData().add(series);
        return chart;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("card-title");
        return label;
    }

    private static String hourLabel(int hour) {
        if (hour == 0) return "12AM";
        if (hour < 12) return hour + "AM";
        if (hour == 12) return "12PM";
        return (hour - 12) + "PM";
    }
}
