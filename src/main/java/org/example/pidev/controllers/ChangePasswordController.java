package org.example.pidev.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import org.example.pidev.entities.User;
import org.example.pidev.services.UserService;
import org.example.pidev.utils.AlertUtils;

public class ChangePasswordController {

    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;

    private User currentUser;
    private final UserService userService = new UserService();

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    @FXML
    private void handleChangePassword() {
        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        try {
            // Verify current password
            if (!userService.verifyPassword(currentUser, currentPasswordField.getText())) {
                errorLabel.setText("Current password is incorrect");
                return;
            }

            // Update password
            boolean success = userService.changePassword(
                currentUser, 
                newPasswordField.getText()
            );

            if (success) {
                AlertUtils.showInformationAlert("Success", "Password changed successfully");
                closeDialog();
            } else {
                errorLabel.setText("Failed to change password");
            }
        } catch (Exception e) {
            errorLabel.setText("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private boolean validateInputs() {
        if (currentPasswordField.getText().isEmpty()) {
            errorLabel.setText("Current password is required");
            return false;
        }

        if (newPasswordField.getText().isEmpty()) {
            errorLabel.setText("New password is required");
            return false;
        }

        if (!newPasswordField.getText().equals(confirmPasswordField.getText())) {
            errorLabel.setText("New passwords don't match");
            return false;
        }

        if (newPasswordField.getText().length() < 6) {
            errorLabel.setText("Password must be at least 6 characters");
            return false;
        }

        errorLabel.setText("");
        return true;
    }

    private void closeDialog() {
        Stage stage = (Stage) currentPasswordField.getScene().getWindow();
        stage.close();
    }
}