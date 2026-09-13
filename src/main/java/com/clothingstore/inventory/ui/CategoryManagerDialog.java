package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.dao.CategoryDao;
import com.clothingstore.inventory.dao.DataAccessException;
import com.clothingstore.inventory.model.Category;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Simple dialog for adding or removing item categories. */
public class CategoryManagerDialog extends Dialog<Void> {

    public CategoryManagerDialog(CategoryDao categoryDao) {
        setTitle("Manage Categories");

        ObservableList<Category> data = FXCollections.observableArrayList(categoryDao.findAll());
        ListView<Category> listView = new ListView<>(data);
        listView.setPrefSize(280, 220);

        TextField newCategoryField = new TextField();
        newCategoryField.setPromptText("New category name");

        Button addBtn = new Button("Add");
        addBtn.setOnAction(e -> {
            String name = newCategoryField.getText().trim();
            if (name.isEmpty()) return;
            try {
                Category saved = categoryDao.save(new Category(0, name));
                data.add(saved);
                newCategoryField.clear();
            } catch (DataAccessException ex) {
                new Alert(Alert.AlertType.ERROR, "Couldn't add category (it may already exist).").showAndWait();
            }
        });

        Button deleteBtn = new Button("Delete Selected");
        deleteBtn.setOnAction(e -> {
            Category selected = listView.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            try {
                categoryDao.deleteById(selected.getCategoryId());
                data.remove(selected);
            } catch (DataAccessException ex) {
                new Alert(Alert.AlertType.ERROR,
                        "Couldn't delete this category -- items are still assigned to it.").showAndWait();
            }
        });

        HBox addRow = new HBox(8, newCategoryField, addBtn);
        VBox content = new VBox(10, listView, addRow, deleteBtn);
        content.setPadding(new Insets(16));

        BorderPane pane = new BorderPane(content);
        getDialogPane().setContent(pane);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    }
}
