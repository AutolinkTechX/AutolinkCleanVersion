package org.example.pidev.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import org.example.pidev.services.UserService;
import org.example.pidev.test.MainFX.TokenReceiver;

public class ResetPasswordController implements TokenReceiver {

    @FXML
    private Button cancelButton;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label messageLabel;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private Button resetButton;

    private String resetToken;

    @Override
    public void setToken(String token) {
        this.resetToken = token;
        // You can add additional logic here if needed when token is set
        System.out.println("Token received: " + token);
    }

    @FXML
    private void handleResetAction() {
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            messageLabel.setText("Please fill in all fields!");
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            messageLabel.setText("Passwords do not match!");
            return;
        }
        
        if (resetToken == null || resetToken.isEmpty()) {
            messageLabel.setText("Invalid reset token!");
            return;
        }
        
        UserService userService = new UserService();
        try {
            boolean success = userService.resetPassword(resetToken, newPassword);
            if (success) {
                messageLabel.setText("Password reset successfully!");
                // Optionally close the window after successful reset
                // ((Stage) resetButton.getScene().getWindow()).close();
            } else {
                messageLabel.setText("Failed to reset password. Token may be invalid/expired.");
            }
        } catch (Exception e) {
            messageLabel.setText("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancelAction() {
        ((Stage) cancelButton.getScene().getWindow()).close();
    }
}