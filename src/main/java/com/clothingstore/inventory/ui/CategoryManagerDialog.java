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
import javafx.scene.control.Label;

/** Simple dialog for adding or removing item categories. */
public class CategoryManagerDialog extends Dialog<Void> {

    public CategoryManagerDialog(CategoryDao categoryDao) {
        setTitle("Categories");
        Ui.style(this);

        ObservableList<Category> data = FXCollections.observableArrayList(categoryDao.findAll());
        ListView<Category> listView = new ListView<>(data);
        listView.setPrefSize(280, 220);

        TextField newCategoryField = new TextField();
        newCategoryField.setPromptText("New category name");
        newCategoryField.setAccessibleText("New category name");
        HBox.setHgrow(newCategoryField, javafx.scene.layout.Priority.ALWAYS);

        Button addBtn = Ui.button("Add", "plus", "accent");
        addBtn.setDefaultButton(true);
        addBtn.setOnAction(e -> {
            String name = newCategoryField.getText().trim();
            if (name.isEmpty()) return;
            try {
                Category saved = categoryDao.save(new Category(0, name));
                data.add(saved);
                newCategoryField.clear();
            } catch (DataAccessException ex) {
                Ui.alert(Alert.AlertType.ERROR, "Couldn't add category (it may already exist).").showAndWait();
            }
        });

        Button deleteBtn = Ui.button("Delete selected", "trash", "danger", "flat");
        deleteBtn.disableProperty().bind(listView.getSelectionModel().selectedItemProperty().isNull());
        deleteBtn.setOnAction(e -> {
            Category selected = listView.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            try {
                categoryDao.deleteById(selected.getCategoryId());
                data.remove(selected);
            } catch (DataAccessException ex) {
                Ui.alert(Alert.AlertType.ERROR,
                        "Couldn't delete this category -- items are still assigned to it.").showAndWait();
            }
        });

        Label heading = new Label("Categories");
        heading.getStyleClass().add("dialog-title");
        Label sub = new Label("Group your items. A category can only be deleted when no items use it.");
        sub.getStyleClass().add("field-hint");
        sub.setWrapText(true);
        listView.setPlaceholder(Ui.muted("No categories yet."));

        HBox addRow = new HBox(8, newCategoryField, addBtn);
        VBox content = new VBox(12, new VBox(4, heading, sub), addRow, listView, deleteBtn);
        content.setPadding(new Insets(20, 20, 8, 20));
        content.setPrefWidth(360);

        BorderPane pane = new BorderPane(content);
        getDialogPane().setContent(pane);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    }
}
