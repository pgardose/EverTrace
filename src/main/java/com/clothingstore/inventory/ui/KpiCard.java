package com.clothingstore.inventory.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** The rounded KPI tile used on the Dashboard and the Sales screen (styled by styles.css .kpi-card). */
final class KpiCard {

    private KpiCard() {}

    static VBox create(String iconGlyph, String caption, String value) {
        Label iconLabel = new Label(iconGlyph);
        iconLabel.getStyleClass().add("kpi-icon");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("kpi-value");
        valueLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE); // never truncate amounts with "..."

        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("kpi-label");

        HBox topRow = new HBox(8, iconLabel, valueLabel);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(6, topRow, captionLabel);
        box.getStyleClass().add("kpi-card");
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMinWidth(200);
        return box;
    }
}
