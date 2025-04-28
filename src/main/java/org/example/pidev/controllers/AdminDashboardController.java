package org.example.pidev.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.pidev.utils.SessionManager;
import javafx.application.Platform;
import java.util.prefs.Preferences;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.prefs.BackingStoreException;

public class AdminDashboardController {

    @FXML
    private Button LogoutButton;
    
    @FXML
    private MenuButton ProductsMenuButton;
    
    @FXML
    private MenuItem ForRecyclingButton;
    
    @FXML
    private MenuItem ForSaleButton;
    
    @FXML
    private MenuButton UsersMenuButton;
    
    @FXML
    private MenuItem AdminsMenuItem;
    
    @FXML
    private MenuItem ClientsMenuItem;

    @FXML
    private MenuItem EntreprisesMenuItem;

    @FXML
    private VBox contentArea;
    @FXML private StackPane contentPane;
    @FXML private Button BlogButton;

    @FXML private MenuButton ServicesMenuButton;
    @FXML private MenuItem OrdersMenuItem;
    @FXML private MenuItem CalendarMenuItem;
    @FXML private MenuItem StatisticsMenuItem;

    private static final Logger logger = Logger.getLogger(ClientDashboardController.class.getName());

    @FXML
    private void initialize() {
        if (!verifySession()) {
            return;
        }
        System.out.println("AdminDashboardController initialized");
        if (LogoutButton != null) {
            System.out.println("LogoutButton is not null");
            LogoutButton.setOnAction(event -> {
                System.out.println("Logout button clicked");
                try {
                    handleLogout();
                } catch (BackingStoreException e) {
                    e.printStackTrace();
                }
            });
        } else {
            System.out.println("LogoutButton is null - FXML injection failed");
        }
        
        // Add CSS class to the ProductsMenuButton
        ProductsMenuButton.getStyleClass().add("menu-button");
        
        // Apply styling to the Products menu items
        ForRecyclingButton.getStyleClass().add("menu-item");
        ForSaleButton.getStyleClass().add("menu-item");
        
        // Create a custom graphic for the Products menu button with icon and white text
        HBox productsCustomGraphic = new HBox(5); // 5 is the spacing between elements
        productsCustomGraphic.setStyle("-fx-alignment: center-left;");
        
        // Create the Products icon
        ImageView productsIcon = new ImageView();
        productsIcon.setImage(new Image(getClass().getResourceAsStream("/icons/Products.png")));
        productsIcon.setFitHeight(25.0);
        productsIcon.setFitWidth(25.0);
        productsIcon.setPreserveRatio(true);
        
        // Create the "Products" text label
        Label productsLabel = new Label("Products");
        productsLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px;");
        
        // Add elements to the HBox in the correct order: icon, text
        productsCustomGraphic.getChildren().addAll(productsIcon, productsLabel);
        
        // Set the custom graphic as the button's graphic
        ProductsMenuButton.setGraphic(productsCustomGraphic);
        ProductsMenuButton.setText(""); // Clear the text since we're using a custom graphic
        
        // Add CSS class to the UsersMenuButton
        UsersMenuButton.getStyleClass().add("menu-button");
        
        // Apply styling to the Users menu items
        AdminsMenuItem.getStyleClass().add("menu-item");
        ClientsMenuItem.getStyleClass().add("menu-item");
        EntreprisesMenuItem.getStyleClass().add("menu-item");
        
        // Create a custom graphic for the Users menu button with icon and white text
        HBox usersCustomGraphic = new HBox(5); // 5 is the spacing between elements
        usersCustomGraphic.setStyle("-fx-alignment: center-left;");
        
        // Create the Users icon
        ImageView usersIcon = new ImageView();
        usersIcon.setImage(new Image(getClass().getResourceAsStream("/icons/Users.png")));
        usersIcon.setFitHeight(27.0);
        usersIcon.setFitWidth(34.0);
        usersIcon.setPreserveRatio(true);
        
        // Create the "Users" text label
        Label usersLabel = new Label("Users");
        usersLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px;");
        
        // Add elements to the HBox in the correct order: icon, text
        usersCustomGraphic.getChildren().addAll(usersIcon, usersLabel);
        
        // Set the custom graphic as the button's graphic
        UsersMenuButton.setGraphic(usersCustomGraphic);
        UsersMenuButton.setText(""); // Clear the text since we're using a custom graphic

        // Add CSS class to the ServicesMenuButton
        ServicesMenuButton.getStyleClass().add("menu-button");

        // Apply styling to the Services menu items
        OrdersMenuItem.getStyleClass().add("menu-item");
        CalendarMenuItem.getStyleClass().add("menu-item");
        StatisticsMenuItem.getStyleClass().add("menu-item");


        // Create a custom graphic for the Services menu button
        HBox servicesCustomGraphic = new HBox(5);
        servicesCustomGraphic.setStyle("-fx-alignment: center-left;");

        // Create the Services icon
        ImageView servicesIcon = new ImageView();
        servicesIcon.setImage(new Image(getClass().getResourceAsStream("/icons/Orders.png")));
        servicesIcon.setFitHeight(25.0);
        servicesIcon.setFitWidth(39.0);
        servicesIcon.setPreserveRatio(true);

        // Create the "Services" text label
        Label servicesLabel = new Label("Services");
        servicesLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px;");

        // Add elements to the HBox
        servicesCustomGraphic.getChildren().addAll(servicesIcon, servicesLabel);

        // Set the custom graphic
        ServicesMenuButton.setGraphic(servicesCustomGraphic);
        ServicesMenuButton.setText(""); // Clear the text since we're using a custom graphic
    }

