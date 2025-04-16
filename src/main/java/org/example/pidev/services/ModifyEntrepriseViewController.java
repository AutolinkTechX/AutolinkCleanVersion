package org.example.pidev.services;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.pidev.entities.Entreprise;

import java.net.URL;
import java.util.ResourceBundle;

public class ModifyEntrepriseViewController implements Initializable {

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
    private CheckBox supplierCheckBox;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    private Stage stage;
    private Entreprise entrepriseToModify;
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

    public void setEntrepriseToModify(Entreprise entreprise) {
        this.entrepriseToModify = entreprise;
        populateFields();
    }

    private void populateFields() {
        if (entrepriseToModify != null) {
            companyNameField.setText(entrepriseToModify.getCompanyName());
            emailField.setText(entrepriseToModify.getEmail());
            phoneField.setText(entrepriseToModify.getPhone());
            taxCodeField.setText(entrepriseToModify.getTaxCode());
            fieldField.setText(entrepriseToModify.getField());
            supplierCheckBox.setSelected(entrepriseToModify.getSupplier());
        }
    }

    private void handleSave() {
        try {
            // Update the entreprise object with the new values
            entrepriseToModify.setCompanyName(companyNameField.getText());
            entrepriseToModify.setEmail(emailField.getText());
            entrepriseToModify.setPhone(phoneField.getText());
            entrepriseToModify.setTaxCode(taxCodeField.getText());
            entrepriseToModify.setField(fieldField.getText());
            entrepriseToModify.setSupplier(supplierCheckBox.isSelected());

            // Save the changes to the database
            entrepriseService.modifier(entrepriseToModify);

            // Close the window
            stage.close();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to save changes");
            alert.setContentText("An error occurred while saving the changes: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void handleCancel() {
        stage.close();
    }
} 