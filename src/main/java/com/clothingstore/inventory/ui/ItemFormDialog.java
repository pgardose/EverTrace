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
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Modal dialog for adding a new item or editing an existing one. */
public class ItemFormDialog extends Dialog<Item> {

    // Tracks the newly picked file (if any) separately from the item's existing
    // stored path, so we only copy it into images/ once Save is actually clicked --
    // picking an image and then hitting Cancel shouldn't leave orphan files behind.
    private static final double PHOTO_SIZE = 168;

    private File pickedImageFile;
    private String existingImagePath;

    public ItemFormDialog(List<Category> categories, Item existing) {
        this(categories, existing, List.of());
    }

    /**
     * @param existingItems current inventory, used only to compute the next
     *                       sequence number when auto-generating a SKU for a
     *                       brand-new item. Pass an empty list when editing --
     *                       auto-generation never runs in edit mode.
     */
    public ItemFormDialog(List<Category> categories, Item existing, List<Item> existingItems) {
        setTitle(existing == null ? "Add item" : "Edit item");
        Ui.style(this);
        existingImagePath = existing != null ? existing.getImagePath() : null;

        TextField skuField = new TextField(existing != null ? existing.getSku() : "");
        TextField nameField = new TextField(existing != null ? existing.getName() : "");
        ComboBox<Category> categoryBox = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(categories));
        TextField sizeField = new TextField(existing != null ? existing.getSize() : "");
        TextField colorField = new TextField(existing != null ? existing.getColor() : "");
        TextField priceField = new TextField(existing != null ? String.valueOf(existing.getPrice()) : "");
        TextField quantityField = new TextField(existing != null ? String.valueOf(existing.getQuantity()) : "0");
        TextField reorderField = new TextField(existing != null ? String.valueOf(existing.getReorderThreshold()) : "5");

