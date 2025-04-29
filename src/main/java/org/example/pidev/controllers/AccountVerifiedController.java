package org.example.pidev.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import java.io.IOException;

import org.example.pidev.services.UserService;
import org.example.pidev.entities.User;
import org.example.pidev.utils.SessionManager;

import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import org.example.pidev.test.MainFX.TokenReceiver;

public class AccountVerifiedController implements TokenReceiver{

    private String verificationToken;

    @FXML
    private Hyperlink closeButton;

    @FXML
    private Button continueToAutolinkButton;

    @FXML
    private Label messageLabel;

    @Override
    public void setToken(String token) {
        this.verificationToken = token;
        // You can add additional logic here if needed when token is set
        System.out.println("Token received: " + token);
    }

    @FXML
    void handleClose(ActionEvent event) {
        try{
            Stage currentStage = (Stage) closeButton.getScene().getWindow();
            currentStage.close();

            Parent root = FXMLLoader.load(getClass().getResource("/DashboardLogin.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Autolink Login");
            stage.show();
    }catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleContinueToAutolink(ActionEvent event) {
        try {
            // 1. Verify the token again (optional but recommended)
            UserService userService = new UserService();
            boolean isValid = userService.verifyAccountToken(verificationToken);
            
            if (!isValid) {
                messageLabel.setText("Verification expired. Please try again.");
                return;
            }
    
            // 2. Get user details using the token
            User verifiedUser = userService.getUserByVerificationToken(verificationToken);
            
            if (verifiedUser == null) {
                messageLabel.setText("User not found. Please register again.");
                return;
            }
    
            // 3. Automatically authenticate the user
            User authenticatedUser = userService.authenticate(verifiedUser.getEmail(), verifiedUser.getPassword());
            SessionManager.setCurrentUser(authenticatedUser);
            
            if (authenticatedUser == null) {
                messageLabel.setText("Authentication failed. Please login manually.");
                return;
            }
    
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Error during authentication: " + e.getMessage());
        }
    }

    public void setVerificationToken(String token) {
        this.verificationToken = token;
    }
}
