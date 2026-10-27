package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.util.ImageStorage;
<<<<<<< HEAD
import javafx.scene.control.TableCell;
=======
import javafx.scene.Cursor;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
>>>>>>> 1fad3874ce4da0e34b3cc67029bd87899e5d4823
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;

<<<<<<< HEAD
/** Renders a small thumbnail (or nothing, if the item has no photo) in a table cell. */
public class ThumbnailCell<S> extends TableCell<S, String> {

    private final ImageView imageView = new ImageView();

    public ThumbnailCell() {
        imageView.setFitWidth(40);
        imageView.setFitHeight(40);
        imageView.setPreserveRatio(true);
=======
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
>>>>>>> 1fad3874ce4da0e34b3cc67029bd87899e5d4823
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
<<<<<<< HEAD
            imageView.setImage(new Image(file.toURI().toString(), 40, 40, true, true));
=======
            imageView.setImage(new Image(file.toURI().toString(), SIZE, SIZE, true, true));
>>>>>>> 1fad3874ce4da0e34b3cc67029bd87899e5d4823
            setGraphic(imageView);
        } else {
            setGraphic(null);
        }
    }
}
