package org.example.pidev.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.example.pidev.entities.Entreprise;
import org.example.pidev.services.EntrepriseService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class EntreprisesViewController implements Initializable {

    @FXML
    private FlowPane entreprisesContainer;

    @FXML
    private ImageView addEntrepriseButton;

    private final EntrepriseService entrepriseService = new EntrepriseService();
    private ObservableList<Entreprise> entreprisesList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("EntreprisesViewController initialized");
        if (entreprisesContainer == null) {
            System.out.println("entreprisesContainer is null");
            return;
        }
        System.out.println("entreprisesContainer is not null");
        loadEntreprisesData();

        // Set up add entreprise button
        if (addEntrepriseButton != null) {
            addEntrepriseButton.setOnMouseClicked(event -> openAddEntrepriseWindow());
        } else {
            System.out.println("addEntrepriseButton is null");
        }
    }

    private void loadEntreprisesData() {
        try {
            System.out.println("Loading entreprises data...");
            if (entreprisesContainer == null) {
                System.out.println("Error: entreprisesContainer is null!");
                return;
            }
            
            entreprisesList = FXCollections.observableArrayList(entrepriseService.getAllEntreprises());
            System.out.println("Number of entreprises loaded: " + entreprisesList.size());
            
            if (entreprisesList.isEmpty()) {
                System.out.println("No entreprises found to display");
                // Show a message to the user
                Label noEntreprisesLabel = new Label("No entreprises found in the database");
                noEntreprisesLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 16px;");
                entreprisesContainer.getChildren().add(noEntreprisesLabel);
                return;
            }
            
            createEntrepriseCards();
        } catch (Exception e) {
            System.out.println("Error loading entreprises data: " + e.getMessage());
            e.printStackTrace();
            
            // Show error message to user
            Label errorLabel = new Label("Error loading entreprise data: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");
            entreprisesContainer.getChildren().add(errorLabel);
        }
    }

    private void createEntrepriseCards() {
        System.out.println("Creating entreprise cards...");
        entreprisesContainer.getChildren().clear(); // Clear any existing content
        
        for (Entreprise entreprise : entreprisesList) {
            if (entreprise == null) {
                System.out.println("Warning: Found null entreprise in list");
                continue;
            }
            
            System.out.println("Creating card for: " + entreprise.getCompanyName());
            VBox card = new VBox(10);
            card.setStyle("-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");
            card.setPadding(new Insets(15));
            card.setPrefWidth(300);
            card.setPrefHeight(250);
            
            // Store the entreprise object in the card's user data
            card.setUserData(entreprise);
            card.setAlignment(Pos.CENTER);

            // Create image container
            VBox imageContainer = new VBox();
            imageContainer.setAlignment(Pos.CENTER);
            
            // Create and configure ImageView
            ImageView entrepriseImageView = new ImageView();
            entrepriseImageView.setFitWidth(100);
            entrepriseImageView.setFitHeight(100);
            entrepriseImageView.setPreserveRatio(true);
            
            // Try to load the entreprise's image
            try {
                String imagePath = entreprise.getImagePath();
                if (imagePath != null && !imagePath.isEmpty()) {
                    Image image = new Image("file:" + imagePath);
                    entrepriseImageView.setImage(image);
                } else {
                    // Load logo.jpg as default image if no image path is provided
                    Image defaultImage = new Image(getClass().getResourceAsStream("/images/logo.jpg"));
                    entrepriseImageView.setImage(defaultImage);
                }
            } catch (Exception e) {
                System.out.println("Error loading image for entreprise: " + entreprise.getCompanyName());
                // Load logo.jpg as default image in case of error
                Image defaultImage = new Image(getClass().getResourceAsStream("/images/logo.jpg"));
                entrepriseImageView.setImage(defaultImage);
            }
            
            imageContainer.getChildren().add(entrepriseImageView);
            imageContainer.setAlignment(Pos.CENTER);

            // Entreprise information
            Label nameLabel = new Label(entreprise.getCompanyName());
            nameLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
            nameLabel.setAlignment(Pos.CENTER);
            Label emailLabel = new Label(entreprise.getEmail());
            emailLabel.setFont(Font.font("System", 14));
            emailLabel.setAlignment(Pos.CENTER);
            
            Label phoneLabel = new Label("Phone: " + entreprise.getPhone());
            phoneLabel.setFont(Font.font("System", 14));
            phoneLabel.setTextFill(Color.GRAY);
            phoneLabel.setAlignment(Pos.CENTER);

            // Action buttons
            HBox buttonContainer = new HBox(10);
            buttonContainer.setPadding(new Insets(10, 0, 0, 0));
            buttonContainer.setAlignment(Pos.CENTER);

            Button modifyButton = new Button("Modify");
            modifyButton.setStyle("-fx-background-color: rgb(202,138,98); -fx-text-fill: white; -fx-background-radius: 5px;");
            modifyButton.setOnAction(e -> handleModifyEntreprise(entreprise));

            Button deleteButton = new Button("Delete");
            deleteButton.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white; -fx-background-radius: 5px;");
            deleteButton.setOnAction(e -> handleDeleteEntreprise(card));

            buttonContainer.getChildren().addAll(modifyButton, deleteButton);

            card.getChildren().addAll(imageContainer, nameLabel, emailLabel, phoneLabel, buttonContainer);
            entreprisesContainer.getChildren().add(card);
            System.out.println("Card added to container");
        }
        System.out.println("All cards created");
    }

    @FXML
    private void handleModifyEntreprise(Entreprise entreprise) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifyEntrepriseView.fxml"));
            Parent root = loader.load();
            
            ModifyEntrepriseViewController controller = loader.getController();
            Stage stage = new Stage();
            controller.setStage(stage);
            controller.setEntrepriseToModify(entreprise);
            
            stage.setTitle("Modify Entreprise");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            // Refresh the entreprise list after modification
            loadEntreprisesData();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open modify window");
            alert.setContentText("An error occurred while opening the modify window: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void handleDeleteEntreprise(VBox card) {
        System.out.println("Delete entreprise clicked");
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Entreprise");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to delete this entreprise?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Get the entreprise's ID from the card's user data
                    Entreprise entreprise = (Entreprise) card.getUserData();
                    if (entreprise != null) {
                        // Delete from database
                        entrepriseService.supprimer(entreprise.getId());
                        // Remove from UI
                        entreprisesContainer.getChildren().remove(card);
                        System.out.println("Entreprise deleted successfully");
                    }
                } catch (SQLException e) {
                    System.out.println("Error deleting entreprise: " + e.getMessage());
                    e.printStackTrace();
                    // Show error alert
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Error");
                    errorAlert.setHeaderText("Failed to delete entreprise");
                    errorAlert.setContentText("An error occurred while deleting the entreprise. Please try again.");
                    errorAlert.showAndWait();
                }
            } else {
                System.out.println("User cancelled deletion");
            }
        });
    }

    private void openAddEntrepriseWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AddEntrepriseView.fxml"));
            Parent root = loader.load();
            AddEntrepriseViewController controller = loader.getController();
            
            Stage stage = new Stage();
            stage.setTitle("Add New Entreprise");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            
            controller.setStage(stage);
            stage.showAndWait();
            
            // Refresh the entreprise list after adding
            loadEntreprisesData();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open add window");
            alert.setContentText("An error occurred while opening the add window: " + e.getMessage());
            alert.showAndWait();
        }
    }
} 