package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.dao.DataAccessException;
import com.clothingstore.inventory.model.User;
import com.clothingstore.inventory.service.AuthException;
import com.clothingstore.inventory.service.AuthService;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.Arrays;

/** Result is true when the password was changed, null/false when cancelled. */
public class ChangePasswordDialog extends Dialog<Boolean> {

    public ChangePasswordDialog(AuthService authService, User user) {
        setTitle("Change password");
        Ui.style(this);

        PasswordField currentField = new PasswordField();
        PasswordField newField = new PasswordField();
        PasswordField confirmField = new PasswordField();

        Label error = new Label();
        error.getStyleClass().add("form-error");
        error.setWrapText(true);
        error.setMaxWidth(Double.MAX_VALUE);
        error.visibleProperty().bind(error.textProperty().isNotEmpty());
        error.managedProperty().bind(error.visibleProperty());

        Label heading = new Label("Change password");
        heading.getStyleClass().add("dialog-title");
        Label sub = new Label("Signed in as " + user.username() + ".");
        sub.getStyleClass().add("field-hint");
        Label hint = new Label("At least " + AuthService.MIN_PASSWORD_LENGTH + " characters, with a letter and a number.");
        hint.getStyleClass().add("field-hint");
        VBox newBox = Ui.field("New password", newField);
        newBox.getChildren().add(hint);

        VBox form = new VBox(14, new VBox(4, heading, sub), error,
                Ui.field("Current password", currentField), newBox, Ui.field("Confirm new password", confirmField));
        form.setPadding(new Insets(20, 20, 8, 20));
        form.setPrefWidth(360);
        javafx.application.Platform.runLater(currentField::requestFocus);

        getDialogPane().setContent(form);
        ButtonType saveType = new ButtonType("Change password", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        // Run the change when Save is clicked; consume the event (dialog stays open) on any problem.
        Button saveButton = (Button) getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            char[] current = currentField.getText().toCharArray();
            char[] next = newField.getText().toCharArray();
            char[] confirm = confirmField.getText().toCharArray();
            try {
                authService.changePassword(user, current, next, confirm);
            } catch (AuthException ex) {
                error.setText(ex.getMessage());
                event.consume();
                javafx.application.Platform.runLater(() -> getDialogPane().getScene().getWindow().sizeToScene());
            } catch (DataAccessException ex) {
                error.setText("Couldn't save the new password. Please try again.");
                event.consume();
            } finally {
                Arrays.fill(current, '\0');
                Arrays.fill(next, '\0');
                Arrays.fill(confirm, '\0');
            }
        });

        setResultConverter(buttonType -> buttonType == saveType);
    }
}
