package com.clothingstore.inventory;

import com.clothingstore.inventory.dao.*;
import com.clothingstore.inventory.service.InventoryService;
import com.clothingstore.inventory.service.OrderService;
import com.clothingstore.inventory.ui.CheckoutView;
import com.clothingstore.inventory.ui.DashboardView;
import com.clothingstore.inventory.ui.InventoryView;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Entry point. Opening this app is meant to be instant: it opens straight
 * to the Dashboard with no login screen or setup wizard, since it's a
 * single-user desktop tool running entirely off a local SQLite file.
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        DatabaseManager.initializeSchema();

        ItemDao itemDao = new ItemDao();
        CategoryDao categoryDao = new CategoryDao();
        OrderDao orderDao = new OrderDao();
        StockTransactionDao stockTransactionDao = new StockTransactionDao();

        InventoryService inventoryService = new InventoryService(itemDao);
        OrderService orderService = new OrderService(itemDao, orderDao, stockTransactionDao);

        StackPane content = new StackPane();
        DashboardView dashboardView = new DashboardView(inventoryService);
        InventoryView inventoryView = new InventoryView(inventoryService, categoryDao);
        CheckoutView checkoutView = new CheckoutView(inventoryService, orderService, () -> {
            // After a sale completes, both other screens should reflect it immediately.
            dashboardView.refresh();
            inventoryView.refresh();
        });

        content.getChildren().addAll(dashboardView, inventoryView, checkoutView);
        showOnly(content, dashboardView);

        Button dashboardBtn = navButton("Dashboard", () -> {
            dashboardView.refresh();
            showOnly(content, dashboardView);
        });
        Button inventoryBtn = navButton("Inventory", () -> {
            inventoryView.refresh();
            showOnly(content, inventoryView);
        });
        Button checkoutBtn = navButton("Checkout", () -> {
            checkoutView.refreshCatalog();
            showOnly(content, checkoutView);
        });

        VBox nav = new VBox(8, dashboardBtn, inventoryBtn, checkoutBtn);
        nav.setPadding(new Insets(20, 12, 20, 12));
        nav.setPrefWidth(160);
        nav.setStyle("-fx-background-color: #1e293b;");
        for (Button b : java.util.List.of(dashboardBtn, inventoryBtn, checkoutBtn)) {
            b.setMaxWidth(Double.MAX_VALUE);
        }

        BorderPane root = new BorderPane();
        root.setLeft(nav);
        root.setCenter(content);

        Scene scene = new Scene(root, 1100, 700);
        stage.setTitle("Clothing Store Inventory System");
        stage.setScene(scene);
        stage.show();
    }

    private Button navButton(String text, Runnable onClick) {
        Button b = new Button(text);
        b.setOnAction(e -> onClick.run());
        return b;
    }

    private void showOnly(StackPane stack, javafx.scene.Node visible) {
        for (javafx.scene.Node node : stack.getChildren()) {
            node.setVisible(node == visible);
            node.setManaged(node == visible);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
