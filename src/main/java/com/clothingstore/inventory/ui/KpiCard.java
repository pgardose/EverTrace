package com.clothingstore.inventory.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

<<<<<<< HEAD
/** The rounded KPI tile used on the Dashboard and the Sales screen (styled by styles.css .kpi-card). */
final class KpiCard {

    private KpiCard() {}

    static VBox create(String iconGlyph, String caption, String value) {
=======
/** A single KPI card: icon glyph, big value, small caption -- styled by styles.css .kpi-card. */
public final class KpiCard {

    private KpiCard() {}

    public static VBox create(String iconGlyph, String caption, String value) {
>>>>>>> 1fad3874ce4da0e34b3cc67029bd87899e5d4823
        Label iconLabel = new Label(iconGlyph);
        iconLabel.getStyleClass().add("kpi-icon");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("kpi-value");
<<<<<<< HEAD
        valueLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE); // never truncate amounts with "..."
=======
>>>>>>> 1fad3874ce4da0e34b3cc67029bd87899e5d4823

        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("kpi-label");

<<<<<<< HEAD
        HBox topRow = new HBox(8, iconLabel, valueLabel);
=======
        HBox topRow = new HBox(10, iconLabel, valueLabel);
>>>>>>> 1fad3874ce4da0e34b3cc67029bd87899e5d4823
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(6, topRow, captionLabel);
        box.getStyleClass().add("kpi-card");
        box.setAlignment(Pos.CENTER_LEFT);
<<<<<<< HEAD
        box.setMinWidth(200);
=======
        box.setPrefWidth(230);
>>>>>>> 1fad3874ce4da0e34b3cc67029bd87899e5d4823
        return box;
    }
}
