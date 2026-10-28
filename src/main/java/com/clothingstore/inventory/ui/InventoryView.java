package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.dao.CategoryDao;
import com.clothingstore.inventory.dao.DataAccessException;
import com.clothingstore.inventory.model.Category;
import com.clothingstore.inventory.model.Item;
import com.clothingstore.inventory.model.ItemStatus;
import com.clothingstore.inventory.service.InventoryService;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

public class InventoryView extends BorderPane {

    private static final String ALL_CATEGORIES = "All categories";
    private static final String ALL_STATUSES = "Any status";

    private final InventoryService inventoryService;
    private final CategoryDao categoryDao;
    private final TableView<Item> table = new TableView<>();
    private final ObservableList<Item> data = FXCollections.observableArrayList();
    private final FilteredList<Item> filteredData = new FilteredList<>(data, i -> true);
    private final TextField searchField = new TextField();
    private final ComboBox<String> categoryFilter = new ComboBox<>();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final Label subtitle = new Label();
    private final Label resultCount = new Label();

    public InventoryView(InventoryService inventoryService, CategoryDao categoryDao) {
        this.inventoryService = inventoryService;
        this.categoryDao = categoryDao;
        setPadding(new Insets(28, 32, 28, 32));

        buildTable();

        // ---- header actions
        Button addBtn = Ui.button("Add item", "plus", "accent");
        addBtn.setOnAction(e -> onAdd());
        addBtn.setTooltip(new Tooltip("Add a new item (Ctrl+N)"));
        Button categoriesBtn = Ui.button("Categories", "folder");
        categoriesBtn.setOnAction(e -> onManageCategories());

        // ---- toolbar: search + filters on the left, selection actions on the right
        searchField.setPromptText("Search name or SKU");
        searchField.setAccessibleText("Search inventory by name or SKU");
        searchField.setPrefWidth(260);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        StackPane searchBox = new StackPane(searchField, Icons.styled("search", 15, "icon-muted"));
        StackPane.setAlignment(searchBox.getChildren().get(1), Pos.CENTER_LEFT);
        StackPane.setMargin(searchBox.getChildren().get(1), new Insets(0, 0, 0, 10));
        searchField.setStyle("-fx-padding: 6 10 6 32;");

        categoryFilter.setAccessibleText("Filter by category");
        categoryFilter.valueProperty().addListener((o, a, b) -> applyFilter());
        statusFilter.setAccessibleText("Filter by status");
        statusFilter.getItems().add(ALL_STATUSES);
        for (ItemStatus s : ItemStatus.values()) statusFilter.getItems().add(s.displayName());
        statusFilter.setValue(ALL_STATUSES);
        statusFilter.valueProperty().addListener((o, a, b) -> applyFilter());

        Button editBtn = Ui.button("Edit", "edit");
        editBtn.setOnAction(e -> onEdit());
        Button restockBtn = Ui.button("Restock", "restock");
        restockBtn.setOnAction(e -> onRestock());
        Button deleteBtn = Ui.button("Delete", "trash", "danger", "flat");
        deleteBtn.setOnAction(e -> onDelete());
        // Selection actions are only enabled when there's something selected.
        var noSelection = table.getSelectionModel().selectedItemProperty().isNull();
        editBtn.disableProperty().bind(noSelection);
        restockBtn.disableProperty().bind(noSelection);
        deleteBtn.disableProperty().bind(noSelection);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, searchBox, categoryFilter, statusFilter, spacer, editBtn, restockBtn, deleteBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        resultCount.getStyleClass().add("muted");
        Label hint = new Label("Double-click a row to edit  ·  Del to remove  ·  Ctrl+F to search");
        hint.getStyleClass().add("kbd-hint");
        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        HBox footer = new HBox(resultCount, footerSpacer, hint);

        VBox card = Ui.card(null, toolbar, table, footer);
        VBox.setVgrow(table, Priority.ALWAYS);

        setTop(Ui.pageHeader("Inventory", subtitle, categoriesBtn, addBtn));
        setCenter(card);

        // ---- keyboard
        table.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) onDelete();
            else if (e.getCode() == KeyCode.ENTER) onEdit();
        });
        sceneProperty().addListener((o, oldScene, scene) -> {
            if (scene == null) return;
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN), () -> {
                if (isVisible()) Platform.runLater(searchField::requestFocus);
            });
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN), () -> {
                if (isVisible()) onAdd();
            });
        });

        refresh();
    }

    public void refresh() {
        data.setAll(inventoryService.getAllItems());

        // Rebuild the category filter from what's actually in stock, keeping the choice.
        String keep = categoryFilter.getValue();
        TreeSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Item i : data) if (i.getCategoryName() != null) names.add(i.getCategoryName());
        categoryFilter.getItems().setAll(ALL_CATEGORIES);
        categoryFilter.getItems().addAll(names);
        categoryFilter.setValue(keep != null && categoryFilter.getItems().contains(keep) ? keep : ALL_CATEGORIES);

        // Re-apply whatever search text is already there rather than resetting it,
        // so refreshing after an edit doesn't clobber what the user was searching for.
        applyFilter();

        long low = data.stream().filter(i -> i.getStatus() == ItemStatus.LOW_STOCK).count();
        long out = data.stream().filter(i -> i.getStatus() == ItemStatus.OUT_OF_STOCK).count();
        subtitle.setText(data.size() + (data.size() == 1 ? " item" : " items")
                + "  ·  " + low + " low stock  ·  " + out + " out of stock");
    }

    private void applyFilter() {
        String query = searchField.getText();
        String needle = query == null ? "" : query.trim().toLowerCase();
        String category = categoryFilter.getValue();
        String status = statusFilter.getValue();
        filteredData.setPredicate(item ->
                (needle.isEmpty()
                        || item.getName().toLowerCase().contains(needle)
                        || item.getSku().toLowerCase().contains(needle))
                && (category == null || ALL_CATEGORIES.equals(category) || category.equals(item.getCategoryName()))
                && (status == null || ALL_STATUSES.equals(status)
                        || (item.getStatus() != null && status.equals(item.getStatus().displayName()))));

        boolean filtering = !needle.isEmpty()
                || (category != null && !ALL_CATEGORIES.equals(category))
                || (status != null && !ALL_STATUSES.equals(status));
        resultCount.setText(filtering
                ? "Showing " + filteredData.size() + " of " + data.size()
                : data.size() + (data.size() == 1 ? " item" : " items"));
        table.setPlaceholder(data.isEmpty()
                ? Ui.emptyState("box", "No items yet", "Click “Add item” to add your first product.")
                : Ui.emptyState("search", "No matching items", "Try a different search or clear the filters."));
    }

    private void buildTable() {
        Ui.setupTable(table);

        TableColumn<Item, String> photoCol = new TableColumn<>("");
        photoCol.setCellValueFactory(new PropertyValueFactory<>("imagePath"));
        photoCol.setCellFactory(col -> new ThumbnailCell<>(40));
        photoCol.setSortable(false);
        photoCol.setMinWidth(64);
        photoCol.setMaxWidth(64);

        TableColumn<Item, Item> nameCol = new TableColumn<>("ITEM");
        nameCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        nameCol.setCellFactory(col -> Ui.twoLineCell(Item::getName, Item::getSku));
        nameCol.setComparator(Comparator.comparing(Item::getName, String.CASE_INSENSITIVE_ORDER));
        nameCol.setMinWidth(200);

        TableColumn<Item, String> categoryCol = new TableColumn<>("CATEGORY");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        categoryCol.setMinWidth(100);

        TableColumn<Item, String> sizeCol = new TableColumn<>("SIZE");
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("size"));
        sizeCol.setMinWidth(56);
        sizeCol.setMaxWidth(80);

        TableColumn<Item, String> colorCol = new TableColumn<>("COLOR");
        colorCol.setCellValueFactory(new PropertyValueFactory<>("color"));
        colorCol.setMinWidth(70);

        TableColumn<Item, Number> priceCol = new TableColumn<>("PRICE");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setCellFactory(col -> Ui.moneyCell());
        priceCol.getStyleClass().add("numeric");
        priceCol.setMinWidth(100);

        TableColumn<Item, Number> qtyCol = new TableColumn<>("QTY");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setCellFactory(col -> Ui.numericCell());
        qtyCol.getStyleClass().add("numeric");
        qtyCol.setMinWidth(56);
        qtyCol.setMaxWidth(80);

        TableColumn<Item, ItemStatus> statusCol = new TableColumn<>("STATUS");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new StatusBadgeCell<>());
        statusCol.setMinWidth(120);

        table.getColumns().setAll(List.of(photoCol, nameCol, categoryCol, sizeCol, colorCol, priceCol, qtyCol, statusCol));
        table.setRowFactory(tv -> {
            TableRow<Item> row = new TableRow<>();
            row.setPrefHeight(56); // tall enough for the thumbnail
            row.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !row.isEmpty()) onEdit();
            });
            return row;
        });

        SortedList<Item> sorted = new SortedList<>(filteredData);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
    }

    private void onAdd() {
        List<Category> categories = categoryDao.findAll();
        if (categories.isEmpty()) {
            Ui.alert(Alert.AlertType.WARNING, "Add at least one category first (Categories button, top right).").showAndWait();
            return;
        }
        ItemFormDialog dialog = new ItemFormDialog(categories, null, inventoryService.getAllItems());
        dialog.showAndWait().ifPresent(item -> {
            try {
                inventoryService.addItem(item);
                refresh();
                selectItem(item.getSku());
            } catch (DataAccessException ex) {
                Ui.alert(Alert.AlertType.ERROR,
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
                selectItem(item.getSku());
            } catch (DataAccessException ex) {
                Ui.alert(Alert.AlertType.ERROR,
                        "Couldn't save changes -- check that the SKU isn't already used by another item.")
                        .showAndWait();
            }
        });
    }

    private void selectItem(String sku) {
        for (Item i : table.getItems()) {
            if (i.getSku().equals(sku)) {
                table.getSelectionModel().select(i);
                table.scrollTo(i);
                return;
            }
        }
    }

    private void onRestock() {
        Item selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Ui.alert(Alert.AlertType.INFORMATION, "Select an item to restock first.").showAndWait();
            return;
        }

        TextInputDialog qtyDialog = new TextInputDialog("10");
        Ui.style(qtyDialog);
        qtyDialog.setTitle("Restock");
        qtyDialog.setHeaderText("Restock “" + selected.getName() + "”\nCurrently " + selected.getQuantity() + " in stock.");
        qtyDialog.setGraphic(null);
        qtyDialog.setContentText("Quantity to add:");

        Optional<String> result = qtyDialog.showAndWait();
        result.ifPresent(text -> {
            try {
                int qty = Integer.parseInt(text.trim());
                if (qty <= 0) {
                    Ui.alert(Alert.AlertType.WARNING, "Enter a positive quantity.").showAndWait();
                    return;
                }
                inventoryService.restockItem(selected, qty);
                refresh();
                selectItem(selected.getSku());
            } catch (NumberFormatException ex) {
                Ui.alert(Alert.AlertType.WARNING, "Enter a whole number.").showAndWait();
            } catch (DataAccessException ex) {
                Ui.alert(Alert.AlertType.ERROR, "Restock failed: " + ex.getMessage()).showAndWait();
            }
        });
    }

    private void onDelete() {
        Item selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirm = Ui.alert(Alert.AlertType.CONFIRMATION,
                "Remove “" + selected.getName() + "” from your inventory?\n\n"
                        + "Items with sales or stock history are archived instead, so past orders stay intact.");
        confirm.setTitle("Remove item");
        ButtonType remove = new ButtonType("Remove", ButtonBar.ButtonData.OK_DONE);
        confirm.getButtonTypes().setAll(ButtonType.CANCEL, remove);
        ((Button) confirm.getDialogPane().lookupButton(remove)).getStyleClass().add("danger");
        // Safer default: Enter cancels rather than deletes.
        ((Button) confirm.getDialogPane().lookupButton(remove)).setDefaultButton(false);
        ((Button) confirm.getDialogPane().lookupButton(ButtonType.CANCEL)).setDefaultButton(true);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == remove) {
            try {
                InventoryService.RemoveResult outcome = inventoryService.removeItem(selected);
                refresh();
                if (outcome == InventoryService.RemoveResult.ARCHIVED) {
                    Ui.alert(Alert.AlertType.INFORMATION,
                            "“" + selected.getName() + "” has sales or stock history, so it was archived "
                                    + "instead of deleted. It no longer appears in Inventory or Checkout, "
                                    + "and your past orders are unchanged.").showAndWait();
                }
            } catch (DataAccessException ex) {
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                Ui.alert(Alert.AlertType.ERROR,
                        "Couldn't remove this item: " + cause.getMessage()).showAndWait();
            }
        }
    }

    private void onManageCategories() {
        new CategoryManagerDialog(categoryDao).showAndWait();
        refresh(); // category names may have changed
    }
}
