package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.model.Order;
import com.clothingstore.inventory.util.Money;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Shared building blocks so every screen has the same header, cards, table
 * formatting and empty states. Visual rules live in styles.css.
 */
final class Ui {

    private Ui() {}

    static final String STYLESHEET =
            Objects.requireNonNull(Ui.class.getResource("/styles.css")).toExternalForm();

    // ------------------------------------------------------------ layout

    /** Page title + muted subtitle on the left, actions on the right. */
    static HBox pageHeader(String title, Label subtitle, Node... actions) {
        Label t = new Label(title);
        t.getStyleClass().add("page-title");
        VBox text = new VBox(2, t);
        if (subtitle != null) {
            subtitle.getStyleClass().add("page-subtitle");
            text.getChildren().add(subtitle);
        }
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(10, text, spacer);
        row.getChildren().addAll(actions);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("page-header");
        return row;
    }

    /** White rounded surface with an optional heading. */
    static VBox card(String heading, Node... content) {
        VBox box = new VBox(12);
        box.getStyleClass().add("card");
        if (heading != null) {
            Label h = new Label(heading);
            h.getStyleClass().add("card-title");
            box.getChildren().add(h);
        }
        box.getChildren().addAll(content);
        return box;
    }

    static Label sectionLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("section-label");
        return l;
    }

    static Label muted(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("muted");
        return l;
    }

    /** A label shown above a form control and announced with it by screen readers. */
    static VBox field(String label, Control control) {
        Label l = new Label(label);
        l.getStyleClass().add("field-label");
        l.setLabelFor(control);
        if (control.getAccessibleText() == null) control.setAccessibleText(label);
        control.setMaxWidth(Double.MAX_VALUE);
        return new VBox(5, l, control);
    }

    // ------------------------------------------------------------ buttons

    static Button button(String text, String icon, String... styleClasses) {
        Button b = new Button(text);
        if (icon != null) Icons.on(b, icon, 15);
        b.getStyleClass().addAll(styleClasses);
        return b;
    }

    /** Icon-only button; the tooltip doubles as its accessible name. */
    static Button iconButton(String icon, String accessibleName) {
        Button b = new Button();
        Icons.on(b, icon, 16);
        b.getStyleClass().addAll("button-icon");
        b.setAccessibleText(accessibleName);
        b.setTooltip(new Tooltip(accessibleName));
        return b;
    }

    // ------------------------------------------------------------ empty states

    static Node emptyState(String icon, String title, String message) {
        Label t = new Label(title);
        t.getStyleClass().add("empty-title");
        Label m = new Label(message);
        m.getStyleClass().add("empty-message");
        m.setWrapText(true);
        m.setMaxWidth(320);
        m.setAlignment(Pos.CENTER);
        m.setStyle("-fx-text-alignment: center;");
        StackPane chip = new StackPane(Icons.styled(icon, 22, "icon-muted"));
        chip.getStyleClass().add("empty-icon");
        VBox box = new VBox(8, chip, t, m);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(24));
        return box;
    }

    // ------------------------------------------------------------ tables

    static <S> void setupTable(TableView<S> table) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getStyleClass().add("data-table");
    }

    /** Money column: "₱1,299.00", right-aligned so the decimals line up. */
    static <S> TableCell<S, Number> moneyCell() {
        TableCell<S, Number> cell = new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : Money.peso(value.doubleValue()));
            }
        };
        cell.getStyleClass().add("numeric-cell");
        return cell;
    }

    static <S, T> TableCell<S, T> numericCell() {
        TableCell<S, T> cell = new TableCell<>() {
            @Override
            protected void updateItem(T value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : String.valueOf(value));
            }
        };
        cell.getStyleClass().add("numeric-cell");
        return cell;
    }

    /** Bold primary text with a muted second line (e.g. item name + SKU). */
    static <S> TableCell<S, S> twoLineCell(java.util.function.Function<S, String> primary,
                                           java.util.function.Function<S, String> secondary) {
        return new TableCell<>() {
            private final Label top = new Label();
            private final Label bottom = new Label();
            private final VBox box = new VBox(1, top, bottom);
            {
                top.getStyleClass().add("cell-primary");
                bottom.getStyleClass().add("cell-secondary");
                box.setAlignment(Pos.CENTER_LEFT);
            }
            @Override
            protected void updateItem(S row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setGraphic(null);
                    return;
                }
                top.setText(primary.apply(row));
                String sub = secondary.apply(row);
                bottom.setText(sub);
                bottom.setVisible(sub != null && !sub.isBlank());
                bottom.setManaged(bottom.isVisible());
                setGraphic(box);
            }
        };
    }

    /** Order status as a soft pill, matching the stock-status badges. */
    static <S> TableCell<S, Order.OrderStatus> orderStatusCell() {
        return new TableCell<>() {
            private final Label badge = new Label();
            {
                badge.getStyleClass().add("status-badge");
                badge.setMinWidth(Region.USE_PREF_SIZE);
            }
            @Override
            protected void updateItem(Order.OrderStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    return;
                }
                applyOrderStatus(badge, status);
                setGraphic(badge);
            }
        };
    }

    static void applyOrderStatus(Label badge, Order.OrderStatus status) {
        badge.getStyleClass().removeAll("available", "low-stock", "discontinued");
        switch (status) {
            case COMPLETED -> { badge.setText("Completed"); badge.getStyleClass().add("available"); }
            case PENDING -> { badge.setText("Pending"); badge.getStyleClass().add("low-stock"); }
            case CANCELLED -> { badge.setText("Cancelled"); badge.getStyleClass().add("discontinued"); }
        }
    }

    // ------------------------------------------------------------ dialogs

    /** Gives a dialog the app stylesheet so custom classes (cards, fields, badges) apply. */
    static void style(Dialog<?> dialog) {
        dialog.getDialogPane().getStylesheets().add(STYLESHEET);
        dialog.getDialogPane().getStyleClass().add("app-dialog");
    }

    static Alert alert(Alert.AlertType type, String message) {
        Alert a = new Alert(type, message);
        a.setHeaderText(null);
        style(a);
        return a;
    }
}
