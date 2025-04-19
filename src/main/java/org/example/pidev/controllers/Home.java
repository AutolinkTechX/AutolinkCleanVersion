package org.example.pidev.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import org.example.pidev.entities.User;
import org.example.pidev.services.ArticleService;
import org.example.pidev.utils.SessionManager;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Home {

    private static final Logger LOGGER = Logger.getLogger(Home.class.getName());
    private static final int CARDS_PER_PAGE = 5;

    @FXML private ScrollPane mainScrollPane;
    @FXML private VBox mainContainer;
    @FXML private Button buyProductBtn;
    @FXML private Button addProductBtn;
    @FXML private HBox categoriesContainer;
    @FXML private HBox navigationContainer;
    @FXML private Button prevButton;
    @FXML private Button nextButton;

    private ClientDashboardController dashboardController;
    private User currentUser;
    private final ArticleService articleService = new ArticleService();
    private List<String> allCategories = new ArrayList<>();
    private int currentPage = 0;

    public void setDashboardController(ClientDashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        initializeUserDependentComponents();
    }

    @FXML
    public void initialize() {
        currentUser = SessionManager.getCurrentUser();

        if (currentUser == null) {
            LOGGER.warning("Aucun utilisateur connecté.");
            showErrorAlert("Erreur", "Vous devez être connecté pour accéder à cette fonctionnalité.");
            return;
        }

        LOGGER.info("Utilisateur connecté : " + currentUser.getName());
        initializeComponents();
    }

    private void initializeUserDependentComponents() {
        if (currentUser != null) {
            setupButtonActions();
            loadCategories();
        }
    }

    private void initializeComponents() {
        try {
            setupButtonActions();
            setupNavigationButtons();
            loadCategories();
            mainScrollPane.setVvalue(0);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation", e);
            showErrorAlert("Erreur d'initialisation", "Une erreur est survenue lors du chargement de l'interface.");
        }
    }

    private void setupNavigationButtons() {
        prevButton.setOnAction(e -> {
            if (currentPage > 0) {
                currentPage--;
                displayCurrentPage();
            }
        });

        nextButton.setOnAction(e -> {
            if ((currentPage + 1) * CARDS_PER_PAGE < allCategories.size()) {
                currentPage++;
                displayCurrentPage();
            }
        });

        // Style des boutons de navigation
        prevButton.getStyleClass().add("nav-button");
        nextButton.getStyleClass().add("nav-button");
    }

    private void setupButtonActions() {
        if (buyProductBtn != null) {
            buyProductBtn.setOnAction(event -> {
                if (dashboardController != null) {
                    dashboardController.showAllArticles();
                }
            });
        }

        if (addProductBtn != null) {
            addProductBtn.setOnAction(event -> {
                if (dashboardController != null) {
                    dashboardController.showAllArticles();
                }
            });
            addProductBtn.setVisible(currentUser != null && currentUser.getRoleId() == 1);
        }
    }

    private void loadCategories() {
        try {
            allCategories.clear();
            Set<String> categories = articleService.getAllCategoriesFromArticles();

            if (categories != null && !categories.isEmpty()) {
                allCategories.addAll(categories);
                displayCurrentPage();
            } else {
                LOGGER.info("Aucune catégorie trouvée.");
                Label noCategoryLabel = new Label("Aucune catégorie disponible");
                noCategoryLabel.getStyleClass().add("no-data-label");
                categoriesContainer.getChildren().add(noCategoryLabel);
                navigationContainer.setVisible(false);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des catégories", e);
            showErrorAlert("Erreur de base de données", "Impossible de charger les catégories.");
        }
    }

    private void displayCurrentPage() {
        categoriesContainer.getChildren().clear();

        int startIndex = currentPage * CARDS_PER_PAGE;
        int endIndex = Math.min(startIndex + CARDS_PER_PAGE, allCategories.size());

        for (int i = startIndex; i < endIndex; i++) {
            String category = allCategories.get(i);
            VBox categoryCard = createCategoryCard(category);
            categoriesContainer.getChildren().add(categoryCard);
        }

        // Mettre à jour l'état des boutons de navigation
        prevButton.setDisable(currentPage == 0);
        nextButton.setDisable(endIndex >= allCategories.size());
    }
/*
    private VBox createCategoryCard(String categoryName) {
        VBox card = new VBox();
        card.setAlignment(Pos.CENTER);
        card.setSpacing(10);
        card.setPrefWidth(180);
        card.setPrefHeight(220);
        card.getStyleClass().add("category-card");
        card.setUserData(categoryName);

        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 0);" +
                    "-fx-background-color: #f5f5f5;");
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 0);" +
                    "-fx-background-color: white;");
        });

        ImageView imageView = new ImageView();
        try {
            String imagePath = "/images/categories/" + categoryName.toLowerCase() + ".png";
            Image image = new Image(getClass().getResourceAsStream(imagePath));
            imageView.setImage(image);
        } catch (Exception e) {
            LOGGER.warning("Image de catégorie non trouvée, image par défaut utilisée.");
            imageView.setImage(new Image(getClass().getResourceAsStream("/images/logo.jpg")));
        }

        imageView.setFitWidth(120);
        imageView.setFitHeight(120);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCache(true);
        imageView.getStyleClass().add("category-image");

        Label label = new Label(categoryName);
        label.getStyleClass().add("category-title");
        label.setWrapText(true);
        label.setMaxWidth(150);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Button button = new Button("Voir + ");
        button.getStyleClass().add("category-btn");
        button.setStyle("-fx-background-color: #4a7bd6; -fx-text-fill: white;");
        button.setOnAction(e -> {
            if (dashboardController != null) {
                dashboardController.showArticlesByCategory(categoryName);
            }
        });

        card.setOnMouseClicked(e -> {
            if (dashboardController != null) {
                dashboardController.showArticlesByCategory(categoryName);
            }
        });

        card.getChildren().addAll(imageView, label, button);
        return card;
    }
*/

    private VBox createCategoryCard(String categoryName) {
        VBox card = new VBox();
        card.setAlignment(Pos.CENTER);
        card.setSpacing(10);
        card.setPrefWidth(180);
        card.setPrefHeight(220);
        card.getStyleClass().add("category-card");
        card.setUserData(categoryName);

        // Styles pour hover
        card.setOnMouseEntered(e -> card.getStyleClass().add("category-card-hover"));
        card.setOnMouseExited(e -> card.getStyleClass().remove("category-card-hover"));

        ImageView imageView = new ImageView();
        try {
            // Utilisez toujours la même image pour toutes les catégories
            imageView.setImage(new Image(getClass().getResourceAsStream("/images/categories/Category.jpg")));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur de chargement de l'image de catégorie", e);
            imageView.setImage(new Image(getClass().getResourceAsStream("/images/logo.jpg")));
        }

        imageView.setFitWidth(120);
        imageView.setFitHeight(120);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCache(true);
        imageView.getStyleClass().add("category-image");

        Label label = new Label(categoryName);
        label.getStyleClass().add("category-title");
        label.setWrapText(true);
        label.setMaxWidth(150);
        label.setTextAlignment(TextAlignment.CENTER);

        Button button = new Button("Voir + ");
        button.getStyleClass().add("category-btn");
        button.setOnAction(e -> {
            if (dashboardController != null) {
                dashboardController.showArticlesByCategory(categoryName);
            }
        });

        card.getChildren().addAll(imageView, label, button);
        return card;
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}