package org.example.pidev.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.example.pidev.services.ApiKeyManager;

public class ApiConfigController {
    @FXML private TextField apiKeyField;
    @FXML private Label errorMessageLabel; // Ajout de la déclaration du label

    private final ApiKeyManager apiKeyManager = new ApiKeyManager();

    @FXML
    public void initialize() {
        // Initialise le message d'erreur
        errorMessageLabel.setVisible(false);

        // Charge la clé existante si elle existe
        String existingKey = apiKeyManager.getApiKey();
        if (existingKey != null) {
            apiKeyField.setText(existingKey);
            // Cache le message si la clé est valide
            if (apiKeyManager.isApiKeyValid()) {
                errorMessageLabel.setVisible(false);
            }
        } else {
            // Affiche le message si aucune clé n'est configurée
            errorMessageLabel.setVisible(true);
            errorMessageLabel.setText("Configuration requise: Veuillez entrer une clé API valide");
        }
    }

    @FXML
    private void handleSaveApiKey() {
        String apiKey = apiKeyField.getText().trim();
        System.out.println("Tentative de sauvegarde de la clé: " + apiKey);

        try {
            apiKeyManager.configureApiKey(apiKey);
            System.out.println("Clé valide et sauvegardée");
            errorMessageLabel.setVisible(false); // Cache le message après succès
            showAlert("Succès", "Clé API configurée avec succès", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            System.err.println("Erreur de configuration: " + e.getMessage());
            errorMessageLabel.setVisible(true);
            errorMessageLabel.setText("Erreur: " + e.getMessage());
            showAlert("Erreur", "Échec: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}