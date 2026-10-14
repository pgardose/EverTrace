package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.model.ItemStatus;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;

/**
 * Renders an ItemStatus as a colored pill badge (styles.css: .status-badge
 * plus a status-specific class) instead of plain text. This is the JavaFX
 * equivalent of a Swing custom TableCellRenderer.
 */
public class StatusBadgeCell<S> extends TableCell<S, ItemStatus> {

    private final Label badge = new Label();

    public StatusBadgeCell() {
        badge.getStyleClass().add("status-badge");
    }

    @Override
    protected void updateItem(ItemStatus status, boolean empty) {
        super.updateItem(status, empty);
        if (empty || status == null) {
            setGraphic(null);
            return;
        }

        badge.setText(status.displayName());
        badge.getStyleClass().removeAll("available", "low-stock", "out-of-stock", "discontinued");
        badge.getStyleClass().add(switch (status) {
            case AVAILABLE -> "available";
            case LOW_STOCK -> "low-stock";
            case OUT_OF_STOCK -> "out-of-stock";
            case DISCONTINUED -> "discontinued";
        });
        setGraphic(badge);
    }
}
