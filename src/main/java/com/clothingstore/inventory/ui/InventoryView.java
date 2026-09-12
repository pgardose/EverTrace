package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.dao.CategoryDao;
import com.clothingstore.inventory.model.Category;
import com.clothingstore.inventory.model.Item;
import com.clothingstore.inventory.service.InventoryService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.Optional;

public class InventoryView extends BorderPane {

    private final InventoryService inventoryService;
    private final CategoryDao categoryDao;
    private final TableView<Item> table = new TableView<>();
    private final ObservableList<Item> data = FXCollections.observableArrayList();

    public InventoryView(InventoryService inventoryService, CategoryDao categoryDao) {
        this.inventoryService = inventoryService;
        this.categoryDao = categoryDao;
        setPadding(new Insets(20));

        Label title = new Label("Inventory");
        title.setFont(Font.font(null, FontWeight.BOLD, 22));

        buildTable();

        Button addBtn = new Button("Add Item");
        addBtn.setOnAction(e -> onAdd());

        Button editBtn = new Button("Edit");
        editBtn.setOnAction(e -> onEdit());

        Button restockBtn = new Button("Restock +10");
        restockBtn.setOnAction(e -> onQuickRestock());

        Button deleteBtn = new Button("Delete");
        deleteBtn.setOnAction(e -> onDelete());

        HBox toolbar = new HBox(10, addBtn, editBtn, restockBtn, deleteBtn);
        toolbar.setPadding(new Insets(12, 0, 12, 0));

        setTop(new javafx.scene.layout.VBox(4, title, toolbar));
        setCenter(table);

        refresh();
    }

    public void refresh() {
        data.setAll(inventoryService.getAllItems());
        table.setItems(data);
    }

    private void buildTable() {
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

        TableColumn<Item, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().getStatus().displayName()));

        table.getColumns().setAll(List.of(skuCol, nameCol, categoryCol, sizeCol, colorCol, priceCol, qtyCol, statusCol));
        table.setPrefHeight(500);
    }

    private void onAdd() {
        List<Category> categories = categoryDao.findAll();
        ItemFormDialog dialog = new ItemFormDialog(categories, null);
        dialog.showAndWait().ifPresent(item -> {
            inventoryService.addItem(item);
            refresh();
        });
    }

    private void onEdit() {
        Item selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        List<Category> categories = categoryDao.findAll();
        ItemFormDialog dialog = new ItemFormDialog(categories, selected);
        dialog.showAndWait().ifPresent(item -> {
            inventoryService.updateItem(item);
            refresh();
        });
    }

    private void onQuickRestock() {
        Item selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        selected.restock(10);
        inventoryService.updateItem(selected);
        refresh();
    }

    private void onDelete() {
        Item selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + selected.getName() + "\"? This cannot be undone.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            inventoryService.removeItem(selected.getItemId());
            refresh();
        }
    }
}
