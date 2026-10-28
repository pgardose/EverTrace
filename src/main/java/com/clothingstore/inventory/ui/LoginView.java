package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.dao.DataAccessException;
import com.clothingstore.inventory.model.User;
import com.clothingstore.inventory.service.AuthException;
import com.clothingstore.inventory.service.AuthService;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Sign-in screen. On the very first launch (no accounts yet) the same screen
 * turns into "create the owner account" -- there is no default password to
 * remember or leave unchanged.
 */
public class LoginView extends StackPane {

    public LoginView(AuthService authService, Consumer<User> onSuccess) {
        boolean setupMode = authService.isSetupRequired();

        StackPane mark = new StackPane(Icons.styled("shirt", 22, "icon-white"));
        mark.getStyleClass().add("brand-mark");
        mark.setMinSize(44, 44);
        mark.setMaxSize(44, 44);

        Label title = new Label(setupMode ? "Set up EverTrace" : "Welcome back");
        title.getStyleClass().add("login-title");

        Label subtitle = new Label(setupMode
                ? "Create the owner account for this store. You'll use it to sign in from now on."
                : "Sign in to manage your store's inventory and sales.");
        subtitle.getStyleClass().add("login-subtitle");
        subtitle.setWrapText(true);

        TextField usernameField = new TextField();
        usernameField.setPromptText(setupMode ? "e.g. maria" : "");

        PasswordField passwordField = new PasswordField();

        // "Show password" swaps in a plain TextField sharing the same text.
        TextField passwordPlain = new TextField();
        passwordPlain.setAccessibleText("Password");
        passwordPlain.textProperty().bindBidirectional(passwordField.textProperty());
        CheckBox showPassword = new CheckBox("Show password");
        passwordPlain.visibleProperty().bind(showPassword.selectedProperty());
        passwordPlain.managedProperty().bind(showPassword.selectedProperty());
        passwordField.visibleProperty().bind(showPassword.selectedProperty().not());
        passwordField.managedProperty().bind(showPassword.selectedProperty().not());

        PasswordField confirmField = new PasswordField();

        Label hint = new Label("At least " + AuthService.MIN_PASSWORD_LENGTH
                + " characters, with a letter and a number.");
        hint.getStyleClass().add("login-hint");
        hint.setWrapText(true);

        Label error = new Label();
        error.getStyleClass().add("login-error");
        error.setMaxWidth(Double.MAX_VALUE);
        error.setWrapText(true);
        error.visibleProperty().bind(error.textProperty().isNotEmpty());
        error.managedProperty().bind(error.textProperty().isNotEmpty());

        Button submit = new Button(setupMode ? "Create account" : "Sign in");
        submit.setDefaultButton(true);
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.getStyleClass().addAll("accent", "large");

        submit.setOnAction(e -> {
            error.setText("");
            char[] password = passwordField.getText().toCharArray();
            char[] confirm = confirmField.getText().toCharArray();
            try {
                User user = setupMode
                        ? authService.registerFirstUser(usernameField.getText(), password, confirm)
                        : authService.login(usernameField.getText(), password);
                onSuccess.accept(user);
            } catch (AuthException ex) {
                error.setText(ex.getMessage());
                passwordField.clear();
                confirmField.clear();
            } catch (DataAccessException ex) {
                error.setText("Couldn't reach the database. Please close and reopen the app.");
            } finally {
                Arrays.fill(password, '\0');
                Arrays.fill(confirm, '\0');
            }
        });

        // Visible labels above each field: placeholder text disappears as soon as
        // you type, so it can't be the only label.
        VBox passwordBox = Ui.field("Password", passwordField);
        Label passwordLabel = (Label) passwordBox.getChildren().get(0);
        showPassword.selectedProperty().addListener((o, was, isShown) ->
                passwordLabel.setLabelFor(isShown ? passwordPlain : passwordField));
        passwordBox.getChildren().add(passwordPlain);

        VBox heading = new VBox(6, title, subtitle);
        heading.setPadding(new javafx.geometry.Insets(6, 0, 6, 0));

        VBox card = new VBox(14, mark, heading, Ui.field("Username", usernameField), passwordBox);
        if (setupMode) {
            VBox confirmBox = Ui.field("Confirm password", confirmField);
            confirmBox.getChildren().add(hint);
            card.getChildren().add(confirmBox);
        }
        card.getChildren().addAll(showPassword, error, submit);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(380);
        card.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        card.setAlignment(Pos.CENTER_LEFT);

        Label footer = new Label("Your data is stored only on this computer.");
        footer.getStyleClass().add("login-footer");
        VBox column = new VBox(16, card, footer);
        column.setAlignment(Pos.CENTER);
        column.setMaxWidth(380);
        column.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        getStyleClass().add("login-root");
        getChildren().add(column);
        setAlignment(Pos.CENTER);

        Platform.runLater(usernameField::requestFocus);
    }
}
