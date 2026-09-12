package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.model.Category;
import com.clothingstore.inventory.model.Item;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Modal dialog for adding a new item or editing an existing one. */
public class ItemFormDialog extends Dialog<Item> {

    public ItemFormDialog(List<Category> categories, Item existing) {
        setTitle(existing == null ? "Add New Item" : "Edit Item");

        TextField skuField = new TextField(existing != null ? existing.getSku() : "");
        TextField nameField = new TextField(existing != null ? existing.getName() : "");
        ComboBox<Category> categoryBox = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(categories));
        TextField sizeField = new TextField(existing != null ? existing.getSize() : "");
        TextField colorField = new TextField(existing != null ? existing.getColor() : "");
        TextField priceField = new TextField(existing != null ? String.valueOf(existing.getPrice()) : "");
        TextField quantityField = new TextField(existing != null ? String.valueOf(existing.getQuantity()) : "0");
        TextField reorderField = new TextField(existing != null ? String.valueOf(existing.getReorderThreshold()) : "5");

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

        getDialogPane().setContent(grid);
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        setResultConverter(buttonType -> {
            if (buttonType != saveButtonType) return null;

            Optional<Category> selected = Optional.ofNullable(categoryBox.getValue());
            Item item = existing != null ? existing : new Item();
            item.setSku(skuField.getText().trim());
            item.setName(nameField.getText().trim());
            item.setCategoryId(selected.map(Category::getCategoryId).orElse(0));
            item.setSize(sizeField.getText().trim());
            item.setColor(colorField.getText().trim());
            item.setPrice(parseDoubleSafe(priceField.getText(), 0));
            item.setReorderThreshold((int) parseDoubleSafe(reorderField.getText(), 5));
            item.setQuantity((int) parseDoubleSafe(quantityField.getText(), 0));
            if (existing == null) {
                item.setDateAdded(LocalDate.now());
            }
            return item;
        });
    }

    private double parseDoubleSafe(String text, double fallback) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
