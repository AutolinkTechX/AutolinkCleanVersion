package org.example.pidev.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.pidev.utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

public class AdminDashboardController {

    // UI Components
    @FXML private Button LogoutButton;
    @FXML private MenuButton ProductsMenuButton;
    @FXML private MenuItem ForRecyclingButton;
    @FXML private MenuItem ForSaleButton;
    @FXML private MenuButton UsersMenuButton;
    @FXML private MenuItem AdminsMenuItem;
    @FXML private MenuItem ClientsMenuItem;
    @FXML private MenuItem EntreprisesMenuItem;
    @FXML private VBox contentArea;
    @FXML private StackPane contentPane;
    @FXML private Button BlogButton;
    @FXML private Button OrdersButton;
    @FXML private Button StatisticsButton;

    @FXML
    private void initialize() {
        System.out.println("AdminDashboardController initialized");

        // Initialize button actions
        if (LogoutButton != null) {
            LogoutButton.setOnAction(event -> handleLogout());
        }

        if (BlogButton != null) {
            BlogButton.setOnAction(event -> handleBlog());
        }

        if (OrdersButton != null) {
            OrdersButton.setOnAction(event -> handleOrders());
        }

        if (StatisticsButton != null) {
            StatisticsButton.setOnAction(event -> handleStatistics(event));
        }

        // Setup Products menu button
        setupMenuButton(ProductsMenuButton, "Products", "/icons/Products.png", 25.0, 25.0);
        ForRecyclingButton.getStyleClass().add("menu-item");
        ForSaleButton.getStyleClass().add("menu-item");
        ForRecyclingButton.setOnAction(event -> handleRecyclingProducts());
        ForSaleButton.setOnAction(event -> handleSaleProducts());

        // Setup Users menu button
        setupMenuButton(UsersMenuButton, "Users", "/icons/Users.png", 27.0, 34.0);
        AdminsMenuItem.getStyleClass().add("menu-item");
        ClientsMenuItem.getStyleClass().add("menu-item");
        EntreprisesMenuItem.getStyleClass().add("menu-item");
        AdminsMenuItem.setOnAction(event -> handleAdmins());
        ClientsMenuItem.setOnAction(event -> handleClients());
        EntreprisesMenuItem.setOnAction(event -> handleEntreprises());
    }

    private void setupMenuButton(MenuButton menuButton, String text, String iconPath,
                                 double iconHeight, double iconWidth) {
        menuButton.getStyleClass().add("menu-button");

        HBox customGraphic = new HBox(5);
        customGraphic.setStyle("-fx-alignment: center-left;");

        ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(iconPath)));
        icon.setFitHeight(iconHeight);
        icon.setFitWidth(iconWidth);
        icon.setPreserveRatio(true);

        Label label = new Label(text);
        label.setStyle("-fx-text-fill: white; -fx-font-size: 20px;");

        customGraphic.getChildren().addAll(icon, label);
        menuButton.setGraphic(customGraphic);
        menuButton.setText("");
    }

    // Navigation Handlers
    @FXML
    private void handleRecyclingProducts() {
        System.out.println("Recycling Products clicked");
        // TODO: Implement recycling products view
    }

    @FXML
    private void handleSaleProducts() {
        System.out.println("Sale Products clicked");
        loadProductView();
    }

    @FXML
    private void handleAdmins() {
        System.out.println("Admins clicked - Loading content");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdminsView.fxml"));
            Parent adminsView = loader.load();

            contentArea.getChildren().clear();
            contentArea.getChildren().add(adminsView);

        } catch (IOException e) {
            System.out.println("Error loading admins view: " + e.getMessage());
            showError("Failed to load Admins view", "An error occurred while trying to load the Admins view.");
        }
    }

    @FXML
    private void handleClients() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ClientsView.fxml"));
            Parent clientsView = loader.load();

            contentArea.getChildren().clear();
            contentArea.getChildren().add(clientsView);

        } catch (IOException e) {
            System.out.println("Error loading clients view: " + e.getMessage());
            showError("Failed to load Clients view", "An error occurred while trying to load the Clients view.");
        }
    }

    @FXML
    private void handleEntreprises() {
        try {
            System.out.println("Loading Entreprises view...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/EntreprisesView.fxml"));
            Parent entreprisesView = loader.load();

            contentArea.getChildren().clear();
            contentArea.getChildren().add(entreprisesView);

        } catch (IOException e) {
            System.out.println("Error loading entreprises view: " + e.getMessage());
            showError("Failed to load Entreprises view", "An error occurred while trying to load the Entreprises view.");
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
    private void handleOrders() {
        try {
            URL fxmlUrl = getClass().getResource("/Orders.fxml");
            if (fxmlUrl == null) {
                throw new IOException("Fichier FXML non trouvé: Orders.fxml");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            VBox ordersPage = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(ordersPage);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load Orders view", "An error occurred while trying to load the Orders view.");
        }
    }

    @FXML
    private void handleStatistics(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Statistique.fxml"));
            Parent root = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(root);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load Statistics view", "An error occurred while trying to load the Statistics view.");
        }
    }

    private void loadProductView() {
        System.out.println("Attempting to load product view...");

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

            contentArea.getChildren().setAll(productView);

        } catch (IOException e) {
            System.out.println("Error loading product view: " + e.getMessage());
            showError("Failed to load Product view", "An error occurred while trying to load the Product view.");
        }
    }

    @FXML
    private void handleLogout() {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Logout Confirmation");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to logout?");

        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
        ButtonType logoutButtonType = new ButtonType("Logout", ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(cancelButtonType, logoutButtonType);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles/dialog.css").toExternalForm());
        dialogPane.getStyleClass().add("dialog-pane");

        Button logoutButton = (Button) dialogPane.lookupButton(logoutButtonType);
        Button cancelButton = (Button) dialogPane.lookupButton(cancelButtonType);
        logoutButton.getStyleClass().add("logout-button");
        cancelButton.getStyleClass().add("cancel-button");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == logoutButtonType) {
            SessionManager.clearSession();
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/DashboardLogin.fxml"));
                Parent root = loader.load();
                Scene scene = new Scene(root);
                Stage stage = (Stage) LogoutButton.getScene().getWindow();
                stage.setScene(scene);
                stage.setTitle("Login");
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
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