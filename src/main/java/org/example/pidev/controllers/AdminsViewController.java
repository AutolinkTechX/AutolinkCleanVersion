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

public class AdminsViewController implements Initializable {

    @FXML
    private FlowPane adminsContainer;

    @FXML
    private ImageView addAdminButton;

    private final UserService userService = new UserService();
    private ObservableList<User> adminsList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("AdminsViewController initialized");
        if (adminsContainer == null) {
            System.out.println("adminsContainer is null");
            return;
        }
        System.out.println("adminsContainer is not null");
        loadAdminsData();

        // Set up add admin button
        if (addAdminButton != null) {
            addAdminButton.setOnMouseClicked(event -> openAddAdminWindow());
        } else {
            System.out.println("addAdminButton is null");
        }
    }

    private void loadAdminsData() {
        try {
            System.out.println("Loading admins data...");
            if (adminsContainer == null) {
                System.out.println("Error: adminsContainer is null!");
                return;
            }

            adminsList = FXCollections.observableArrayList(userService.getAllAdmins());
            System.out.println("Number of admins loaded: " + adminsList.size());

            if (adminsList.isEmpty()) {
                System.out.println("No admins found to display");
                // Show a message to the user
                Label noAdminsLabel = new Label("No admins found in the database");
                noAdminsLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 16px;");
                adminsContainer.getChildren().add(noAdminsLabel);
                return;
            }

            createAdminCards();
        } catch (Exception e) {
            System.out.println("Error loading admins data: " + e.getMessage());
            e.printStackTrace();

            // Show error message to user
            Label errorLabel = new Label("Error loading admin data: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");
            adminsContainer.getChildren().add(errorLabel);
        }
    }

    private void createAdminCards() {
        System.out.println("Creating admin cards...");
        adminsContainer.getChildren().clear(); // Clear any existing content

        for (User admin : adminsList) {
            if (admin == null) {
                System.out.println("Warning: Found null admin in list");
                continue;
            }

            System.out.println("Creating card for: " + admin.getName());
            VBox card = new VBox(10);
            card.setStyle("-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");
            card.setPadding(new Insets(15));
            card.setPrefWidth(300);
            card.setPrefHeight(250);

            // Store the admin object in the card's user data
            card.setUserData(admin);
            card.setAlignment(Pos.CENTER);

            // Create image container
            VBox imageContainer = new VBox();
            imageContainer.setAlignment(Pos.CENTER);

            // Create and configure ImageView
            ImageView userImageView = new ImageView();
            userImageView.setFitWidth(100);
            userImageView.setFitHeight(100);
            userImageView.setPreserveRatio(true);

            // Try to load the user's image
            try {
                String image_path = admin.getImage_path();
                if (image_path != null && !image_path.isEmpty()) {
                    Image image = new Image("file:" + image_path);
                    userImageView.setImage(image);
                } else {
                    // Load logo.jpg as default image if no image path is provided
                    Image defaultImage = new Image(getClass().getResourceAsStream("/images/logo.jpg"));
                    userImageView.setImage(defaultImage);
                }
            } catch (Exception e) {
                System.out.println("Error loading image for user: " + admin.getName());
                // Load logo.jpg as default image in case of error
                Image defaultImage = new Image(getClass().getResourceAsStream("/images/logo.jpg"));
                userImageView.setImage(defaultImage);
            }

            imageContainer.getChildren().add(userImageView);
            imageContainer.setAlignment(Pos.CENTER);

            // Admin information
            Label nameLabel = new Label(admin.getName() + " " + admin.getLastName());
            nameLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
            nameLabel.setAlignment(Pos.CENTER);
            Label emailLabel = new Label(admin.getEmail());
            emailLabel.setFont(Font.font("System", 14));
            emailLabel.setAlignment(Pos.CENTER);

            Label phoneLabel = new Label("Phone: " + admin.getPhone());
            phoneLabel.setFont(Font.font("System", 14));
            phoneLabel.setTextFill(Color.GRAY);
            phoneLabel.setAlignment(Pos.CENTER);

            // Action buttons
            HBox buttonContainer = new HBox(10);
            buttonContainer.setPadding(new Insets(10, 0, 0, 0));
            buttonContainer.setAlignment(Pos.CENTER);

            Button modifyButton = new Button("Modify");
            modifyButton.setStyle("-fx-background-color: rgb(202,138,98); -fx-text-fill: white; -fx-background-radius: 5px;");
            modifyButton.setOnAction(e -> handleModifyAdmin(admin));

            Button deleteButton = new Button("Delete");
            deleteButton.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white; -fx-background-radius: 5px;");
            deleteButton.setOnAction(e -> handleDeleteAdmin(card));

            buttonContainer.getChildren().addAll(modifyButton, deleteButton);

            card.getChildren().addAll(imageContainer, nameLabel, emailLabel, phoneLabel, buttonContainer);
            adminsContainer.getChildren().add(card);
            System.out.println("Card added to container");
        }
        System.out.println("All cards created");
    }


    @FXML
    private void handleModifyAdmin(User admin) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifyAdminView.fxml"));
            Parent root = loader.load();

            ModifyAdminViewController controller = loader.getController();
            Stage stage = new Stage();
            controller.setStage(stage);
            controller.setAdminToModify(admin);

            stage.setTitle("Modify Admin");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // Refresh the admin list after modification
            loadAdminsData();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open modify window");
            alert.setContentText("An error occurred while opening the modify window: " + e.getMessage());
            alert.showAndWait();
        }
    }


    private void handleDeleteAdmin(VBox card) {
        System.out.println("Delete admin clicked");
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Admin");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to delete this admin?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Get the admin's ID from the card's user data
                    User admin = (User) card.getUserData();
                    if (admin != null) {
                        // Delete from database
                        userService.supprimer(admin.getId());
                        // Remove from UI
                        adminsContainer.getChildren().remove(card);
                        System.out.println("Admin deleted successfully");
                    }
                } catch (SQLException e) {
                    System.out.println("Error deleting admin: " + e.getMessage());
                    e.printStackTrace();
                    // Show error alert
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Error");
                    errorAlert.setHeaderText("Failed to delete admin");
                    errorAlert.setContentText("An error occurred while deleting the admin. Please try again.");
                    errorAlert.showAndWait();
                }
            } else {
                System.out.println("User cancelled deletion");
            }
        });
    }

    private void openAddAdminWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AddAdminView.fxml"));
            Parent root = loader.load();
            AddAdminViewController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Add New Admin");
            stage.setScene(new Scene(root));
            stage.setResizable(false);

            controller.setStage(stage);
            stage.showAndWait();

            // Refresh the admin list after adding a new admin
            loadAdminsData();
        } catch (Exception e) {
            System.out.println("Error opening add admin window: " + e.getMessage());
            e.printStackTrace();
        }
    }
}