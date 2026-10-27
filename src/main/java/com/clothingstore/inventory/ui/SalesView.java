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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

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
        setPadding(new Insets(20));

        Label title = new Label("Sales");
        title.setFont(Font.font(null, FontWeight.BOLD, 22));

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
        HBox toggles = new HBox(0, dayToggle, weekToggle, monthToggle);

        Button prev = new Button("◀");
        prev.setOnAction(e -> shift(-1));
        Button next = new Button("▶");
        next.setOnAction(e -> shift(1));
        Button today = new Button("Today");
        today.setOnAction(e -> {
            anchor = LocalDate.now();
            render();
        });

        periodLabel.setFont(Font.font(null, FontWeight.BOLD, 16));
        periodLabel.setMinWidth(270);
        periodLabel.setAlignment(Pos.CENTER);

        datePicker.setEditable(false);
        datePicker.valueProperty().addListener((obs, old, picked) -> {
            if (!updatingPicker && picked != null) {
                anchor = picked;
                render();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox controls = new HBox(10, toggles, spacer, prev, periodLabel, next, today, datePicker);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(12, 0, 12, 0));

        statTiles.setPadding(new Insets(0, 0, 16, 0));

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");

        setTop(new VBox(4, title, controls, statTiles));
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
                KpiCard.create("💰", "Total sales", Money.peso(total.total())),
                KpiCard.create("🧾", "Orders", String.valueOf(total.orders())),
                KpiCard.create("📊", "Average order", Money.peso(total.average()))
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
        TableColumn<Order, String> idCol = new TableColumn<>("Order");
        idCol.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getOrderId()));
        TableColumn<Order, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getOrderDate().format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))));
        TableColumn<Order, String> customerCol = new TableColumn<>("Customer");
        customerCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCustomerName()));
        customerCol.setPrefWidth(200);
        TableColumn<Order, String> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(c -> new SimpleStringProperty(Money.peso(c.getValue().getTotalAmount())));
        totalCol.setPrefWidth(140);
        TableColumn<Order, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        statusCol.setPrefWidth(130);
        table.getColumns().setAll(List.of(idCol, timeCol, customerCol, totalCol, statusCol));
        table.setItems(FXCollections.observableArrayList(salesService.ordersOn(anchor)));
        table.setPlaceholder(new Label("No sales on this day."));
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
            // Busier days get a stronger blue tint; capped so the text stays readable.
            double alpha = 0.08 + 0.27 * (sales.total() / max);
            cell.setStyle("-fx-background-color: rgba(37, 99, 235, " + String.format(Locale.ROOT, "%.2f", alpha) + ");");
        }
        cell.setMinHeight(84);
        cell.setMaxWidth(Double.MAX_VALUE);
        cell.setOnMouseClicked(e -> {
            anchor = date;
            dayToggle.setSelected(true); // the toggle listener switches to Day mode and re-renders
        });
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
        label.setFont(Font.font(null, FontWeight.SEMI_BOLD, 15));
        return label;
    }

    private static String hourLabel(int hour) {
        if (hour == 0) return "12AM";
        if (hour < 12) return hour + "AM";
        if (hour == 12) return "12PM";
        return (hour - 12) + "PM";
    }
}
