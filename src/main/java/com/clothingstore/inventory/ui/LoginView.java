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

        Label title = new Label("Clothing Store Inventory");
        title.getStyleClass().add("login-title");

        Label subtitle = new Label(setupMode
                ? "Welcome! Create the owner account to get started."
                : "Sign in to continue");
        subtitle.getStyleClass().add("login-subtitle");
        subtitle.setWrapText(true);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        // "Show password" swaps in a plain TextField sharing the same text.
        TextField passwordPlain = new TextField();
        passwordPlain.setPromptText("Password");
        passwordPlain.textProperty().bindBidirectional(passwordField.textProperty());
        CheckBox showPassword = new CheckBox("Show password");
        passwordPlain.visibleProperty().bind(showPassword.selectedProperty());
        passwordPlain.managedProperty().bind(showPassword.selectedProperty());
        passwordField.visibleProperty().bind(showPassword.selectedProperty().not());
        passwordField.managedProperty().bind(showPassword.selectedProperty().not());

        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Confirm password");

        Label hint = new Label("At least " + AuthService.MIN_PASSWORD_LENGTH
                + " characters, with a letter and a number.");
        hint.getStyleClass().add("login-hint");
        hint.setWrapText(true);

        Label error = new Label();
        error.getStyleClass().add("login-error");
        error.setWrapText(true);
        error.visibleProperty().bind(error.textProperty().isNotEmpty());
        error.managedProperty().bind(error.textProperty().isNotEmpty());

        Button submit = new Button(setupMode ? "Create account" : "Sign in");
        submit.setDefaultButton(true);
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.getStyleClass().add("login-button");

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

        VBox card = new VBox(12, title, subtitle, usernameField, passwordField, passwordPlain, showPassword);
        if (setupMode) {
            card.getChildren().addAll(confirmField, hint);
        }
        card.getChildren().addAll(error, submit);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(400);
        card.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        card.setAlignment(Pos.CENTER_LEFT);

        getStyleClass().add("login-root");
        getChildren().add(card);
        setAlignment(Pos.CENTER);

        Platform.runLater(usernameField::requestFocus);
    }
}
