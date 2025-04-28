package org.example.pidev.controllers;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.pidev.entities.Article;
import org.example.pidev.entities.User;
import org.example.pidev.services.FavorieService;
import org.example.pidev.services.PanierService;
import org.example.pidev.utils.AlertUtils;
import org.example.pidev.utils.MyDatabase;
import org.example.pidev.utils.SessionManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class Favorie implements Initializable {

    private static final int ITEMS_PER_PAGE = 3; // Augmenté à 6 comme dans ListeArticle
    private static final int CARDS_PER_ROW = 3; // 3 cartes par ligne comme dans ListeArticle
    private static final double CARD_WIDTH = 280; // Largeur augmentée comme dans ListeArticle
    private static final double CARD_HEIGHT = 320;
    private static final double HGAP = 20;
    private static final double VGAP = 20;

    // Styles pour les effets de survol
    private static final String HOVER_STYLE = "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 2);";
    private static final String NORMAL_STYLE = "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1);";

    @FXML private FlowPane articlesContainer;
    @FXML private TextField searchField;
    @FXML private Button prevPageBtn;
    @FXML private HBox pageIndicatorsContainer;
    @FXML private Button nextPageBtn;

    private final FavorieService favorieService = new FavorieService();
    private final PanierService panierService = new PanierService(MyDatabase.getInstance().getConnection());
    private List<Article> allFavorites = new ArrayList<>();
    private int currentPage = 1;
    private int totalPages = 1;
    private User currentUser;
    private ClientDashboardController clientDashboardController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.currentUser = SessionManager.getCurrentUser();
        setupUI();
        setupEventHandlers();

        if (currentUser != null && "USER".equals(SessionManager.getCurrentUserType())) {
            loadFavoriteArticles();
        } else {
            showLoginAlertAndRedirect();
        }
    }
    // This is an alias for setCurrentUser to resolve the "Cannot resolve method 'setUserData'" error
    public void setUserData(User user) {
        setCurrentUser(user);
    }

    public void setClientDashboardController(ClientDashboardController controller) {
        this.clientDashboardController = controller;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (this.currentUser != null && "USER".equals(SessionManager.getCurrentUserType())) {
            loadFavoriteArticles();
        } else {
            showLoginAlertAndRedirect();
        }
    }

    private void setupUI() {
        articlesContainer.setHgap(HGAP);
        articlesContainer.setVgap(VGAP);
        articlesContainer.setAlignment(Pos.CENTER);
        articlesContainer.setPrefWrapLength(1200); // Largeur augmentée comme dans ListeArticle
    }

    private void setupEventHandlers() {
        prevPageBtn.setOnAction(e -> navigateToPage(currentPage - 1));
        nextPageBtn.setOnAction(e -> navigateToPage(currentPage + 1));
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterArticles(newVal));
    }

    /*
    private void loadFavoriteArticles() {
        try {
            allFavorites = favorieService.getFavoriteArticlesByUser(currentUser.getId());
            totalPages = (int) Math.ceil((double) allFavorites.size() / ITEMS_PER_PAGE);
            updatePageIndicators();
            updateDisplayedArticles();
        } catch (SQLException e) {
            AlertUtils.showErrorAlert("Erreur", "Impossible de charger les favoris", e.getMessage());
        }
    }
*/


    private void loadFavoriteArticles() {
        try {
            int initialCount = allFavorites.size();
            allFavorites = favorieService.getFavoriteArticlesByUser(currentUser.getId());
            totalPages = (int) Math.ceil((double) allFavorites.size() / ITEMS_PER_PAGE);

            // Notifier l'utilisateur si des articles ont été retirés
            if (initialCount > 0 && allFavorites.size() < initialCount) {
                int removedCount = initialCount - allFavorites.size();
                AlertUtils.showInformationAlert("Mise à jour des favoris",
                        removedCount + " article(s) ont été retiré(s) de vos favoris car ils ne sont plus disponibles en stock.");
            }

            updatePageIndicators();
            updateDisplayedArticles();
        } catch (SQLException e) {
            AlertUtils.showErrorAlert("Erreur", "Impossible de charger les favoris", e.getMessage());
        }
    }

    private void navigateToPage(int page) {
        if (page < 1 || page > totalPages) return;

        currentPage = page;
        updateDisplayedArticles();
        updatePageIndicators();
    }

    private void updateDisplayedArticles() {
        int fromIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, allFavorites.size());

        if (allFavorites.isEmpty()) {
            showEmptyState();
        } else {
            displayArticles(allFavorites.subList(fromIndex, toIndex));
        }
    }

    private void showEmptyState() {
        articlesContainer.getChildren().clear();
        Label noResults = new Label("Aucun article favori trouvé");
        noResults.setStyle("-fx-font-size: 16px; -fx-text-fill: gray;");
        articlesContainer.getChildren().add(noResults);
    }

    /*
    private void displayArticles(List<Article> articles) {
        articlesContainer.getChildren().clear();

        for (Article article : articles) {
            VBox card = createArticleCard(article);
            articlesContainer.getChildren().add(card);
            animateCardAppearance(card);
        }

        updateNavigationButtons();
    }
*/

    private void displayArticles(List<Article> articles) {
        articlesContainer.getChildren().clear();

        for (Article article : articles) {
            // Vérification supplémentaire (au cas où)
            if (article.getQuantitestock() > 0) {
                VBox card = createArticleCard(article);
                articlesContainer.getChildren().add(card);
                animateCardAppearance(card);
            }
        }

        updateNavigationButtons();
    }

    private VBox createArticleCard(Article article) {
        VBox card = new VBox();
        card.getStyleClass().add("article-card"); // Utilisation de classe CSS plutôt que style inline

        // Configuration de base de la carte
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        card.setPadding(new Insets(10));
        card.setSpacing(10);

        // Effets de survol gérés par CSS
        card.setOnMouseEntered(e -> card.getStyleClass().add("card-hover"));
        card.setOnMouseExited(e -> card.getStyleClass().remove("card-hover"));

        // Image frame
        StackPane imageFrame = new StackPane();
        imageFrame.getStyleClass().add("card-image-container");
        imageFrame.setMaxWidth(CARD_WIDTH);
        imageFrame.setPrefHeight(160);

        ImageView imageView = createArticleImageView(article);
        imageFrame.getChildren().add(imageView);

        // Contenu de la carte
        Label nameLabel = new Label(article.getNom());
        nameLabel.getStyleClass().add("article-name");
        nameLabel.setMaxWidth(CARD_WIDTH - 20);
        nameLabel.setWrapText(true);

        Label priceLabel = new Label(String.format("%.2f Dt", article.getPrix()));
        priceLabel.getStyleClass().add("article-price");

        HBox buttonBox = createCardButtons(article);

        VBox contentBox = new VBox(8, nameLabel, priceLabel, buttonBox);
        contentBox.getStyleClass().add("card-info-container");
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(0, 10, 10, 10));

        card.getChildren().addAll(imageFrame, contentBox);
        return card;
    }

    private ImageView createArticleImageView(Article article) {
        ImageView imageView = new ImageView();
        Image image;

        try {
            if (article.getImage().startsWith("http") || article.getImage().startsWith("file:")) {
                image = new Image(article.getImage(), true);
            } else {
                // Essayez de charger comme ressource interne
                InputStream is = getClass().getResourceAsStream(article.getImage());
                if (is != null) {
                    image = new Image(is);
                } else {
                    // Essayez de charger comme fichier externe
                    image = new Image("file:" + article.getImage(), true);
                }
            }

            image.errorProperty().addListener((obs, wasError, isNowError) -> {
                if (isNowError) {
                    // Charger l'image par défaut en cas d'erreur
                    imageView.setImage(new Image(getClass().getResourceAsStream("/images/logo.jpg")));
                }
            });

            imageView.setImage(image);
        } catch (Exception e) {
            try {
                imageView.setImage(new Image(getClass().getResourceAsStream("/images/logo.jpg")));
            } catch (Exception ex) {
                System.err.println("Erreur de chargement de l'image par défaut : " + ex.getMessage());
            }
        }

        imageView.setFitWidth(120);
        imageView.setFitHeight(120);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        return imageView;
    }

    private HBox createCardButtons(Article article) {
        Button detailsBtn = createButton("👁", "#2196F3", e -> showArticleDetails(article), "Voir détails");
        Button cartBtn = createButton("🛒", "#4CAF50", e -> addToCart(article), "Ajouter au panier");
        Button removeBtn = createButton("-", "#f44336", e -> removeFromFavorites(article), "Retirer des favoris");

        HBox buttonBox = new HBox(10, detailsBtn, cartBtn, removeBtn);
        buttonBox.setAlignment(Pos.CENTER);
        return buttonBox;
    }

    private Button createButton(String text, String color, javafx.event.EventHandler<ActionEvent> handler, String tooltip) {
        Button button = new Button(text);
        button.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-background-radius: 15;", color));
        button.setMinSize(30, 30);
        button.setPrefSize(30, 30);
        button.setMaxSize(30, 30);
        button.setOnAction(handler);
        button.setTooltip(new Tooltip(tooltip));
        return button;
    }

    private void animateCardAppearance(Node card) {
        FadeTransition ft = new FadeTransition(Duration.millis(300), card);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void updatePageIndicators() {
        pageIndicatorsContainer.getChildren().clear();

        int startPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(totalPages, currentPage + 2);

        if (startPage > 1) {
            addPageIndicator(1);
            if (startPage > 2) addEllipsis();
        }

        for (int i = startPage; i <= endPage; i++) {
            addPageIndicator(i);
        }

        if (endPage < totalPages) {
            if (endPage < totalPages - 1) addEllipsis();
            addPageIndicator(totalPages);
        }

        updateNavigationButtons();
    }

    private void addPageIndicator(int pageNumber) {
        Label indicator = new Label(String.valueOf(pageNumber));
        indicator.getStyleClass().add("page-indicator");

        if (pageNumber == currentPage) {
            indicator.getStyleClass().add("current");
        }

        indicator.setOnMouseClicked(e -> navigateToPage(pageNumber));
        pageIndicatorsContainer.getChildren().add(indicator);
    }

    private void addEllipsis() {
        Label ellipsis = new Label("...");
        ellipsis.getStyleClass().add("page-indicator");
        pageIndicatorsContainer.getChildren().add(ellipsis);
    }

    private void updateNavigationButtons() {
        prevPageBtn.setDisable(currentPage <= 1);
        nextPageBtn.setDisable(currentPage >= totalPages || totalPages == 0);
    }

    private void filterArticles(String keyword) {
        List<Article> filtered = new ArrayList<>();
        for (Article article : allFavorites) {
            if (article.getNom().toLowerCase().contains(keyword.toLowerCase())) {
                filtered.add(article);
            }
        }
        totalPages = (int) Math.ceil((double) filtered.size() / ITEMS_PER_PAGE);
        currentPage = 1;
        updatePageIndicators();
        displayArticles(filtered.subList(0, Math.min(ITEMS_PER_PAGE, filtered.size())));
    }

    private void showArticleDetails(Article article) {
        AlertUtils.showInformationAlert("Détails Article",
                "Nom: " + article.getNom() + "\n" +
                        "Prix: " + String.format("%.2f Dt", article.getPrix()) + "\n" +
                        "Catégorie: " + article.getCategory());
    }

    private void addToCart(Article article) {
        try {
            if (currentUser == null || !"USER".equals(SessionManager.getCurrentUserType())) {
                showLoginAlertAndRedirect();
                return;
            }

            boolean added = panierService.addArticleToPanier(currentUser.getId(), article.getId(), article.getPrix());
            if (added) {
                AlertUtils.showSuccessAlert("Succès", article.getNom() + " ajouté au panier");
                loadFavoriteArticles();

                if (clientDashboardController != null) {
                    clientDashboardController.updateBadges();
                }
            }
        } catch (Exception e) {
            AlertUtils.showErrorAlert("Erreur", "Problème lors de l'ajout au panier", e.getMessage());
        }
    }

    private void removeFromFavorites(Article article) {
        try {
            if (currentUser == null || !"USER".equals(SessionManager.getCurrentUserType())) {
                showLoginAlertAndRedirect();
                return;
            }

            favorieService.removeFromFavorites(article.getId(), currentUser.getId());
            allFavorites = favorieService.getFavoriteArticlesByUser(currentUser.getId());

            totalPages = (int) Math.ceil((double) allFavorites.size() / ITEMS_PER_PAGE);
            if (currentPage > totalPages && totalPages > 0) {
                currentPage = totalPages;
            } else if (totalPages == 0) {
                currentPage = 1;
            }

            updateDisplayedArticles();
            updatePageIndicators();

            if (clientDashboardController != null) {
                clientDashboardController.updateBadges();
            }

            AlertUtils.showSuccessAlert("Succès", article.getNom() + " retiré des favoris");
        } catch (SQLException e) {
            AlertUtils.showErrorAlert("Erreur", "Impossible de retirer l'article", e.getMessage());
        }
    }

    private void showLoginAlertAndRedirect() {
        boolean confirmed = AlertUtils.showConfirmationAlert(
                "Connexion requise",
                "Vous devez être connecté en tant qu'utilisateur pour accéder à cette fonctionnalité",
                "Voulez-vous vous connecter maintenant?"
        );

        if (confirmed) {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
                Stage stage = (Stage) articlesContainer.getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (IOException e) {
                AlertUtils.showErrorAlert("Erreur", "Impossible d'ouvrir la page de connexion", e.getMessage());
            }
        }
    }
}