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
import com.clothingstore.inventory.ui.Icons;
import com.clothingstore.inventory.ui.InventoryView;
import com.clothingstore.inventory.ui.LoginView;
import com.clothingstore.inventory.ui.OrdersView;
import com.clothingstore.inventory.ui.SalesView;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
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

    static final String APP_NAME = "EverTrace";

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

        stage.setTitle(APP_NAME + " \u2014 Clothing Store Inventory");
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

        Button dashboardBtn = navButton("Dashboard", "dashboard", "Ctrl+1");
        Button inventoryBtn = navButton("Inventory", "box", "Ctrl+2");
        Button checkoutBtn = navButton("Checkout", "cart", "Ctrl+3");
        Button ordersBtn = navButton("Orders", "receipt", "Ctrl+4");
        Button salesBtn = navButton("Sales", "chart", "Ctrl+5");
        List<Button> navButtons = List.of(dashboardBtn, inventoryBtn, checkoutBtn, ordersBtn, salesBtn);

        Consumer<Button> markActive = active -> {
            for (Button b : navButtons) {
                b.getStyleClass().remove("active");
                b.setAccessibleHelp(null);
            }
            active.getStyleClass().add("active");
            active.setAccessibleHelp("Current page");
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

        // ---- brand
        StackPane mark = new StackPane(Icons.styled("shirt", 18, "icon-white"));
        mark.getStyleClass().add("brand-mark");
        mark.setMinSize(34, 34);
        mark.setMaxSize(34, 34);
        Label brandName = new Label(APP_NAME);
        brandName.getStyleClass().add("brand-name");
        Label tagline = new Label("Clothing inventory");
        tagline.getStyleClass().add("brand-tagline");
        HBox brand = new HBox(10, mark, new VBox(0, brandName, tagline));
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setPadding(new Insets(0, 4, 10, 4));

        Label menuLabel = new Label("MENU");
        menuLabel.getStyleClass().add("nav-section");

        // ---- signed-in user
        Label initial = new Label(user.username().isEmpty() ? "?" : user.username().substring(0, 1).toUpperCase());
        initial.getStyleClass().add("avatar-text");
        StackPane avatar = new StackPane(initial);
        avatar.getStyleClass().add("avatar");
        avatar.setMinSize(32, 32);
        avatar.setMaxSize(32, 32);
        Label userName = new Label(user.username());
        userName.getStyleClass().add("user-name");
        Label role = new Label("Owner");
        role.getStyleClass().add("user-role");
        HBox userChip = new HBox(10, avatar, new VBox(0, userName, role));
        userChip.setAlignment(Pos.CENTER_LEFT);
        userChip.getStyleClass().add("user-chip");
        userChip.setAccessibleText("Signed in as " + user.username());

        Button changePasswordBtn = navButton("Change password", "key", null);
        changePasswordBtn.getStyleClass().add("small");
        changePasswordBtn.setOnAction(e ->
                new ChangePasswordDialog(authService, user).showAndWait().ifPresent(changed -> {
                    if (changed) {
                        Alert done = new Alert(Alert.AlertType.INFORMATION, "Your password was changed.");
                        done.setHeaderText(null);
                        done.initOwner(stage);
                        done.showAndWait();
                    }
                }));
        Button logoutBtn = navButton("Log out", "logout", null);
        logoutBtn.getStyleClass().add("small");
        logoutBtn.setOnAction(e -> showLogin());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox nav = new VBox(2);
        nav.getChildren().addAll(brand, menuLabel);
        nav.getChildren().addAll(navButtons);
        nav.getChildren().addAll(spacer, userChip, new Region() {{ setMinHeight(6); }}, changePasswordBtn, logoutBtn);
        nav.getStyleClass().add("nav-sidebar");
        nav.setPadding(new Insets(20, 12, 16, 12));
        nav.setPrefWidth(212);
        nav.setMinWidth(212);

        content.getStyleClass().add("app-content");

        BorderPane root = new BorderPane();
        root.setLeft(nav);
        root.setCenter(content);

        Scene scene = show(root, 1240, 780);
        // Ctrl+1..5 jump between screens (Cmd on macOS).
        for (int i = 0; i < navButtons.size(); i++) {
            Button target = navButtons.get(i);
            scene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.valueOf("DIGIT" + (i + 1)), KeyCombination.SHORTCUT_DOWN),
                    target::fire);
        }
        stage.setMinWidth(1040);
        stage.setMinHeight(660);
    }

    private Scene show(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.sizeToScene();
        stage.centerOnScreen();
        return scene;
    }

    private Button navButton(String text, String icon, String shortcut) {
        Button b = new Button(text);
        b.getStyleClass().add("nav-button");
        b.setMaxWidth(Double.MAX_VALUE);
        Icons.on(b, icon, 17);
        b.setGraphicTextGap(12);
        if (shortcut != null) b.setTooltip(new Tooltip(text + "  (" + shortcut + ")"));
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
