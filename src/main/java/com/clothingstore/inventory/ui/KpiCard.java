package com.clothingstore.inventory.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** A single KPI card: icon glyph, big value, small caption -- styled by styles.css .kpi-card. */
public final class KpiCard {

    private KpiCard() {}

    public static VBox create(String iconGlyph, String caption, String value) {
        Label iconLabel = new Label(iconGlyph);
        iconLabel.getStyleClass().add("kpi-icon");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("kpi-value");

        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("kpi-label");

        HBox topRow = new HBox(10, iconLabel, valueLabel);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(6, topRow, captionLabel);
        box.getStyleClass().add("kpi-card");
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPrefWidth(230);
        return box;
    }
}