        // --- SKU auto-generation (new items only) ---
        // Regenerates the SKU from category/color/size UNTIL the user types into
        // the SKU field themselves -- after that, we assume they want manual control
        // and stop overwriting what they typed.
        boolean[] userEditedSku = {false};
        boolean[] settingSkuProgrammatically = {false};
        skuField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!settingSkuProgrammatically[0]) {
                userEditedSku[0] = true;
            }
        });
        Runnable regenerateSku = () -> {
            if (existing != null || userEditedSku[0]) return;
            Category cat = categoryBox.getValue();
            String size = sizeField.getText();
            String color = colorField.getText();
            if (cat == null || size == null || size.isBlank() || color == null || color.isBlank()) return;

            String generated = generateSku(cat.getName(), color, size, existingItems);
            settingSkuProgrammatically[0] = true;
            skuField.setText(generated);
            settingSkuProgrammatically[0] = false;
        };
        categoryBox.valueProperty().addListener((o, ov, nv) -> regenerateSku.run());
        sizeField.textProperty().addListener((o, ov, nv) -> regenerateSku.run());
        colorField.textProperty().addListener((o, ov, nv) -> regenerateSku.run());

        ImageView preview = new ImageView();
        preview.setFitWidth(PHOTO_SIZE);
        preview.setFitHeight(PHOTO_SIZE);
        preview.setPreserveRatio(true);
        loadPreview(preview, existingImagePath);

        // Placeholder shown in the photo tile until there's an image.
        Label noPhoto = new Label("No photo yet");
        noPhoto.getStyleClass().add("field-hint");
        VBox placeholder = new VBox(6, Icons.styled("image", 28, "icon-subtle"), noPhoto);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.visibleProperty().bind(preview.imageProperty().isNull());
        StackPane photoTile = new StackPane(placeholder, preview);
        photoTile.getStyleClass().add("photo-drop");
        photoTile.setMinSize(PHOTO_SIZE + 16, PHOTO_SIZE + 16);
        photoTile.setMaxSize(PHOTO_SIZE + 16, PHOTO_SIZE + 16);

        Button chooseImageBtn = Ui.button(existingImagePath == null ? "Choose photo…" : "Change photo…", "image");
        chooseImageBtn.setMaxWidth(Double.MAX_VALUE);
        Button removeImageBtn = Ui.button("Remove photo", null, "flat", "danger");
        removeImageBtn.setMaxWidth(Double.MAX_VALUE);
        removeImageBtn.visibleProperty().bind(preview.imageProperty().isNotNull());
        removeImageBtn.managedProperty().bind(removeImageBtn.visibleProperty());
        removeImageBtn.setOnAction(e -> {
            pickedImageFile = null;
            existingImagePath = null;
            preview.setImage(null);
            chooseImageBtn.setText("Choose photo…");
        });
        chooseImageBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Item Photo");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
            File selected = chooser.showOpenDialog(getDialogPane().getScene().getWindow());
            if (selected != null) {
                pickedImageFile = selected;
                preview.setImage(new Image(selected.toURI().toString(), PHOTO_SIZE, PHOTO_SIZE, true, true));
                chooseImageBtn.setText("Change photo…");
            }
        });

        Label photoHint = new Label("PNG, JPG or GIF. Shown in Inventory and Checkout.");
        photoHint.getStyleClass().add("field-hint");
        photoHint.setWrapText(true);
        photoHint.setMaxWidth(PHOTO_SIZE + 16);
        VBox imageBox = new VBox(10, Ui.sectionLabel("PHOTO"), photoTile, chooseImageBtn, removeImageBtn, photoHint);
        imageBox.setAlignment(Pos.TOP_LEFT);
        imageBox.setMinWidth(PHOTO_SIZE + 16);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("form-error");
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(Double.MAX_VALUE);
        errorLabel.setMinHeight(Region.USE_PREF_SIZE);
        errorLabel.visibleProperty().bind(errorLabel.textProperty().isNotEmpty());
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        if (existing != null) {
            categories.stream()
                    .filter(c -> c.getCategoryId() == existing.getCategoryId())
                    .findFirst()
                    .ifPresent(categoryBox::setValue);
        }

        // ---- layout: header, then photo on the left and the form in a 2-column grid
        nameField.setPromptText("e.g. Classic Crew Tee");
        sizeField.setPromptText("e.g. M, 32, OS");
        colorField.setPromptText("e.g. Black");
        priceField.setPromptText("0.00");
        categoryBox.setPromptText("Choose…");
        categoryBox.setMaxWidth(Double.MAX_VALUE);
        Label skuHint = new Label(existing == null ? "Filled in automatically from category, color and size." : " ");
        skuHint.getStyleClass().add("field-hint");
        skuHint.setWrapText(true);
        Label qtyHint = new Label(existing == null ? "Starting stock." : "Tip: Restock keeps a log.");
        qtyHint.getStyleClass().add("field-hint");
        Label reorderHint = new Label("Flag as low stock at or below this.");
        reorderHint.getStyleClass().add("field-hint");
        reorderHint.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        ColumnConstraints c1 = new ColumnConstraints(150, 190, Double.MAX_VALUE);
        c1.setHgrow(Priority.ALWAYS);
        ColumnConstraints c2 = new ColumnConstraints(150, 190, Double.MAX_VALUE);
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);
        int r = 0;
        grid.add(Ui.sectionLabel("DETAILS"), 0, r++, 2, 1);
        grid.add(required(Ui.field("Name", nameField)), 0, r++, 2, 1);
        grid.add(required(Ui.field("Category", categoryBox)), 0, r);
        VBox skuBox = required(Ui.field("SKU", skuField));
        skuBox.getChildren().add(skuHint);
        grid.add(skuBox, 1, r++);
        grid.add(Ui.field("Size", sizeField), 0, r);
        grid.add(Ui.field("Color", colorField), 1, r++);
        grid.add(Ui.sectionLabel("PRICING & STOCK"), 0, r++, 2, 1);
        grid.add(required(Ui.field("Price (₱)", priceField)), 0, r);
        VBox qtyBox = required(Ui.field("Quantity", quantityField));
        qtyBox.getChildren().add(qtyHint);
        grid.add(qtyBox, 1, r++);
        VBox reorderBox = required(Ui.field("Reorder threshold", reorderField));
        reorderBox.getChildren().add(reorderHint);
        grid.add(reorderBox, 0, r++);

        Label heading = new Label(existing == null ? "Add a new item" : "Edit “" + existing.getName() + "”");
        heading.getStyleClass().add("dialog-title");
        Label sub = new Label("Fields marked * are required.");
        sub.getStyleClass().add("field-hint");

        HBox columns = new HBox(24, imageBox, grid);
        HBox.setHgrow(grid, Priority.ALWAYS);
        VBox root = new VBox(16, new VBox(4, heading, sub), errorLabel, columns);
        root.setPadding(new Insets(20, 20, 8, 20));

        getDialogPane().setContent(root);
        ButtonType saveButtonType = new ButtonType(existing == null ? "Add item" : "Save changes", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Validate on click and CONSUME the event (keeps the dialog open) if anything's wrong,
        // instead of letting bad data reach the database and fail there with a raw exception.
        Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            List<String> errors = validate(skuField, nameField, categoryBox, priceField, quantityField, reorderField);
            if (!errors.isEmpty()) {
                errorLabel.setText(errors.size() == 1
                        ? errors.get(0) + "."
                        : "Please fix the highlighted fields:\n\u2022 " + String.join("\n\u2022 ", errors));
                event.consume();
                // The banner adds height; grow the window so the buttons stay visible.
                javafx.application.Platform.runLater(() -> getDialogPane().getScene().getWindow().sizeToScene());
                // Move focus to the first problem so keyboard users land on it.
                for (Control c : List.of(nameField, categoryBox, skuField, priceField, quantityField, reorderField)) {
                    if (c.getStyleClass().contains("invalid")) {
                        c.requestFocus();
                        break;
                    }
                }
            } else {
                errorLabel.setText("");
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
            } else {
                // Either the existing photo, or null if "Remove photo" was used.
                // (The old file stays on disk; past data never points at a missing image.)
                item.setImagePath(existingImagePath);
            }

            return item;
        });
    }

    /**
     * Builds a SKU like "TOP-BLA-M-001": first 3 letters of category + first 3
     * letters of color + size, uppercased, plus a 3-digit sequence number based
     * on how many existing items already share that exact combination.
     * <p>
     * Note: this uses the first 3 letters of the color name (so "Black" -> "BLA"),
     * not a curated abbreviation table (which would give "BLK"). Swap in a
     * lookup map here if specific abbreviations matter for your catalog.
     */
    private String generateSku(String categoryName, String color, String size, List<Item> existingItems) {
        String catPart = abbreviate(categoryName);
        String colorPart = abbreviate(color);
        String sizePart = size.trim().toUpperCase(Locale.ROOT);

        long matchingCount = existingItems.stream()
                .filter(i -> catPart.equals(abbreviate(i.getCategoryName()))
                        && colorPart.equals(abbreviate(i.getColor()))
                        && sizePart.equalsIgnoreCase(i.getSize()))
                .count();

        int sequence = (int) matchingCount + 1;
        return "%s-%s-%s-%03d".formatted(catPart, colorPart, sizePart, sequence);
    }

    private String abbreviate(String text) {
        if (text == null || text.isBlank()) return "GEN";
        String cleaned = text.trim().toUpperCase(Locale.ROOT);
        return cleaned.length() <= 3 ? cleaned : cleaned.substring(0, 3);
    }

    private void loadPreview(ImageView view, String imagePath) {
        if (imagePath == null || imagePath.isBlank()) return;
        File file = ImageStorage.resolve(imagePath).toFile();
        if (file.exists()) {
            view.setImage(new Image(file.toURI().toString(), PHOTO_SIZE, PHOTO_SIZE, true, true));
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

        boolean nameBad = nameField.getText() == null || nameField.getText().isBlank();
        if (nameBad) errors.add("Name is required");
        boolean categoryBad = categoryBox.getValue() == null;
        if (categoryBad) errors.add("Category is required");
        boolean skuBad = skuField.getText() == null || skuField.getText().isBlank();
        if (skuBad) errors.add("SKU is required");

        List<String> price = checkNonNegativeNumber(priceField.getText(), "Price");
        List<String> qty = checkNonNegativeInteger(quantityField.getText(), "Quantity");
        List<String> reorder = checkNonNegativeInteger(reorderField.getText(), "Reorder threshold");
        errors.addAll(price);
        errors.addAll(qty);
        errors.addAll(reorder);

        markInvalid(nameField, nameBad);
        markInvalid(categoryBox, categoryBad);
        markInvalid(skuField, skuBad);
        markInvalid(priceField, !price.isEmpty());
        markInvalid(quantityField, !qty.isEmpty());
        markInvalid(reorderField, !reorder.isEmpty());

        return errors;
    }

    /** Red border + accessible hint on a field with a problem; cleared once it's fixed. */
    private static void markInvalid(Control field, boolean invalid) {
        field.getStyleClass().remove("invalid");
        if (invalid) field.getStyleClass().add("invalid");
        field.setAccessibleHelp(invalid ? "This field has a problem" : null);
    }

    /** Appends a "*" to a field's label to mark it as required. */
    private static VBox required(VBox fieldBox) {
        Label label = (Label) fieldBox.getChildren().get(0);
        label.setText(label.getText() + " *");
        return fieldBox;
    }

    private List<String> checkNonNegativeNumber(String text, String fieldName) {
        if (text == null) text = "";
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
        if (text == null) text = "";
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
