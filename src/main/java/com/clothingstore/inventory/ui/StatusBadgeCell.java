package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.model.ItemStatus;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;

/**
 * Renders an ItemStatus as a soft coloured pill (styles.css: .status-badge
 * plus a status-specific class). The status is always written out in words,
 * so colour is never the only signal.
 */
public class StatusBadgeCell<S> extends TableCell<S, ItemStatus> {

    private final Label badge = new Label();

    public StatusBadgeCell() {
        badge.getStyleClass().add("status-badge");
        badge.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE); // never "Out of St..."
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
