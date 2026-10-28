package com.clothingstore.inventory;

import atlantafx.base.theme.CupertinoLight;
import com.clothingstore.inventory.dao.*;
import com.clothingstore.inventory.model.User;
import com.clothingstore.inventory.service.AuthService;
import com.clothingstore.inventory.service.InventoryService;
import com.clothingstore.inventory.service.OrderService;
import com.clothingstore.inventory.service.SalesService;
import com.clothingstore.inventory.ui.ChangePasswordDialog;
import com.clothingstore.inventory.ui.CheckoutView;
import com.clothingstore.inventory.ui.DashboardView;
import com.clothingstore.inventory.ui.InventoryView;
import com.clothingstore.inventory.ui.LoginView;
import com.clothingstore.inventory.ui.OrdersView;
import com.clothingstore.inventory.ui.SalesView;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Consumer;

/**
 * Entry point. The app opens on the sign-in screen (or, on the very first
 * launch, "create the owner account"), then shows the main window.
 */
public class App extends Application {

    private Stage stage;

    private ItemDao itemDao;
    private CategoryDao categoryDao;
    private OrderDao orderDao;
    private AuthService authService;
    private InventoryService inventoryService;
    private OrderService orderService;
    private SalesService salesService;

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        // AtlantaFX's flat, modern theme -- the JavaFX equivalent of FlatLaf.
        // CupertinoLight matches the "Mac Light" look from the original spec;
        // swap for PrimerLight for a cleaner GitHub-style look instead.
        Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());

        DatabaseManager.initializeSchema();

        itemDao = new ItemDao();
        categoryDao = new CategoryDao();
        orderDao = new OrderDao();
        StockTransactionDao stockTransactionDao = new StockTransactionDao();

        authService = new AuthService(new UserDao());
        inventoryService = new InventoryService(itemDao, stockTransactionDao);
        orderService = new OrderService(itemDao, orderDao, stockTransactionDao);
        salesService = new SalesService(new SalesDao());

        stage.setTitle("Clothing Store Inventory System");
        showLogin();
        stage.show();
    }

    @Override
    public void stop() {
        DatabaseManager.closeConnection();
    }

    // ------------------------------------------------------------ screens

    private void showLogin() {
        stage.setMinWidth(0);
        stage.setMinHeight(0);
        show(new LoginView(authService, this::showMain), 520, 700);
    }

    private void showMain(User user) {
        StackPane content = new StackPane();
        DashboardView dashboardView = new DashboardView(inventoryService, salesService);
        InventoryView inventoryView = new InventoryView(inventoryService, categoryDao);
        SalesView salesView = new SalesView(salesService);
        OrdersView[] ordersViewHolder = new OrdersView[1]; // filled in below; the callbacks need it

        // A sale or a cancellation changes stock AND sales, so every screen catches up.
        Runnable refreshAll = () -> {
            dashboardView.refresh();
            inventoryView.refresh();
            salesView.refresh();
            if (ordersViewHolder[0] != null) ordersViewHolder[0].refresh();
        };

        CheckoutView checkoutView = new CheckoutView(inventoryService, orderService, refreshAll);
        OrdersView ordersView = new OrdersView(orderDao, orderService, refreshAll);
        ordersViewHolder[0] = ordersView;

        content.getChildren().addAll(dashboardView, inventoryView, checkoutView, ordersView, salesView);
        showOnly(content, dashboardView);

        Button dashboardBtn = navButton("Dashboard");
        Button inventoryBtn = navButton("Inventory");
        Button checkoutBtn = navButton("Checkout");
        Button ordersBtn = navButton("Orders");
        Button salesBtn = navButton("Sales");
        List<Button> navButtons = List.of(dashboardBtn, inventoryBtn, checkoutBtn, ordersBtn, salesBtn);

        Consumer<Button> markActive = active -> {
            for (Button b : navButtons) b.getStyleClass().remove("active");
            active.getStyleClass().add("active");
        };
        dashboardBtn.setOnAction(e -> {
            dashboardView.refresh();
            showOnly(content, dashboardView);
            markActive.accept(dashboardBtn);
        });
        inventoryBtn.setOnAction(e -> {
            inventoryView.refresh();
            showOnly(content, inventoryView);
            markActive.accept(inventoryBtn);
        });
        checkoutBtn.setOnAction(e -> {
            checkoutView.refreshCatalog();
            showOnly(content, checkoutView);
            markActive.accept(checkoutBtn);
        });
        ordersBtn.setOnAction(e -> {
            ordersView.refresh();
            showOnly(content, ordersView);
            markActive.accept(ordersBtn);
        });
        salesBtn.setOnAction(e -> {
            salesView.refresh();
            showOnly(content, salesView);
            markActive.accept(salesBtn);
        });
        markActive.accept(dashboardBtn);

        Label signedInAs = new Label("Signed in as " + user.username());
        signedInAs.getStyleClass().add("nav-footer-label");
        Button changePasswordBtn = navButton("Change password");
        changePasswordBtn.setOnAction(e ->
                new ChangePasswordDialog(authService, user).showAndWait().ifPresent(changed -> {
                    if (changed) {
                        new Alert(Alert.AlertType.INFORMATION, "Your password was changed.").showAndWait();
                    }
                }));
        Button logoutBtn = navButton("Log out");
        logoutBtn.setOnAction(e -> showLogin());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox nav = new VBox(8);
        nav.getChildren().addAll(navButtons);
        nav.getChildren().addAll(spacer, signedInAs, changePasswordBtn, logoutBtn);
        nav.getStyleClass().add("nav-sidebar");
        nav.setPadding(new Insets(20, 12, 20, 12));
        nav.setPrefWidth(170);

        BorderPane root = new BorderPane();
        root.setLeft(nav);
        root.setCenter(content);

        show(root, 1150, 720);
        stage.setMinWidth(980);
        stage.setMinHeight(620);
    }

    private Scene show(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.sizeToScene();
        stage.centerOnScreen();
        return scene;
    }

    private Button navButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("nav-button");
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    private void showOnly(StackPane stack, Node visible) {
        for (Node node : stack.getChildren()) {
            node.setVisible(node == visible);
            node.setManaged(node == visible);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
