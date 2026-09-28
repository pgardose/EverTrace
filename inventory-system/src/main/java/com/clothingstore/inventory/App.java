package com.clothingstore.inventory;

import atlantafx.base.theme.CupertinoLight;
import com.clothingstore.inventory.dao.*;
import com.clothingstore.inventory.service.InventoryService;
import com.clothingstore.inventory.service.OrderService;
import com.clothingstore.inventory.ui.CheckoutView;
import com.clothingstore.inventory.ui.DashboardView;
import com.clothingstore.inventory.ui.InventoryView;
import com.clothingstore.inventory.ui.OrdersView;
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
        // AtlantaFX's flat, modern theme -- the JavaFX equivalent of FlatLaf.
        // CupertinoLight matches the "Mac Light" look from the original spec;
        // swap for PrimerLight for a cleaner GitHub-style look instead.
        Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());

        DatabaseManager.initializeSchema();

        ItemDao itemDao = new ItemDao();
        CategoryDao categoryDao = new CategoryDao();
        OrderDao orderDao = new OrderDao();
        StockTransactionDao stockTransactionDao = new StockTransactionDao();

        InventoryService inventoryService = new InventoryService(itemDao, stockTransactionDao);
        OrderService orderService = new OrderService(itemDao, orderDao, stockTransactionDao);

        StackPane content = new StackPane();
        DashboardView dashboardView = new DashboardView(inventoryService);
        InventoryView inventoryView = new InventoryView(inventoryService, categoryDao);
        OrdersView[] ordersViewHolder = new OrdersView[1]; // filled in below, referenced by the checkout callback

        CheckoutView checkoutView = new CheckoutView(inventoryService, orderService, () -> {
            // After a sale completes, every other screen needs to reflect it immediately.
            dashboardView.refresh();
            inventoryView.refresh();
            if (ordersViewHolder[0] != null) ordersViewHolder[0].refresh();
        });

        OrdersView ordersView = new OrdersView(orderDao, orderService, () -> {
            // After a cancellation, stock changed -- dashboard and inventory need to catch up too.
            dashboardView.refresh();
            inventoryView.refresh();
        });
        ordersViewHolder[0] = ordersView;

        content.getChildren().addAll(dashboardView, inventoryView, checkoutView, ordersView);
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
        Button ordersBtn = navButton("Orders", () -> {
            ordersView.refresh();
            showOnly(content, ordersView);
        });

        VBox nav = new VBox(8, dashboardBtn, inventoryBtn, checkoutBtn, ordersBtn);
        nav.getStyleClass().add("nav-sidebar");
        nav.setPadding(new Insets(20, 12, 20, 12));
        nav.setPrefWidth(170);
        for (Button b : java.util.List.of(dashboardBtn, inventoryBtn, checkoutBtn, ordersBtn)) {
            b.setMaxWidth(Double.MAX_VALUE);
        }

        BorderPane root = new BorderPane();
        root.setLeft(nav);
        root.setCenter(content);

        Scene scene = new Scene(root, 1150, 700);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        stage.setTitle("Clothing Store Inventory System");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> DatabaseManager.closeConnection());
        stage.show();
    }

    private Button navButton(String text, Runnable onClick) {
        Button b = new Button(text);
        b.getStyleClass().add("nav-button");
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
