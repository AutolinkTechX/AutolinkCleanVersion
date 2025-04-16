package org.example.pidev.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.pidev.entities.User;
import org.example.pidev.services.UserService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class ModifyClientViewController implements Initializable {

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
    private User clientToModify;
    private final UserService userService = new UserService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Add input validation
        saveButton.setDisable(true);
        
        // Enable save button only when required fields are filled
        firstNameField.textProperty().addListener((observable, oldValue, newValue) -> validateFields());
        lastNameField.textProperty().addListener((observable, oldValue, newValue) -> validateFields());
        emailField.textProperty().addListener((observable, oldValue, newValue) -> validateFields());
        phoneField.textProperty().addListener((observable, oldValue, newValue) -> validateFields());
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setClientToModify(User client) {
        this.clientToModify = client;
        // Populate fields with current client data
        firstNameField.setText(client.getName());
        lastNameField.setText(client.getLastName());
        emailField.setText(client.getEmail());
        phoneField.setText(String.valueOf(client.getPhone()));
    }

    private void validateFields() {
        boolean isValid = !firstNameField.getText().isEmpty() &&
                         !lastNameField.getText().isEmpty() &&
                         !emailField.getText().isEmpty() &&
                         !phoneField.getText().isEmpty();
        saveButton.setDisable(!isValid);
    }

    @FXML
    private void handleSave() {
        try {
            // Update client data
            clientToModify.setName(firstNameField.getText());
            clientToModify.setLastName(lastNameField.getText());
            clientToModify.setEmail(emailField.getText());
            clientToModify.setPhone(Integer.parseInt(phoneField.getText()));
            
            // Only update password if a new one is provided
            String newPassword = passwordField.getText();
            if (!newPassword.isEmpty()) {
                clientToModify.setPassword(newPassword);
            }
            // If password is empty, keep the existing password (don't update it)

            // Update in database
            userService.modifier(clientToModify);

            // Show success message
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Client updated successfully!");
            alert.showAndWait();

            // Close the window
            stage.close();
        } catch (SQLException e) {
            // Show error message
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to update client");
            alert.setContentText("An error occurred while updating the client: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleCancel() {
        stage.close();
    }
} 