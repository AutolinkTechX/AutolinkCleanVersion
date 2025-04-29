package org.example.pidev.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;
import javafx.scene.layout.AnchorPane;
import org.example.pidev.entities.User;
import javafx.scene.Parent;
import javafx.scene.Scene;


public class SignUpController {
    private User userData;


    @FXML
    private Label confirmPasswordErrorLabel;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label emailErrorLabel;

    @FXML
    private TextField emailField;

    @FXML
    private Label errorLabel;

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private Hyperlink loginLink;

    @FXML
    private Button nextButton;

    @FXML
    private Label passwordErrorLabel;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button showPasswordButton;

    @FXML
    private TextField phoneField;

    @FXML
    private Label phoneErrorLabel;

    @FXML
    void handleNextButton(ActionEvent event) {
        // Reset all error labels
        resetErrorLabels();
        
        // Validate all fields
        if (validateFields()) {
            try {
                // Load only the camera view content
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/CameraView.fxml"));
                AnchorPane cameraView = loader.load();
                
                // Get reference to the right pane (assuming it's the second child in main container)
                AnchorPane rightPane = (AnchorPane) ((AnchorPane)nextButton.getScene().getRoot()).getChildren().get(1);
                
                // Clear the right pane and add the camera view
                rightPane.getChildren().setAll(cameraView);
                
                // Optional: Pass user data to camera controller
                CameraViewController cameraController = loader.getController();
                cameraController.initData(getUserData());
                
            } catch (IOException e) {
                e.printStackTrace();
                errorLabel.setText("Failed to load camera view");
                errorLabel.setVisible(true);
            }
         }
    }

    private void resetErrorLabels() {
        emailErrorLabel.setVisible(false);
        passwordErrorLabel.setVisible(false);
        confirmPasswordErrorLabel.setVisible(false);
        errorLabel.setVisible(false);
    }
    
    private boolean validateFields() {
        boolean isValid = true;
        
        // Validate first name
        if (firstNameField.getText().isEmpty()) {
            // You might want to add a firstNameErrorLabel
            isValid = false;
        }
        
        // Validate last name
        if (lastNameField.getText().isEmpty()) {
            // You might want to add a lastNameErrorLabel
            isValid = false;
        }
        
        // Validate email
        if (emailField.getText().isEmpty() || !isValidEmail(emailField.getText())) {
            emailErrorLabel.setText("Please enter a valid email address");
            emailErrorLabel.setVisible(true);
            isValid = false;
        }
        
        // Validate password
        if (passwordField.getText().isEmpty() || passwordField.getText().length() < 6) {
            passwordErrorLabel.setText("Password must be at least 6 characters");
            passwordErrorLabel.setVisible(true);
            isValid = false;
        }
        
        // Validate password confirmation
        if (!confirmPasswordField.getText().equals(passwordField.getText())) {
            confirmPasswordErrorLabel.setText("Passwords do not match");
            confirmPasswordErrorLabel.setVisible(true);
            isValid = false;
        }

        if(phoneField.getText().isEmpty() || !isValidPhone(phoneField.getText()) )
        phoneErrorLabel.setText("Phone must contain 8 numbers");
        phoneErrorLabel.setVisible(true);
        return isValid;
    }
    
    private boolean isValidEmail(String email) {
        // Simple email validation regex
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }

    private boolean isValidPhone(String phone) {
        String digitsOnly = phone.replaceAll("[^+\\d]", "");
        return digitsOnly.matches("^(\\+|00)?\\d{8}$");
    }

    public User getUserData() {
        // Create or return existing user data
        if (userData == null) {
            // Example: Get data from your form fields
            userData = new User(
                firstNameField.getText(),
                lastNameField.getText(),
                Integer.parseInt(phoneField.getText()),
                emailField.getText(),
                passwordField.getText()
            );
        }
        return userData;
    }

    @FXML
    void handleLoginLink(ActionEvent event) {
        try {
            // Close current login window
            Stage currentStage = (Stage) nextButton.getScene().getWindow();
            currentStage.close();

            // Load SignUp view
            Parent root = FXMLLoader.load(getClass().getResource("/DashboardLogin.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Autolink Login");  
            stage.show();
        } catch (IOException e) {
            showError("Failed to load sign up view");
            e.printStackTrace();
        }
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setStyle("-fx-text-fill: #d9534f;"); // Red color for errors
    }
}
