package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.util.ImageStorage;
import javafx.scene.Cursor;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;

/**
 * Renders a thumbnail (or nothing, if there is no photo) in a table cell.
 * Clicking the thumbnail opens the photo full size (see PhotoViewer).
 */
public class ThumbnailCell<S> extends TableCell<S, String> {

    private static final double SIZE = 48;

    private final ImageView imageView = new ImageView();

    public ThumbnailCell() {
        imageView.setFitWidth(SIZE);
        imageView.setFitHeight(SIZE);
        imageView.setPreserveRatio(true);
        imageView.setCursor(Cursor.HAND);
        Tooltip.install(imageView, new Tooltip("Click to view photo"));
        imageView.setOnMouseClicked(e -> PhotoViewer.show(getItem(), "Photo"));
    }

    @Override
    protected void updateItem(String imagePath, boolean empty) {
        super.updateItem(imagePath, empty);
        if (empty || imagePath == null || imagePath.isBlank()) {
            setGraphic(null);
            return;
        }
        File file = ImageStorage.resolve(imagePath).toFile();
        if (file.exists()) {
            imageView.setImage(new Image(file.toURI().toString(), SIZE, SIZE, true, true));
            setGraphic(imageView);
        } else {
            setGraphic(null);
        }
    }
}
