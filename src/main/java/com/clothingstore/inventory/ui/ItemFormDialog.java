package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.model.Category;
import com.clothingstore.inventory.model.Item;
import com.clothingstore.inventory.util.ImageStorage;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Modal dialog for adding a new item or editing an existing one. */
public class ItemFormDialog extends Dialog<Item> {

    // Tracks the newly picked file (if any) separately from the item's existing
    // stored path, so we only copy it into images/ once Save is actually clicked --
    // picking an image and then hitting Cancel shouldn't leave orphan files behind.
    private File pickedImageFile;
    private String existingImagePath;

    public ItemFormDialog(List<Category> categories, Item existing) {
        setTitle(existing == null ? "Add New Item" : "Edit Item");
        existingImagePath = existing != null ? existing.getImagePath() : null;

        TextField skuField = new TextField(existing != null ? existing.getSku() : "");
        TextField nameField = new TextField(existing != null ? existing.getName() : "");
        ComboBox<Category> categoryBox = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(categories));
        TextField sizeField = new TextField(existing != null ? existing.getSize() : "");
        TextField colorField = new TextField(existing != null ? existing.getColor() : "");
        TextField priceField = new TextField(existing != null ? String.valueOf(existing.getPrice()) : "");
        TextField quantityField = new TextField(existing != null ? String.valueOf(existing.getQuantity()) : "0");
        TextField reorderField = new TextField(existing != null ? String.valueOf(existing.getReorderThreshold()) : "5");

        ImageView preview = new ImageView();
        preview.setFitWidth(100);
        preview.setFitHeight(100);
        preview.setPreserveRatio(true);
        loadPreview(preview, existingImagePath);

        Button chooseImageBtn = new Button(existingImagePath == null ? "Choose Image..." : "Change Image...");
        chooseImageBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Item Photo");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
            File selected = chooser.showOpenDialog(getDialogPane().getScene().getWindow());
            if (selected != null) {
                pickedImageFile = selected;
                preview.setImage(new Image(selected.toURI().toString(), 100, 100, true, true));
            }
        });

        VBox imageBox = new VBox(8, preview, chooseImageBtn);
        imageBox.setAlignment(Pos.TOP_CENTER);

        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.web("#dc2626"));
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(320);

        if (existing != null) {
            categories.stream()
                    .filter(c -> c.getCategoryId() == existing.getCategoryId())
                    .findFirst()
                    .ifPresent(categoryBox::setValue);
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        int r = 0;
        grid.addRow(r++, new Label("SKU:"), skuField);
        grid.addRow(r++, new Label("Name:"), nameField);
        grid.addRow(r++, new Label("Category:"), categoryBox);
        grid.addRow(r++, new Label("Size:"), sizeField);
        grid.addRow(r++, new Label("Color:"), colorField);
        grid.addRow(r++, new Label("Price:"), priceField);
        grid.addRow(r++, new Label("Quantity:"), quantityField);
        grid.addRow(r++, new Label("Reorder Threshold:"), reorderField);
        grid.addRow(r++, new Label("Photo:"), imageBox);
        grid.add(errorLabel, 0, r, 2, 1);

        getDialogPane().setContent(grid);
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Validate on click and CONSUME the event (keeps the dialog open) if anything's wrong,
        // instead of letting bad data reach the database and fail there with a raw exception.
        Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            List<String> errors = validate(skuField, nameField, categoryBox, priceField, quantityField, reorderField);
            if (!errors.isEmpty()) {
                errorLabel.setText(String.join(" \u2022 ", errors));
                event.consume();
            }
        });

        setResultConverter(buttonType -> {
            if (buttonType != saveButtonType) return null;

            Optional<Category> selected = Optional.ofNullable(categoryBox.getValue());
            Item item = existing != null ? existing : new Item();
            item.setSku(skuField.getText().trim());
            item.setName(nameField.getText().trim());
            item.setCategoryId(selected.map(Category::getCategoryId).orElse(0));
            item.setSize(sizeField.getText().trim());
            item.setColor(colorField.getText().trim());
            item.setPrice(Double.parseDouble(priceField.getText().trim()));
            item.setReorderThreshold(Integer.parseInt(reorderField.getText().trim()));
            item.setQuantity(Integer.parseInt(quantityField.getText().trim()));
            if (existing == null) {
                item.setDateAdded(LocalDate.now());
            }

            // Only copy the file into images/ now that Save was actually confirmed.
            if (pickedImageFile != null) {
                item.setImagePath(ImageStorage.store(pickedImageFile.toPath()));
            } else if (existingImagePath != null) {
                item.setImagePath(existingImagePath);
            }

            return item;
        });
    }

    private void loadPreview(ImageView view, String imagePath) {
        if (imagePath == null || imagePath.isBlank()) return;
        File file = ImageStorage.resolve(imagePath).toFile();
        if (file.exists()) {
            view.setImage(new Image(file.toURI().toString(), 100, 100, true, true));
        }
    }

    /**
     * Returns a list of human-readable problems with the form, or an empty
     * list if it's valid. Checked all at once (rather than field-by-field
     * on the first failure) so the user sees every problem in one pass.
     */
    private List<String> validate(TextField skuField, TextField nameField, ComboBox<Category> categoryBox,
                                   TextField priceField, TextField quantityField, TextField reorderField) {
        List<String> errors = new ArrayList<>();

        if (skuField.getText() == null || skuField.getText().isBlank()) {
            errors.add("SKU is required");
        }
        if (nameField.getText() == null || nameField.getText().isBlank()) {
            errors.add("Name is required");
        }
        if (categoryBox.getValue() == null) {
            errors.add("Category is required");
        }

        errors.addAll(checkNonNegativeNumber(priceField.getText(), "Price"));
        errors.addAll(checkNonNegativeInteger(quantityField.getText(), "Quantity"));
        errors.addAll(checkNonNegativeInteger(reorderField.getText(), "Reorder threshold"));

        return errors;
    }

    private List<String> checkNonNegativeNumber(String text, String fieldName) {
        try {
            double value = Double.parseDouble(text.trim());
            if (value < 0) {
                return List.of(fieldName + " can't be negative");
            }
        } catch (NumberFormatException e) {
            return List.of(fieldName + " must be a valid number");
        }
        return List.of();
    }

    private List<String> checkNonNegativeInteger(String text, String fieldName) {
        try {
            int value = Integer.parseInt(text.trim());
            if (value < 0) {
                return List.of(fieldName + " can't be negative");
            }
        } catch (NumberFormatException e) {
            return List.of(fieldName + " must be a whole number");
        }
        return List.of();
    }
}
