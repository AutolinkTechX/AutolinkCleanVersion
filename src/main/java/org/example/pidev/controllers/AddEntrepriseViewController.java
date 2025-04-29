package org.example.pidev.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.pidev.entities.Entreprise;
import org.example.pidev.services.EntrepriseService;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class AddEntrepriseViewController implements Initializable {

    @FXML
    private TextField companyNameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextField taxCodeField;

    @FXML
    private TextField fieldField;

    @FXML
    private TextField passwordField;

    @FXML
    private CheckBox supplierCheckBox;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    private Stage stage;
    private final EntrepriseService entrepriseService = new EntrepriseService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set up button actions
        saveButton.setOnAction(event -> handleSave());
        cancelButton.setOnAction(event -> handleCancel());
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void handleSave() {
        try {
            // Validate required fields
            if (companyNameField.getText().isEmpty() || emailField.getText().isEmpty() || 
                phoneField.getText().isEmpty() || passwordField.getText().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Missing Required Fields");
                alert.setContentText("Please fill in all required fields.");
                alert.showAndWait();
                return;
            }

            // Create new entreprise object
            Entreprise newEntreprise = new Entreprise();
            newEntreprise.setCompanyName(companyNameField.getText());
            newEntreprise.setEmail(emailField.getText());
            newEntreprise.setPhone(phoneField.getText());
            newEntreprise.setTaxCode(taxCodeField.getText());
            newEntreprise.setField(fieldField.getText());
            newEntreprise.setPassword(passwordField.getText());
            newEntreprise.setSupplier(supplierCheckBox.isSelected());

            // Save to database
            entrepriseService.ajouter(newEntreprise);

            // Close the window
            stage.close();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to save new entreprise");
            alert.setContentText("An error occurred while saving the new entreprise: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void handleCancel() {
        stage.close();
    }
} 