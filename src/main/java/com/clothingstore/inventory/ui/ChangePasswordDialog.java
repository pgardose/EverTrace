package com.clothingstore.inventory.ui;

import com.clothingstore.inventory.dao.DataAccessException;
import com.clothingstore.inventory.model.User;
import com.clothingstore.inventory.service.AuthException;
import com.clothingstore.inventory.service.AuthService;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

import java.util.Arrays;

/** Result is true when the password was changed, null/false when cancelled. */
public class ChangePasswordDialog extends Dialog<Boolean> {

    public ChangePasswordDialog(AuthService authService, User user) {
        setTitle("Change Password");

        PasswordField currentField = new PasswordField();
        PasswordField newField = new PasswordField();
        PasswordField confirmField = new PasswordField();

        Label error = new Label();
        error.setTextFill(Color.web("#dc2626"));
        error.setWrapText(true);
        error.setMaxWidth(300);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("Current password:"), currentField);
        grid.addRow(1, new Label("New password:"), newField);
        grid.addRow(2, new Label("Confirm new password:"), confirmField);
        grid.add(error, 0, 3, 2, 1);

        getDialogPane().setContent(grid);
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
