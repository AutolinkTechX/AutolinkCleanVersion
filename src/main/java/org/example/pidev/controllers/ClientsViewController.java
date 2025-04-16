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
import org.example.pidev.entities.User;
import org.example.pidev.services.UserService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class ClientsViewController implements Initializable {

    @FXML
    private FlowPane clientsContainer;

    @FXML
    private ImageView addClientButton;

    private final UserService userService = new UserService();
    private ObservableList<User> clientsList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("ClientsViewController initialized");
        if (clientsContainer == null) {
            System.out.println("clientsContainer is null");
            return;
        }
        System.out.println("clientsContainer is not null");
        loadClientsData();

        // Set up add client button
        if (addClientButton != null) {
            addClientButton.setOnMouseClicked(event -> openAddClientWindow());
        } else {
            System.out.println("addClientButton is null");
        }
    }

    private void loadClientsData() {
        try {
            System.out.println("Loading clients data...");
            clientsList = FXCollections.observableArrayList(userService.getAllClients());
            System.out.println("Number of clients loaded: " + clientsList.size());
            createClientCards();
        } catch (SQLException e) {
            System.out.println("Error loading clients: " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load clients");
            alert.setContentText("An error occurred while loading clients: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void createClientCards() {
        clientsContainer.getChildren().clear();
        
        if (clientsList.isEmpty()) {
            Label noClientsLabel = new Label("No clients found");
            noClientsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: gray;");
            clientsContainer.getChildren().add(noClientsLabel);
            return;
        }

        for (User client : clientsList) {
            VBox card = createClientCard(client);
            clientsContainer.getChildren().add(card);
        }
    }

    private VBox createClientCard(User client) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");
        card.setPrefWidth(300);
        card.setPrefHeight(250);
        card.setAlignment(Pos.TOP_CENTER);
        card.setUserData(client);

        // Image container
        HBox imageContainer = new HBox();
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.setPrefHeight(100);

        ImageView userImageView = new ImageView();
        userImageView.setFitWidth(100);
        userImageView.setFitHeight(100);
        userImageView.setPreserveRatio(true);
        
        try {
            String image_path = client.getImage_path();
            if (image_path != null && !image_path.isEmpty()) {
                Image image = new Image("file:" + image_path);
                userImageView.setImage(image);
            } else {
                // Load default image if no image path is provided
                Image defaultImage = new Image(getClass().getResourceAsStream("/images/logo.jpg"));
                userImageView.setImage(defaultImage);
            }
        } catch (Exception e) {
            System.out.println("Error loading image for client: " + client.getName());
            // Load default image in case of error
            Image defaultImage = new Image(getClass().getResourceAsStream("/images/logo.jpg"));
            userImageView.setImage(defaultImage);
        }
        
        imageContainer.getChildren().add(userImageView);

        // Client information
        Label nameLabel = new Label(client.getName() + " " + client.getLastName());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        
        Label emailLabel = new Label(client.getEmail());
        emailLabel.setFont(Font.font("System", 14));
        
        Label phoneLabel = new Label("Phone: " + client.getPhone());
        phoneLabel.setFont(Font.font("System", 14));
        phoneLabel.setTextFill(Color.GRAY);

        // Action buttons
        HBox buttonContainer = new HBox(10);
        buttonContainer.setPadding(new Insets(10, 0, 0, 0));

        Button modifyButton = new Button("Modify");
        modifyButton.setStyle("-fx-background-color: rgb(202,138,98); -fx-text-fill: white; -fx-background-radius: 5px;");
        modifyButton.setOnAction(e -> handleModifyClient(client));

        Button deleteButton = new Button("Delete");
        deleteButton.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white; -fx-background-radius: 5px;");
        deleteButton.setOnAction(e -> handleDeleteClient(card));

        buttonContainer.getChildren().addAll(modifyButton, deleteButton);
        buttonContainer.setAlignment(Pos.CENTER);

        card.getChildren().addAll(imageContainer, nameLabel, emailLabel, phoneLabel, buttonContainer);
        return card;
    }

    private void openAddClientWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AddClientView.fxml"));
            Parent root = loader.load();
            
            AddClientViewController controller = loader.getController();
            Stage stage = new Stage();
            controller.setStage(stage);
            
            stage.setTitle("Add New Client");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            // Refresh the client list after adding
            loadClientsData();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open add window");
            alert.setContentText("An error occurred while opening the add window: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void handleModifyClient(User client) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifyClientView.fxml"));
            Parent root = loader.load();
            
            ModifyClientViewController controller = loader.getController();
            Stage stage = new Stage();
            controller.setStage(stage);
            controller.setClientToModify(client);
            
            stage.setTitle("Modify Client");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            // Refresh the client list after modification
            loadClientsData();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open modify window");
            alert.setContentText("An error occurred while opening the modify window: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void handleDeleteClient(VBox card) {
        System.out.println("Delete client clicked");
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Client");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to delete this client?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Get the client's ID from the card's user data
                    User client = (User) card.getUserData();
                    if (client != null) {
                        // Delete from database
                        userService.supprimer(client.getId());
                        // Remove from UI
                        clientsContainer.getChildren().remove(card);
                        System.out.println("Client deleted successfully");
                    }
                } catch (SQLException e) {
                    System.out.println("Error deleting client: " + e.getMessage());
                    e.printStackTrace();
                    // Show error alert
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Error");
                    errorAlert.setHeaderText("Failed to delete client");
                    errorAlert.setContentText("An error occurred while deleting the client. Please try again.");
                    errorAlert.showAndWait();
                }
            } else {
                System.out.println("User cancelled deletion");
            }
        });
    }
} 