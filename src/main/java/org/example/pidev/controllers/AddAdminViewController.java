package org.example.pidev.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.pidev.entities.User;
import org.example.pidev.services.UserService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AddAdminViewController implements Initializable {

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField phoneField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    private Stage stage;
    private final UserService userService = new UserService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Add input validation
        saveButton.setDisable(true);

        // Enable save button only when all fields are filled
        firstNameField.textProperty().addListener((observable, oldValue, newValue) -> validateFields());
        lastNameField.textProperty().addListener((observable, oldValue, newValue) -> validateFields());
        emailField.textProperty().addListener((observable, oldValue, newValue) -> validateFields());
        phoneField.textProperty().addListener((observable, oldValue, newValue) -> validateFields());
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> validateFields());
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void validateFields() {
        boolean isValid = !firstNameField.getText().isEmpty() &&
                !lastNameField.getText().isEmpty() &&
                !emailField.getText().isEmpty() &&
                !phoneField.getText().isEmpty() &&
                !passwordField.getText().isEmpty();
        saveButton.setDisable(!isValid);
    }

    @FXML
    private void handleSave() {
        try {
            // Create new admin user
            User newAdmin = new User();
            newAdmin.setName(firstNameField.getText());
            newAdmin.setLastName(lastNameField.getText());
            newAdmin.setEmail(emailField.getText());
            newAdmin.setPhone(Integer.parseInt(phoneField.getText()));
            newAdmin.setPassword(passwordField.getText());
            newAdmin.setRoleId(1); // 1 for admin role
            newAdmin.setCreatedAt(java.time.LocalDateTime.now()); // Set current date and time

            // Add to database
            userService.ajouter(newAdmin);

            // Show success message
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Admin added successfully!");
            alert.showAndWait();

            // Close the window
            stage.close();
        } catch (SQLException e) {
            // Show error message
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to add admin");
            alert.setContentText("An error occurred while adding the admin: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleCancel() {
        stage.close();
    }
}