    private boolean verifySession() {
        if (SessionManager.getCurrentUser() == null && 
            SessionManager.getCurrentEntreprise() == null) {
            
            System.out.println("Session verification failed - redirecting to login");
            
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Session Expired");
                alert.setHeaderText(null);
                alert.setContentText("Please log in again to continue");
                alert.showAndWait();
    
                try {
                    Parent root = FXMLLoader.load(getClass().getResource("/DashboardLogin.fxml"));
                    Stage stage = (Stage) contentArea.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            
            return false;
        }
        return true;
    }

    @FXML
    private void handleLogout() throws BackingStoreException {
        System.out.println("handleLogout method called");
        
        // Create a custom alert dialog
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Logout Confirmation");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to logout?");
        
        // Customize the dialog buttons
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
        ButtonType logoutButtonType = new ButtonType("Logout", ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(cancelButtonType, logoutButtonType);
        
        // Get the dialog pane to customize it
        DialogPane dialogPane = alert.getDialogPane();
        
        // Add the dialog CSS file
        dialogPane.getStylesheets().add(getClass().getResource("/styles/dialog.css").toExternalForm());
        
        // Add CSS classes to the dialog pane
        dialogPane.getStyleClass().add("dialog-pane");
        
        // Get the buttons to customize them
        Button logoutButton = (Button) dialogPane.lookupButton(logoutButtonType);
        Button cancelButton = (Button) dialogPane.lookupButton(cancelButtonType);
        
        // Add CSS classes to the buttons
        logoutButton.getStyleClass().add("logout-button");
        cancelButton.getStyleClass().add("cancel-button");
        
        // Show the dialog and wait for user response
        Optional<ButtonType> result = alert.showAndWait();
        
        // Process the result
        if (result.isPresent() && result.get() == logoutButtonType) {
            // User clicked Logout
            System.out.println("User confirmed logout");
            
            // Clear the session
            SessionManager.clearSession();
            // Clear saved credentials from preferences
            Preferences prefs = Preferences.userRoot().node("pidev_app_prefs");
            prefs.remove("remember_me");
            prefs.remove("saved_email");
            prefs.remove("saved_password");
            prefs.remove("user_type");
            try {
                prefs.flush();
            } catch (BackingStoreException e) {
                logger.log(Level.SEVERE, "Failed to clear saved credentials", e);
            }
            
            try {
                // Load the login page
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/DashboardLogin.fxml"));
                Parent root = loader.load();
                
                // Create a new scene with the login page
                Scene scene = new Scene(root);
                
                // Get the current stage
                Stage stage = (Stage) LogoutButton.getScene().getWindow();
                
                // Set the new scene
                stage.setScene(scene);
                stage.setTitle("Login");
                stage.show();
                
            } catch (IOException e) {
                System.out.println("Error during logout: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // User clicked Cancel or closed the dialog
            System.out.println("Logout cancelled");
        }
    }


    @FXML
    private void handleBlog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/BlogAffichage.fxml"));
            Parent blogView = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(blogView);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load Blog view", "An error occurred while trying to load the Blog view.");
        }
    }
    
    @FXML
    private void handleRecyclingProducts() {
        System.out.println("Recycling Products clicked");
        // TODO: Implement navigation to recycling products page
    }
    
    @FXML
    private void handleSaleProducts() {
        System.out.println("Sale Products clicked");
        loadProductView();
        // TODO: Implement navigation to sale products page
    }
    
    @FXML
    private void handleAdmins() {
        System.out.println("Admins clicked - Loading content");
        try {
            System.out.println("Loading FXML file...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdminsView.fxml"));
            System.out.println("FXML loader created");
            
            Parent adminsView = loader.load();
            System.out.println("FXML loaded successfully");
            
            // Clear existing content and add the new view
            contentArea.getChildren().clear();
            contentArea.getChildren().add(adminsView);
            System.out.println("Content updated");
            
        } catch (IOException e) {
            System.out.println("Error loading admins view: " + e.getMessage());
            e.printStackTrace();
            
            // Show error alert to user
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Failed to load Admins view");
            alert.setContentText("An error occurred while trying to load the Admins view. Please try again.");
            alert.showAndWait();
        }
    }
    
