package com.clothingstore.inventory.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * The KPI tile used on the Dashboard and the Sales screen: a tinted icon chip,
 * a muted caption, the big number, and an optional detail line.
 * Tone picks the chip colour: "accent", "success", "warning" or "danger".
 */
final class KpiCard {

    private KpiCard() {}

    static HBox create(String icon, String tone, String caption, String value, String detail) {
        StackPane chip = new StackPane(Icons.styled(icon, 20, "icon-" + tone));
        chip.getStyleClass().addAll("kpi-chip", "kpi-chip-" + tone);
        chip.setMinSize(40, 40);
        chip.setMaxSize(40, 40);

        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("kpi-label");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("kpi-value");
        valueLabel.setMinWidth(Region.USE_PREF_SIZE); // never truncate amounts with "..."

        VBox text = new VBox(2, captionLabel, valueLabel);
        if (detail != null) {
            Label detailLabel = new Label(detail);
            detailLabel.getStyleClass().add("kpi-detail");
            text.getChildren().add(detailLabel);
        }

        HBox box = new HBox(14, chip, text);
        box.getStyleClass().add("kpi-card");
        box.setAlignment(Pos.CENTER_LEFT);
        // Same preferred width for every card, so a row splits its space evenly;
        // the minimum still comes from the content, so amounts never get cut off.
        box.setPrefWidth(180);
        box.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(box, Priority.ALWAYS); // cards in a row share the width evenly
        // Screen readers get one sentence instead of three fragments.
        box.setAccessibleText(caption + ": " + value + (detail != null ? ", " + detail : ""));
        return box;
    }
}
