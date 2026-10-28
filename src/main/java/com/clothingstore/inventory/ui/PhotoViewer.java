package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.util.ImageStorage;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;

/** Opens an item's photo at a large size. Used by every thumbnail in the app (click to enlarge). */
public final class PhotoViewer {

    private static final double MAX_SIZE = 640;

    private PhotoViewer() {}

    public static void show(String imagePath, String title) {
        if (imagePath == null || imagePath.isBlank()) return;
        File file = ImageStorage.resolve(imagePath).toFile();
        if (!file.exists()) return;

        ImageView view = new ImageView(new Image(file.toURI().toString(), MAX_SIZE, MAX_SIZE, true, true));
        view.setPreserveRatio(true);

        Dialog<Void> dialog = new Dialog<>();
        Ui.style(dialog);
        dialog.setTitle(title == null || title.isBlank() ? "Photo" : title);
        dialog.getDialogPane().setContent(view);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
}
