package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.dao.CategoryDao;
import com.clothingstore.inventory.dao.DataAccessException;
import com.clothingstore.inventory.model.Category;
import com.clothingstore.inventory.model.Item;
import com.clothingstore.inventory.service.InventoryService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.Optional;

public class InventoryView extends BorderPane {

    private final InventoryService inventoryService;
    private final CategoryDao categoryDao;
    private final TableView<Item> table = new TableView<>();
    private final ObservableList<Item> data = FXCollections.observableArrayList();
    private final FilteredList<Item> filteredData = new FilteredList<>(data, i -> true);
    private final TextField searchField = new TextField();

    public InventoryView(InventoryService inventoryService, CategoryDao categoryDao) {
        this.inventoryService = inventoryService;
        this.categoryDao = categoryDao;
        setPadding(new Insets(20));

        Label title = new Label("Inventory");
        title.setFont(Font.font(null, FontWeight.BOLD, 22));

        buildTable();

        searchField.setPromptText("Search by name or SKU...");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter(newVal));

        Button addBtn = new Button("Add Item");
        addBtn.setOnAction(e -> onAdd());

        Button editBtn = new Button("Edit");
        editBtn.setOnAction(e -> onEdit());

        Button restockBtn = new Button("Restock");
        restockBtn.setOnAction(e -> onRestock());

        Button deleteBtn = new Button("Delete");
        deleteBtn.setOnAction(e -> onDelete());

        Button categoriesBtn = new Button("Manage Categories");
        categoriesBtn.setOnAction(e -> onManageCategories());

        HBox toolbar = new HBox(10, addBtn, editBtn, restockBtn, deleteBtn, categoriesBtn);
        toolbar.setPadding(new Insets(12, 0, 4, 0));

        HBox searchRow = new HBox(8, new Label("Search:"), searchField);
        searchRow.setPadding(new Insets(0, 0, 12, 0));

        setTop(new VBox(4, title, toolbar, searchRow));
        setCenter(table);

        refresh();
    }

    public void refresh() {
        data.setAll(inventoryService.getAllItems());
        // Re-apply whatever search text is already there rather than resetting it,
        // so refreshing after an edit doesn't clobber what the user was searching for.
        applyFilter(searchField.getText());
        table.setItems(filteredData);
    }

    private void applyFilter(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase();
        filteredData.setPredicate(item ->
                needle.isEmpty()
                        || item.getName().toLowerCase().contains(needle)
                        || item.getSku().toLowerCase().contains(needle));
    }

    private void buildTable() {
        TableColumn<Item, String> photoCol = new TableColumn<>("Photo");
        photoCol.setCellValueFactory(new PropertyValueFactory<>("imagePath"));
        photoCol.setCellFactory(col -> new ThumbnailCell<>());
        photoCol.setSortable(false);
        photoCol.setPrefWidth(70);

        TableColumn<Item, String> skuCol = new TableColumn<>("SKU");
        skuCol.setCellValueFactory(new PropertyValueFactory<>("sku"));

        TableColumn<Item, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Item, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("categoryName"));

        TableColumn<Item, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("size"));

        TableColumn<Item, String> colorCol = new TableColumn<>("Color");
        colorCol.setCellValueFactory(new PropertyValueFactory<>("color"));

        TableColumn<Item, Number> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));

        TableColumn<Item, Number> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<Item, com.clothingstore.inventory.model.ItemStatus> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new StatusBadgeCell<>());

        table.getColumns().setAll(List.of(photoCol, skuCol, nameCol, categoryCol, sizeCol, colorCol, priceCol, qtyCol, statusCol));
        table.setPrefHeight(500);
        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Item> row = new javafx.scene.control.TableRow<>();
            row.setPrefHeight(56); // tall enough for the thumbnail
            return row;
        });
    }

    private void onAdd() {
        List<Category> categories = categoryDao.findAll();
        if (categories.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Add at least one category first (Manage Categories).").showAndWait();
            return;
        }
        ItemFormDialog dialog = new ItemFormDialog(categories, null, inventoryService.getAllItems());
        dialog.showAndWait().ifPresent(item -> {
            try {
                inventoryService.addItem(item);
                refresh();
            } catch (DataAccessException ex) {
                new Alert(Alert.AlertType.ERROR,
                        "Couldn't save this item -- check that the SKU isn't already used by another item.")
                        .showAndWait();
            }
        });
    }

    private void onEdit() {
        Item selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        List<Category> categories = categoryDao.findAll();
        ItemFormDialog dialog = new ItemFormDialog(categories, selected);
        dialog.showAndWait().ifPresent(item -> {
            try {
                inventoryService.updateItem(item);
                refresh();
            } catch (DataAccessException ex) {
                new Alert(Alert.AlertType.ERROR,
                        "Couldn't save changes -- check that the SKU isn't already used by another item.")
                        .showAndWait();
            }
        });
    }

    private void onRestock() {
        Item selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.INFORMATION, "Select an item to restock first.").showAndWait();
            return;
        }

        TextInputDialog qtyDialog = new TextInputDialog("10");
        qtyDialog.setTitle("Restock " + selected.getName());
        qtyDialog.setHeaderText(null);
        qtyDialog.setContentText("Quantity to add:");

        Optional<String> result = qtyDialog.showAndWait();
        result.ifPresent(text -> {
            try {
                int qty = Integer.parseInt(text.trim());
                if (qty <= 0) {
                    new Alert(Alert.AlertType.WARNING, "Enter a positive quantity.").showAndWait();
                    return;
                }
                inventoryService.restockItem(selected, qty);
                refresh();
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.WARNING, "Enter a whole number.").showAndWait();
            } catch (DataAccessException ex) {
                new Alert(Alert.AlertType.ERROR, "Restock failed: " + ex.getMessage()).showAndWait();
            }
        });
    }

    private void onDelete() {
        Item selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + selected.getName() + "\"? This cannot be undone.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                inventoryService.removeItem(selected.getItemId());
                refresh();
            } catch (DataAccessException ex) {
                new Alert(Alert.AlertType.ERROR,
                        "Couldn't delete this item -- it may be referenced by an existing order.")
                        .showAndWait();
            }
        }
    }

    private void onManageCategories() {
        new CategoryManagerDialog(categoryDao).showAndWait();
        refresh(); // category names may have changed
    }
}