    @FXML
    private void handleClients() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ClientsView.fxml"));
            Parent clientsView = loader.load();
            
            // Clear existing content and add the new view
            contentArea.getChildren().clear();
            contentArea.getChildren().add(clientsView);
            
        } catch (IOException e) {
            System.out.println("Error loading clients view: " + e.getMessage());
            e.printStackTrace();
            
            // Show error alert to user
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Failed to load Clients view");
            alert.setContentText("An error occurred while trying to load the Clients view. Please try again.");
            alert.showAndWait();
        }
    }

    @FXML
    private void handleEntreprises() {
        try {
            System.out.println("Loading Entreprises view...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/EntreprisesView.fxml"));
            Parent entreprisesView = loader.load();
            
            // Clear existing content and add the new view
            contentArea.getChildren().clear();
            contentArea.getChildren().add(entreprisesView);
            System.out.println("Entreprises view loaded successfully");
            
        } catch (IOException e) {
            System.out.println("Error loading entreprises view: " + e.getMessage());
            e.printStackTrace();
            
            // Show error alert to user
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Failed to load Entreprises view");
            alert.setContentText("An error occurred while trying to load the Entreprises view. Please try again.");
            alert.showAndWait();
        }
    }

    private void loadProductView() {
        System.out.println("Attempting to load product view...");

        // Vérifier que contentArea est bien injecté
        if (contentArea == null) {
            System.err.println("Error: contentArea is null!");
            return;
        }

        try {
            URL fxmlUrl = getClass().getResource("/AjoutProduit.fxml");
            System.out.println("FXML URL: " + fxmlUrl);

            if (fxmlUrl == null) {
                System.err.println("Error: FXML file not found!");
                throw new IOException("Fichier FXML introuvable");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent productView = loader.load();
            System.out.println("FXML loaded successfully");

            System.out.println("ContentArea children before: " + contentArea.getChildren().size());
            contentArea.getChildren().setAll(productView);
            System.out.println("ContentArea children after: " + contentArea.getChildren().size());

        } catch (IOException e) {
            System.out.println("Error loading product view: " + e.getMessage());
            e.printStackTrace();

            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible de charger la vue");
            alert.setContentText("Une erreur est survenue lors du chargement de la page des produits.");
            alert.showAndWait();
        }
    }

    public void handleCalendrier(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CalendarView.fxml"));
            Parent calendarView = loader.load();

            contentArea.getChildren().clear();
            contentArea.getChildren().add(calendarView);

        } catch (IOException e) {
            e.printStackTrace();
            // Gérer l'erreur (peut-être afficher un message à l'utilisateur)
        }
    }

    @FXML
    private void handleOrders() {
        try {
            // Option 1 - Chemin absolu depuis src/main/resources
            URL fxmlUrl = getClass().getResource("/Orders.fxml");

            // Option 2 - Si dans le même package que le contrôleur
            // URL fxmlUrl = getClass().getResource("orders_page.fxml");

            if (fxmlUrl == null) {
                throw new IOException("Fichier FXML non trouvé: orders_page.fxml");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            VBox ordersPage = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(ordersPage);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la page des commandes",
                    "Détails: " + e.getMessage());
        }
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Add this method to your AdminDashboardController
    @FXML
    private void handleStatistics(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Statistique.fxml"));
            Parent root = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void showError(String header, String content) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Navigation Error");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
