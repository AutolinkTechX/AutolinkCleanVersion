package org.example.pidev.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;

import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.pidev.entities.User;
import org.example.pidev.services.FavorieService;
import org.example.pidev.services.PanierService;
import org.example.pidev.utils.AlertUtils;
import org.example.pidev.utils.MyDatabase;
import org.example.pidev.utils.SessionManager;
import javafx.application.Platform;
import java.util.prefs.BackingStoreException;
import javafx.scene.image.Image;
import java.util.prefs.Preferences;

import java.util.Optional;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientDashboardController {
    @FXML private VBox contentArea;
    @FXML private Button homeBtn;
    @FXML private Button productsBtn;
    @FXML private MenuButton pagesBtn;
    @FXML private Button blogBtn;
    @FXML private Button contactBtn;
    @FXML private Button businessBtn;
    @FXML private Button favoriteIconButton;
    @FXML private Button cartIconButton;
    @FXML private Button invoiceIconButton;
    @FXML private Button userIconButton;
    @FXML private Label favoriteBadge;
    @FXML private Label cartBadge;    
    @FXML private MenuButton userMenuBtn;
    @FXML private ImageView userImage;
    @FXML private MenuItem profileMenuItem;
    @FXML private MenuItem logoutMenuItem;

    private User currentUser;
    private static final Logger logger = Logger.getLogger(ClientDashboardController.class.getName());

    @FXML
    public void initialize() {
        if (!verifySession()) {
            return;
        }
        setupUserMenu();
        initializeBadges();
        setupIconButtons();
        loadDefaultView();
        updateUserMenu();
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

    private void loadDefaultView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Home.fxml"));
            Node homeView = loader.load();

            Home homeController = loader.getController();
            homeController.setCurrentUser(this.currentUser);
            homeController.setDashboardController(this); // Important: passer la référence

            contentArea.getChildren().clear();
            contentArea.getChildren().add(homeView);
        } catch (IOException e) {
            showError("Error", "Failed to load home view");
            loadProductsView();
        }
    }

    private void setupIconButtons() {
        // Position badges
        favoriteBadge.setTranslateX(10);
        favoriteBadge.setTranslateY(-10);
        cartBadge.setTranslateX(10);
        cartBadge.setTranslateY(-10);
        blogBtn.setOnAction(this::handleBlogButton);

        // Set button actions
        favoriteIconButton.setOnAction(this::handleFavoritesButton);
        cartIconButton.setOnAction(this::handleCartButton);
        productsBtn.setOnAction(this::handleProductsButton);
    }

    @FXML
    private void handleProductsButton(ActionEvent event) {
        loadProductsView();
    }

    @FXML
    private void handleFavoritesButton(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Favorie.fxml"));
            Parent favoritesView = loader.load();

            Favorie favorieController = loader.getController();
            favorieController.setUserData(currentUser);
            favorieController.setClientDashboardController(this); // Ceci est crucial

            contentArea.getChildren().clear();
            contentArea.getChildren().add(favoritesView);

        } catch (IOException e) {
            AlertUtils.showErrorAlert("Erreur", "Impossible de charger les favoris", e.getMessage());
        }
    }

    // Dans ClientDashboardController
    public void showFavorites() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Favorie.fxml"));
            Parent root = loader.load();

            Favorie favorieController = loader.getController();
            favorieController.setClientDashboardController(this); // Passer la référence
            favorieController.setUserData(SessionManager.getCurrentUser());

            // Créer une nouvelle scène ou remplacer le contenu actuel
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Méthode pour revenir à l'accueil
    public void showHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Home.fxml"));
            Parent homeView = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(homeView);
        } catch (IOException e) {
            showAlert("Erreur", "Impossible de charger la page d'accueil", Alert.AlertType.ERROR);
        }
    }


    @FXML
    private void handleBlogButton(ActionEvent event) {
        try {
            // Load the blog view FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UserBlogView.fxml"));
            Node blogView = loader.load();

            // Get the controller and set the current user
            UserBlogController blogController = loader.getController();
            blogController.setCurrentUser(this.currentUser);

            // Clear the content area and add the blog view
            contentArea.getChildren().clear();
            contentArea.getChildren().add(blogView);

        } catch (IOException e) {
            // Show error message if loading fails
            AlertUtils.showErrorAlert("Error", "Failed to load blog",
                    "Could not load the blog view. Please try again.");
            logger.log(Level.SEVERE, "Failed to load blog view", e);
        }
    }


    @FXML
    public void handleCartButton(ActionEvent event) {
        try {
            // Charger le panier dans la zone de contenu
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Panier.fxml"));
            Parent panierView = loader.load();

            // Configurer le contrôleur du panier
            Panier panierController = loader.getController();
            panierController.setCurrentUser(this.currentUser);
            panierController.setDashboardController(this);

            // Effacer le contenu précédent et afficher le panier
            contentArea.getChildren().clear();
            contentArea.getChildren().add(panierView);

        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le panier", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void setupUserMenu(){
        profileMenuItem.setOnAction(e -> {
            try {
                loadProfileView();
            } catch (IOException ex) {
                Logger.getLogger(ClientDashboardController.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        logoutMenuItem.setOnAction(e -> {
            try {
                handleLogout();
            } catch (BackingStoreException ex) {
                Logger.getLogger(ClientDashboardController.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }

    private void initializeBadges() {
        favoriteBadge.setVisible(false);
        cartBadge.setVisible(false);

    }

    private void loadProfileView() throws IOException {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Profile.fxml"));
            Node view = loader.load();

            // Get the controller and set the dashboard reference
            ProfileController profileController = loader.getController();
            profileController.setDashboardController(this);

            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            showError("Error", "Failed to load profile view: " + e.getMessage());
        }
    }

    private void loadFavorisView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Favorie.fxml"));
            Node view = loader.load();

            Favorie controller = loader.getController();
            controller.setCurrentUser(currentUser);
            controller.setClientDashboardController(this);

            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            showError("Error", "Failed to load favorites view: " + e.getMessage());
        }
    }

    private void loadPanierView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Panier.fxml"));
            Node view = loader.load();

            Panier controller = loader.getController();
            controller.setCurrentUser(currentUser);
            controller.setDashboardController(this);

            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            showError("Error", "Failed to load cart view: " + e.getMessage());
        }
    }

    private void loadProductsView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ListeArticle.fxml"));
            Node view = loader.load();

            ListeArticle controller = loader.getController();
            controller.setCurrentUser(currentUser);
            controller.setDashboardController(this);

            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            showError("Error", "Failed to load products view: " + e.getMessage());
        }
    }

    private void handleLogout() throws BackingStoreException {
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

        if (result.isPresent() && result.get() == logoutButtonType){
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
                Parent root = FXMLLoader.load(getClass().getResource("/DashboardLogin.fxml"));
                Stage stage = (Stage) contentArea.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException e) {
                showError("Logout Error", "Failed to logout: " + e.getMessage());
            }
        }else{
            System.out.println("Logout cancelled");
        }
    }

    public void updateUserMenu() {
        if (currentUser != null) {
            // Set the user's name and last name in the menu button
            userMenuBtn.setText(currentUser.getName() + " " + currentUser.getLastName());

            // Load the user's profile image if available
            if (currentUser.getImage_path() != null && !currentUser.getImage_path().isEmpty()) {
                try {
                    Image profileImage = new Image(currentUser.getImage_path());
                    userImage.setImage(profileImage);
                } catch (Exception e) {
                    System.err.println("Error loading profile image: " + e.getMessage());
                    // Fallback to default image if there's an error
                    userImage.setImage(new Image(getClass().getResourceAsStream("/icons/Users.png")));
                }
            } else {
                // Use default image if no image path is set
                userImage.setImage(new Image(getClass().getResourceAsStream("/icons/Users.png")));
            }
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        SessionManager.setCurrentUser(user); // Stocke l'utilisateur dans la session
        updateBadges();
        updateUserMenu();
    }


    public void updateBadges() {
        if (currentUser != null) {
            try {
                FavorieService favorieService = new FavorieService();
                int favCount = favorieService.getFavorisCountForUser(currentUser.getId());
                updateBadge(favoriteBadge, favCount);

                PanierService panierService = new PanierService(MyDatabase.getInstance().getConnection());
                int cartCount = panierService.getPanierCountForUser(currentUser.getId());
                updateBadge(cartBadge, cartCount);
            } catch (Exception e) {
                System.err.println("Error updating badges: " + e.getMessage());
            }
        }
    }

    public void updateFavoritesBadge() {
        if (currentUser != null) {
            try {
                FavorieService favorieService = new FavorieService();
                int favCount = favorieService.getFavorisCountForUser(currentUser.getId());
                updateBadge(favoriteBadge, favCount);
            } catch (Exception e) {
                System.err.println("Error updating favorites badge: " + e.getMessage());
            }
        }
    }

    public void updateCartBadge() {
        if (currentUser != null) {
            try {
                PanierService panierService = new PanierService(MyDatabase.getInstance().getConnection());
                int cartCount = panierService.getPanierCountForUser(currentUser.getId());
                updateBadge(cartBadge, cartCount);
            } catch (Exception e) {
                System.err.println("Error updating cart badge: " + e.getMessage());
            }
        }
    }

    public void decrementFavorisBadge() {
        updateBadgeValue(favoriteBadge, -1);
    }

    private void updateBadgeValue(Label badge, int change) {
        try {
            int current = badge.isVisible() ? Integer.parseInt(badge.getText()) : 0;
            int newValue = current + change;

            if (newValue > 0) {
                badge.setText(String.valueOf(newValue));
                badge.setVisible(true);
            } else {
                badge.setVisible(false);
            }
        } catch (NumberFormatException e) {
            badge.setVisible(false);
        }
    }

    private void updateBadge(Label badge, int count) {
        if (count > 0) {
            badge.setText(String.valueOf(count));
            badge.setVisible(true);
        } else {
            badge.setVisible(false);
        }
    }

    public void refreshCurrentView() {
        if (!contentArea.getChildren().isEmpty()) {
            Node currentView = contentArea.getChildren().get(0);
            if (currentView.getId() != null) {
                switch (currentView.getId()) {
                    case "listeArticleView": loadProductsView(); break;
                    case "favorieView": loadFavorisView(); break;
                    case "panierView": loadPanierView(); break;
                    default: loadProductsView();
                }
            } else {
                loadProductsView();
            }
        } else {
            loadProductsView();
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public User getCurrentUser() {
        return this.currentUser;
    }

    public void showArticlesByCategory(String category) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ListeArticle.fxml"));
            Node articleView = loader.load();

            ListeArticle controller = loader.getController();
            controller.setCurrentUser(currentUser);
            controller.setDashboardController(this);
            controller.filterByCategory(category);

            contentArea.getChildren().clear();
            contentArea.getChildren().add(articleView);
        } catch (IOException e) {
            Logger.getLogger(ClientDashboardController.class.getName()).log(Level.SEVERE, null, e);
            showErrorAlert("Erreur", "Impossible de charger les articles");
        }
    }

    public void showAllArticles() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ListeArticle.fxml"));
            Node articleView = loader.load();

            ListeArticle controller = loader.getController();
            controller.setCurrentUser(currentUser);
            controller.setDashboardController(this);
            controller.loadArticles();

            contentArea.getChildren().clear();
            contentArea.getChildren().add(articleView);
        } catch (IOException e) {
            Logger.getLogger(ClientDashboardController.class.getName()).log(Level.SEVERE, null, e);
            showErrorAlert("Erreur", "Impossible de charger les articles");
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleInvoiceClick(ActionEvent event) {
        loadFactureView();
    }

    private void loadFactureView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Facture.fxml"));
            Parent factureView = loader.load();

            FactureController controller = loader.getController();
            controller.setCurrentUser(SessionManager.getCurrentUser());
            controller.setDashboardController(this);

            contentArea.getChildren().setAll(factureView);
        } catch (IOException e) {
            AlertUtils.showErrorAlert("Erreur", "Impossible de charger les factures",
                    "Veuillez réessayer ou contacter l'administrateur.");
        }
    }


    /**********code farah****/

    @FXML
    private void loadAddProductView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ShowMaterielRecyclable.fxml"));
            Node view = loader.load();

            ShowMaterielRecyclable controller = loader.getController();
            controller.setCurrentUser(currentUser);
            controller.setDashboardController(this);

            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            showError("Erreur", "Échec du chargement de la vue Ajouter Produit : " + e.getMessage());
        }
    }


    @FXML
    private void loadWorkshopView(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_workshop/TaskView.fxml"));
            Node view = loader.load();

            TaskController controller = loader.getController(); // Use TaskController
            controller.setCurrentUser(currentUser); // Ensure TaskController has this method
            // Optionally pass the dashboard controller if needed
            // controller.setDashboardController(this);

            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            showError("Error", "Failed to load workshop view: " + e.getMessage());
            logger.log(Level.SEVERE, "Failed to load workshop view", e);
        }
    }
}