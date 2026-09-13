package com.clothingstore.inventory.util;

import com.clothingstore.inventory.util.ImageStorage;
import javafx.scene.control.TableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;

/** Renders a small thumbnail (or nothing, if the item has no photo) in a table cell. */
public class ThumbnailCell<S> extends TableCell<S, String> {

    private final ImageView imageView = new ImageView();

    public ThumbnailCell() {
        imageView.setFitWidth(40);
        imageView.setFitHeight(40);
        imageView.setPreserveRatio(true);
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
            imageView.setImage(new Image(file.toURI().toString(), 40, 40, true, true));
            setGraphic(imageView);
        } else {
            setGraphic(null);
        }
    }
}
