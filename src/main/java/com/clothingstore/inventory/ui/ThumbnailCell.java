package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.util.ImageStorage;
import javafx.scene.Cursor;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import java.io.File;

/**
 * Renders a rounded thumbnail in a table cell. Items without a photo get a
 * neutral placeholder tile so rows stay aligned; clicking a real photo opens
 * it large in {@link PhotoViewer}.
 */
public class ThumbnailCell<S> extends TableCell<S, String> {

    private final double size;
    private final ImageView imageView = new ImageView();
    private final StackPane frame = new StackPane();
    private final StackPane placeholder = new StackPane(Icons.styled("image", 18, "icon-subtle"));
    private final Tooltip tooltip = new Tooltip("Click to enlarge");
    private String currentPath;

    public ThumbnailCell() {
        this(40);
    }

    public ThumbnailCell(double size) {
        this.size = size;
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        imageView.setPreserveRatio(true);
        Rectangle clip = new Rectangle(size, size);
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        frame.setClip(clip);
        frame.getChildren().add(imageView);
        frame.getStyleClass().add("thumb");
        frame.setMinSize(size, size);
        frame.setMaxSize(size, size);
        placeholder.getStyleClass().add("thumb-placeholder");
        placeholder.setMinSize(size, size);
        placeholder.setMaxSize(size, size);

        frame.setOnMouseClicked(e -> {
            Object row = getTableRow() != null ? getTableRow().getItem() : null;
            String title = row instanceof com.clothingstore.inventory.model.Item item ? item.getName() : null;
            PhotoViewer.show(currentPath, title);
            e.consume();
        });
        frame.setCursor(Cursor.HAND);
    }

    @Override
    protected void updateItem(String imagePath, boolean empty) {
        super.updateItem(imagePath, empty);
        currentPath = null;
        setTooltip(null);
        if (empty) {
            setGraphic(null);
            return;
        }
        if (imagePath != null && !imagePath.isBlank()) {
            File file = ImageStorage.resolve(imagePath).toFile();
            if (file.exists()) {
                currentPath = imagePath;
                imageView.setImage(new Image(file.toURI().toString(), size * 2, size * 2, true, true, true));
                setGraphic(frame);
                setTooltip(tooltip);
                return;
            }
        }
        setGraphic(placeholder);
    }
}
