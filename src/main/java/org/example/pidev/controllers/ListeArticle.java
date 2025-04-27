package org.example.pidev.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.pidev.entities.Article;
import org.example.pidev.entities.List_article;
import org.example.pidev.entities.User;
import org.example.pidev.services.ArticleService;
import org.example.pidev.services.ListeArticleService;
import org.example.pidev.utils.AlertUtils;
import org.example.pidev.utils.MyDatabase;
import org.example.pidev.utils.SessionManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ListeArticle {

    // Composants FXML avec vérification de nullité
    @FXML private TextField searchField;
    @FXML private ListView<String> categoryList;
    @FXML private FlowPane articlesContainer;
    @FXML private Button prevPageBtn;
    @FXML private Button nextPageBtn;
    @FXML private HBox pageIndicatorsContainer;
    @FXML private Label cartBadge;
    @FXML private Button cartIconButton;
    @FXML private Button favoriteIconButton;
    @FXML private Button invoiceIconButton;
    @FXML private Button userIconButton;
    @FXML private Label favoriteBadge;

    // Services
    private final ArticleService articleService = new ArticleService();
    private final ListeArticleService listArticleService = new ListeArticleService();
    private final Connection connection = MyDatabase.getInstance().getConnection();

    // Variables d'état
    private static final int CARDS_PER_PAGE = 6;
    private int currentPage = 1;
    private int totalPages = 1;
    private List<Article> allArticles;
    private User currentUser;
    @FXML private ImageView userImageView;
    @FXML private TextField minPriceInput;
    @FXML private TextField maxPriceInput;
    @FXML private Button filterButton;

    @FXML private Slider minPriceSlider;
    @FXML private Slider maxPriceSlider;
    @FXML private Label minPriceLabel;
    @FXML private Label maxPriceLabel;



    // Styles
    private static final String HOVER_STYLE = "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 2);";
    private static final String NORMAL_STYLE = "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1);";

    private static final Logger logger = Logger.getLogger(ListeArticle.class.getName());

    private ClientDashboardController dashboardController;

    public void setDashboardController(ClientDashboardController controller) {
        this.dashboardController = controller;
    }

    @FXML
    public void initialize() {
        try {
            // Initialiser les sliders avec les prix min/max réels
            double minPrice = articleService.findMinPrice();
            double maxPrice = articleService.findMaxPrice();

            minPriceSlider.setMin(minPrice);
            minPriceSlider.setMax(maxPrice);
            minPriceSlider.setValue(minPrice);

            maxPriceSlider.setMin(minPrice);
            maxPriceSlider.setMax(maxPrice);
            maxPriceSlider.setValue(maxPrice);

            // Formater les valeurs avec espace pour les milliers
            minPriceLabel.setText(String.format("Min: %,.0f DT", minPriceSlider.getValue()));
            maxPriceLabel.setText(String.format("Max: %,.0f DT", maxPriceSlider.getValue()));

            // Écouteurs pour les sliders
            minPriceSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() > maxPriceSlider.getValue()) {
                    maxPriceSlider.setValue(newVal.doubleValue());
                }
                minPriceLabel.setText(String.format("Min: %,.0f DT", newVal.doubleValue()));
            });

            maxPriceSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() < minPriceSlider.getValue()) {
                    minPriceSlider.setValue(newVal.doubleValue());
                }
                maxPriceLabel.setText(String.format("Max: %,.0f DT", newVal.doubleValue()));
            });

        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtils.showErrorAlert("Erreur", "Initialisation des filtres de prix", e.getMessage());
        }

        setupSearchListener();
    }
    private void updatePriceLabels() {
        minPriceLabel.setText(String.format("Min: %.0f DT", minPriceSlider.getValue()));
        maxPriceLabel.setText(String.format("Max: %.0f DT", maxPriceSlider.getValue()));
    }

    @FXML
    private void applyFilters(ActionEvent event) {
        try {
            double minPrice = minPriceSlider.getValue();
            double maxPrice = maxPriceSlider.getValue();

            // Formater les valeurs pour l'affichage
            minPriceLabel.setText(String.format("Min: %,.0f DT", minPrice));
            maxPriceLabel.setText(String.format("Max: %,.0f DT", maxPrice));

            List<Article> filteredArticles = articleService.filterArticlesByPrice(minPrice, maxPrice);
            updateArticlesDisplay(filteredArticles);

        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtils.showErrorAlert("Erreur", "Erreur SQL", "Une erreur est survenue lors du filtrage.");
        }
    }

/*
    private void updateUserProfile(User user) {
        if (userImageView == null) {
            logger.warning("userImageView n'est pas initialisé dans le FXML");
            return;
        }

        try {
            // 1. Essayer de charger depuis le chemin d'image (String)
            if (user.getImage_path() != null && !user.getImage_path().isEmpty()) {
                try {
                    // Solution 1: Charger comme ressource depuis le classpath
                    InputStream imageStream = getClass().getResourceAsStream(user.getImage_path());
                    if (imageStream != null) {
                        userImageView.setImage(new Image(imageStream));
                        return;
                    }

                    // Solution 2: Charger depuis le système de fichiers (chemin absolu)
                    try {
                        userImageView.setImage(new Image("file:" + user.getImage_path()));
                        return;
                    } catch (Exception e) {
                        logger.log(Level.FINE, "Échec du chargement comme chemin absolu", e);
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Erreur de chargement de l'image depuis le chemin", e);
                }
            }

            // 2. Charger une image par défaut
            try {
                InputStream defaultImageStream = getClass().getResourceAsStream("/images/logo.jpg");
                if (defaultImageStream != null) {
                    userImageView.setImage(new Image(defaultImageStream));
                } else {
                    logger.warning("Image par défaut non trouvée");
                    userImageView.setImage(null);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Erreur de chargement de l'image par défaut", e);
                userImageView.setImage(null);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur critique lors de la mise à jour du profil utilisateur", e);
            userImageView.setImage(null);
        }
    }
*/


    private void updateArticlesDisplay(List<Article> articles) {
        Platform.runLater(() -> {
            articlesContainer.getChildren().clear();

            if (articles == null || articles.isEmpty()) {
                Label noResultsLabel = new Label("Aucun article ne correspond à vos critères de filtrage.");
                noResultsLabel.getStyleClass().add("no-results-label");
                articlesContainer.getChildren().add(noResultsLabel);
                return;
            }

            // Mettre à jour la pagination
            allArticles = articles;
            currentPage = 1;
            calculateTotalPages();
            updateNavigationButtons();
            updatePageIndicators();

            // Afficher les articles de la première page
            int fromIndex = 0;
            int toIndex = Math.min(CARDS_PER_PAGE, articles.size());
            displayArticles(articles.subList(fromIndex, toIndex));
        });
    }

    private void setupNumericValidation() {
        // Validation pour minPriceInput
        minPriceInput.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d{0,2})?")) {
                minPriceInput.setText(oldValue);
            }
        });

        // Validation pour maxPriceInput
        maxPriceInput.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d{0,2})?")) {
                maxPriceInput.setText(oldValue);
            }
        });
    }


    @FXML
    public void setCurrentUser(User user) {
        if (user == null) {
            System.out.println("Warning: No user connected");
            return;
        }
        this.currentUser = user;
        //updateUserProfile(user);

        // Initialisation des éléments nécessitant un utilisateur
        try {
            loadData();

            if (cartBadge != null) {
                updateCartBadge();
            }

            if (favoriteBadge != null) {
                updateFavoriteBadge();
            }

            if (userIconButton != null) {
                setupUserMenu();
            }

            // Configuration des boutons
            if (cartIconButton != null) {
                cartIconButton.setOnAction(e -> showCart());
            }
            if (favoriteIconButton != null) {
                favoriteIconButton.setOnAction(e -> showFavorites());
            }
            if (invoiceIconButton != null) {
                invoiceIconButton.setOnAction(e -> navigateToInvoices());
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de l'initialisation avec l'utilisateur", e);
            AlertUtils.showErrorAlert("Erreur", "Initialisation échouée", e.getMessage());
        }
    }

    private void setupUserMenu() {
        try {
            ContextMenu userMenu = new ContextMenu();
            userMenu.getStyleClass().add("user-context-menu");

            MenuItem profileItem = new MenuItem("Profil");
            profileItem.getStyleClass().add("user-menu-item");
            profileItem.setOnAction(e -> handleProfileAction());

            MenuItem settingsItem = new MenuItem("Paramètres");
            settingsItem.getStyleClass().add("user-menu-item");
            settingsItem.setOnAction(e -> handleSettingsAction());

            SeparatorMenuItem separator = new SeparatorMenuItem();
            separator.getStyleClass().add("user-separator");

            MenuItem logoutItem = new MenuItem("Déconnexion");
            logoutItem.getStyleClass().add("user-menu-item");
            logoutItem.setOnAction(e -> handleLogoutAction());

            userMenu.getItems().addAll(profileItem, settingsItem, separator, logoutItem);

            userIconButton.setContextMenu(userMenu);
            userIconButton.setOnMouseClicked(e -> userMenu.show(userIconButton, Side.BOTTOM, 0, 0));

        } catch (Exception e) {
            logger.log(Level.WARNING, "Échec de la configuration du menu utilisateur", e);
        }
    }

    private void handleProfileAction() {
        AlertUtils.showInformationAlert("Profil", "Fonctionnalité profil");
    }

    private void handleSettingsAction() {
        AlertUtils.showInformationAlert("Paramètres", "Fonctionnalité paramètres");
    }

    private void handleLogoutAction() {
        try {
            SessionManager.clearSession();
        } catch (Exception e) {
            AlertUtils.showErrorAlert("Erreur", "Déconnexion échouée", e.getMessage());
        }
    }

    private void loadScene(String fxmlPath) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        Stage stage = (Stage) articlesContainer.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    private void showCart() {
        try {
            loadScene("/Panier.fxml");
        } catch (IOException e) {
            AlertUtils.showErrorAlert("Erreur", "Impossible d'ouvrir le panier", e.getMessage());
        }
    }

    private void showFavorites() {
        try {
            loadScene("/Favorie.fxml");
        } catch (IOException e) {
            AlertUtils.showErrorAlert("Erreur", "Impossible d'ouvrir les favoris", e.getMessage());
        }
    }

    private void navigateToInvoices() {
        try {
            loadScene("/Facture.fxml");
        } catch (IOException e) {
            AlertUtils.showErrorAlert("Erreur", "Impossible d'ouvrir les factures", e.getMessage());
        }
    }

    private void setupArticlesContainer() {
        // Augmenter la largeur du conteneur pour accommoder 4 cartes
        articlesContainer.setPrefWrapLength(1200); // Augmenté de 780 à 1200
        articlesContainer.setHgap(20);
        articlesContainer.setVgap(20);

        // Ajuster la taille des cartes pour qu'elles tiennent 4 par ligne
        articlesContainer.setPrefWidth(1200);
    }

    private void setupPaginationButtons() {
        prevPageBtn.setText("◀");
        nextPageBtn.setText("▶");

        if (prevPageBtn != null && nextPageBtn != null) {
            prevPageBtn.setOnAction(e -> navigateToPage(currentPage - 1));
            nextPageBtn.setOnAction(e -> navigateToPage(currentPage + 1));
        }
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                loadAllArticles();
            } else {
                searchArticles(newVal.trim());
            }
        });
    }

    private void loadData() {
        loadCategories();
        loadAllArticles();
    }
/*
    private void loadCategories() {
        try {
            if (categoryList != null) {
                categoryList.getItems().setAll(articleService.getAllCategories());
                categoryList.getSelectionModel().selectedItemProperty().addListener(
                        (obs, oldVal, newVal) -> filterByCategory(newVal)
                );
            }
        } catch (Exception e) {
            AlertUtils.showErrorAlert("Erreur", "Chargement des catégories", e.getMessage());
        }
    }
*/

    public void loadArticles() {
        loadAllArticles();
    }

    private void loadAllArticles() {
        try {
            allArticles = articleService.getAllArticles();
            updatePagination();
            displayCurrentPage();
        } catch (Exception e) {
            AlertUtils.showErrorAlert("Erreur", "Chargement des articles", e.getMessage());
        }
    }

    private void updatePagination() {
        calculateTotalPages();
        updateNavigationButtons();
        updatePageIndicators();
    }

    private void calculateTotalPages() {
        totalPages = (allArticles == null || allArticles.isEmpty())
                ? 1
                : (int) Math.ceil((double) allArticles.size() / CARDS_PER_PAGE);
    }

    private void updateNavigationButtons() {
        if (prevPageBtn != null && nextPageBtn != null) {
            prevPageBtn.setDisable(currentPage <= 1);
            nextPageBtn.setDisable(currentPage >= totalPages);
        }
    }

    private void updatePageIndicators() {
        if (pageIndicatorsContainer == null) return;

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

    private void navigateToPage(int page) {
        currentPage = page;
        displayCurrentPage();
        updatePagination();
    }

    private void displayCurrentPage() {
        if (articlesContainer == null) return;

        articlesContainer.getChildren().clear();

        if (allArticles == null || allArticles.isEmpty()) {
            showNoArticlesMessage();
            return;
        }

        int fromIndex = (currentPage - 1) * CARDS_PER_PAGE;
        int toIndex = Math.min(fromIndex + CARDS_PER_PAGE, allArticles.size());

        displayArticles(allArticles.subList(fromIndex, toIndex));
    }

    private void showNoArticlesMessage() {
        Label message = new Label("Aucun article trouvé.");
        message.getStyleClass().add("bold-label");
        articlesContainer.getChildren().add(message);
    }

    private void displayArticles(List<Article> articles) {
        articles.forEach(article -> {
            VBox card = createArticleCard(article);
            if (card != null) {
                articlesContainer.getChildren().add(card);
            }
        });
    }

    private VBox createArticleCard(Article article) {
        try {
            StackPane imageFrame = createImageFrame(article);
            VBox contentBox = createContentBox(article);

            VBox card = new VBox(imageFrame, contentBox);
            card.getStyleClass().add("article-card");
            card.setAlignment(Pos.TOP_CENTER);
            card.setSpacing(0);
            card.setPrefWidth(280); // Définir une largeur fixe pour chaque carte

            setupCardHoverEffects(card);
            card.setUserData(article.getId());

            return card;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors de la création de la carte d'article", e);
            return null;
        }
    }

    private void setupCardHoverEffects(VBox card) {
        card.setOnMouseEntered(e -> card.setStyle(HOVER_STYLE));
        card.setOnMouseExited(e -> card.setStyle(NORMAL_STYLE));
    }

    private StackPane createImageFrame(Article article) {
        StackPane frame = new StackPane();
        frame.getStyleClass().add("image-frame");
        frame.setMaxWidth(280); // Augmenté de 240 à 280
        frame.setPrefHeight(160); // Augmenté de 140 à 160

        ImageView imageView = new ImageView();
        try {
            imageView.setImage(new Image(article.getImage(), true));
        } catch (Exception e) {
            try {
                imageView.setImage(new Image(getClass().getResourceAsStream("/images/logo.jpg")));
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Image par défaut non trouvée", ex);
            }
        }

        imageView.getStyleClass().add("article-image");
        imageView.setFitWidth(120);
        imageView.setFitHeight(120);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        frame.getChildren().add(imageView);
        return frame;
    }

    private VBox createContentBox(Article article) {
        Label nameLabel = new Label(article.getNom());
        nameLabel.getStyleClass().add("article-name");
        nameLabel.setMaxWidth(220);
        nameLabel.setWrapText(true);

        Label priceLabel = new Label(String.format("%.2f Dt", article.getPrix()));
        priceLabel.getStyleClass().add("article-price");

        HBox buttonBox = createButtonBox(article);

        VBox contentBox = new VBox(8, nameLabel, priceLabel, buttonBox);
        contentBox.getStyleClass().add("article-content");
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(0, 10, 10, 10));

        return contentBox;
    }

    private HBox createButtonBox(Article article) {
        Button detailsBtn = createCompactIconButton("👁", "details-button",
                e -> showArticleDetails(article), "Voir détails");

        AtomicReference<Button> favBtnRef = new AtomicReference<>();
        Button favBtn = createCompactIconButton("❤", "favorite-button",
                e -> handleFavoriteAction(article, favBtnRef.get()), "Ajouter aux favoris");
        favBtnRef.set(favBtn);

        favBtn.setId("favBtn_" + article.getId());
        updateFavoriteButtonStyle(favBtn, article); // Mise à jour initiale du style

        Button cartBtn = createCompactIconButton("🛒", "cart-button",
                e -> addToCart(article), "Ajouter au panier");

        HBox buttonBox = new HBox(5, detailsBtn, favBtn, cartBtn);
        buttonBox.getStyleClass().add("buttons-container");
        buttonBox.setAlignment(Pos.CENTER);

        return buttonBox;
    }

    private Button createCompactIconButton(String icon, String styleClass,
                                           javafx.event.EventHandler<javafx.event.ActionEvent> handler,
                                           String tooltip) {
        Button btn = new Button(icon);
        btn.getStyleClass().add(styleClass);
        btn.setMinSize(30, 30);
        btn.setPrefSize(30, 30);
        btn.setMaxSize(30, 30);
        btn.setStyle("-fx-padding: 0; -fx-background-radius: 15; -fx-text-fill: white;");
        btn.setOnAction(handler);
        btn.setTooltip(new Tooltip(tooltip));
        return btn;
    }

    private void updateFavoriteButtonStyle(Button favBtn, Article article) {
        try {
            if (currentUser != null && articleService.isArticleInFavorites(currentUser.getId(), article.getId())) {
                Platform.runLater(() -> {
                    favBtn.setStyle("-fx-background-color: #ff0000; -fx-text-fill: white;");
                    favBtn.setTooltip(new Tooltip("Retirer des favoris"));
                });
            } else {
                Platform.runLater(() -> {
                    favBtn.setStyle("-fx-background-color: #800080; -fx-text-fill: white;");
                    favBtn.setTooltip(new Tooltip("Ajouter aux favoris"));
                });
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Erreur lors de la vérification des favoris", e);
        }
    }

    private void showArticleDetails(Article article) {
        try {
            Article fullArticle = articleService.getById(article.getId());
            createArticleDetailsDialog(fullArticle).showAndWait();
        } catch (Exception e) {
            AlertUtils.showErrorAlert("Erreur", "Détails indisponibles", e.getMessage());
        }
    }

    private Dialog<Void> createArticleDetailsDialog(Article article) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails de l'article");
        dialog.setHeaderText(article.getNom());
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.getDialogPane().setContent(createDetailsContent(article));
        dialog.getDialogPane().setPrefSize(600, 350);
        return dialog;
    }

    private HBox createDetailsContent(Article article) {
        ImageView imageView = new ImageView();
        try {
            imageView.setImage(new Image(article.getImage(), true));
        } catch (Exception e) {
            try {
                imageView.setImage(new Image(getClass().getResourceAsStream("/images/logo.jpg")));
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Image par défaut non trouvée", ex);
            }
        }
        imageView.setFitWidth(250);
        imageView.setFitHeight(250);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");

        StackPane imageContainer = new StackPane(imageView);
        imageContainer.setPadding(new Insets(15));
        imageContainer.setStyle("-fx-background-color: #f9f9f9; -fx-border-radius: 5;");

        VBox infoBox = new VBox(15);
        infoBox.setPadding(new Insets(20));
        infoBox.setStyle("-fx-background-color: #ffffff; -fx-border-radius: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 0);");

        Label categoryLabel = createStyledLabel("Catégorie", article.getCategory());
        Label descriptionLabel = createStyledLabel("Description", article.getDescription());
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(350);

        Label priceLabel = createStyledLabel("Prix", String.format("%.2f DT", article.getPrix()));
        priceLabel.setStyle("-fx-text-fill: #2e8b57; -fx-font-weight: bold; -fx-font-size: 16px;");

        Label stockLabel = createStyledLabel("Stock disponible", String.valueOf(article.getQuantitestock()));

        infoBox.getChildren().addAll(categoryLabel, descriptionLabel, priceLabel, stockLabel);

        HBox content = new HBox(30, imageContainer, infoBox);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(25));
        content.setStyle("-fx-background-color: #f5f5f5; -fx-border-radius: 10;");

        return content;
    }

    private Label createStyledLabel(String title, String value) {
        Label label = new Label();
        label.setText(String.format("%s: %s", title, value));
        label.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");
        label.setPadding(new Insets(0, 0, 5, 0));
        return label;
    }

    private void handleFavoriteAction(Article article, Button favBtn) {
        try {
            if (currentUser == null) {
                AlertUtils.showErrorAlert("Erreur", "Veuillez vous connecter",
                        "Vous devez être connecté pour gérer les favoris");
                return;
            }

            boolean isFavorite = articleService.isArticleInFavorites(currentUser.getId(), article.getId());

            if (isFavorite) {
                // Afficher un message que l'article est déjà dans les favoris
                AlertUtils.showInformationAlert("Information",
                        article.getNom() + " est déjà dans vos favoris");
            } else {
                // Ajouter aux favoris
                articleService.ajouterArticleFavori(currentUser.getId(), article.getId());
                AlertUtils.showSuccessAlert("Succès", article.getNom() + " ajouté aux favoris !");

                // Mise à jour du bouton favori
                updateFavoriteButtonStyle(favBtn, article);

                // Mise à jour locale du badge
                updateFavoriteBadge();

                // Mise à jour du badge dans le dashboard
                if (dashboardController != null) {
                    dashboardController.updateFavoritesBadge();
                }

                // Actualiser la page
                loadAllArticles();
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la modification des favoris", e);
            AlertUtils.showErrorAlert("Erreur", "Impossible de modifier les favoris", e.getMessage());
        }
    }

    private void addToCart(Article article) {
        try {
            // Vérification plus robuste de currentUser
            if (currentUser == null || currentUser.getId() == 0) { // Changé de null à 0 pour un ID int
                AlertUtils.showErrorAlert("Erreur", "Veuillez vous connecter",
                        "Vous devez être connecté pour ajouter des articles au panier");
                return;
            }

            if (article.getQuantitestock() <= 0) {
                AlertUtils.showErrorAlert("Erreur", "Stock épuisé", "Cet article est en rupture de stock");
                return;
            }

            List_article existingItem = listArticleService.findArticleInCart(currentUser.getId(), article.getId());

            if (existingItem != null) {
                if (existingItem.getQuantite() + 1 > article.getQuantitestock()) {
                    AlertUtils.showErrorAlert("Erreur", "Stock insuffisant",
                            "Quantité demandée dépasse le stock disponible");
                    return;
                }

                existingItem.setQuantite(existingItem.getQuantite() + 1);
                listArticleService.updateArticleQuantity(existingItem);
                AlertUtils.showSuccessAlert("Succès", "Quantité augmentée");
            } else {
                if (article.getQuantitestock() < 1) {
                    AlertUtils.showErrorAlert("Erreur", "Stock insuffisant",
                            "Stock insuffisant pour cet article");
                    return;
                }

                List_article newItem = new List_article();
                newItem.setArticle(article);
                newItem.setUser(currentUser);
                newItem.setQuantite(1);
                newItem.setPrixUnitaire(article.getPrix());
                listArticleService.ajouterArticleAuPanier(newItem);
                AlertUtils.showSuccessAlert("Succès", "Ajouté au panier");
            }

            // Mise à jour locale du badge
            updateCartBadge();

            // Mise à jour du badge dans le dashboard
            if (dashboardController != null) {
                dashboardController.updateCartBadge();
            }

        } catch (Exception e) {
            AlertUtils.showErrorAlert("Erreur", "Ajout au panier",
                    "Une erreur est survenue lors de l'ajout au panier");
            logger.log(Level.SEVERE, "Erreur dans addToCart", e);
        }
    }

    private void updateCartBadge() {
        try {
            if (cartBadge == null || currentUser == null) return;

            int count = listArticleService.getCartItemCount(currentUser.getId());
            Platform.runLater(() -> {
                cartBadge.setText(String.valueOf(count));
                cartBadge.setVisible(count > 0);
            });
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Erreur lors de la mise à jour du badge panier", e);
            Platform.runLater(() -> cartBadge.setVisible(false));
        }
    }

    private void updateFavoriteBadge() {
        try {
            if (favoriteBadge == null || currentUser == null) return;

            int count = articleService.getFavoriteCount(currentUser.getId());
            Platform.runLater(() -> {
                favoriteBadge.setText(String.valueOf(count));
                favoriteBadge.setVisible(count > 0);
            });
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Erreur lors de la mise à jour du badge favoris", e);
            Platform.runLater(() -> favoriteBadge.setVisible(false));
        }
    }

    private void searchArticles(String searchText) {
        try {
            allArticles = articleService.searchArticles(searchText);
            currentPage = 1;
            updatePagination();
            displayCurrentPage();
        } catch (Exception e) {
            AlertUtils.showErrorAlert("Erreur", "Recherche", e.getMessage());
        }
    }


    @FXML
    public void filterByCategory(String category) {
        try {
            // Vérifiez d'abord que l'utilisateur est connecté
            if (currentUser == null) {
                currentUser = SessionManager.getCurrentUser();
                if (currentUser == null) {
                    AlertUtils.showErrorAlert("Erreur", "Connexion requise",
                            "Vous devez être connecté pour voir les articles");
                    return;
                }
            }

            if (category == null || category.isEmpty() || category.equalsIgnoreCase("Toutes")) {
                loadAllArticles();
            } else {
                allArticles = articleService.getByCategory(category);
                if (allArticles == null || allArticles.isEmpty()) {
                    logger.info("Aucun article trouvé pour la catégorie: " + category);
                    showNoArticlesMessage();
                } else {
                    currentPage = 1;
                    updatePagination();
                    displayCurrentPage();
                }
            }
        } catch (Exception e) {
            AlertUtils.showErrorAlert("Erreur", "Filtrage par catégorie", e.getMessage());
            logger.log(Level.SEVERE, "Erreur lors du filtrage par catégorie: " + category, e);
        }
    }

    @FXML
    public void handleCategoryButtonClick(ActionEvent event) {
        Button sourceButton = (Button) event.getSource();
        String category = (String) sourceButton.getUserData();
        filterByCategory(category);
    }

    private void loadCategories() {
        try {
            if (categoryList != null) {
                // Ajoutez "Toutes" comme première option
                ObservableList<String> categories = FXCollections.observableArrayList("Toutes");
                categories.addAll(articleService.getAllCategories());

                categoryList.setItems(categories);
                categoryList.getSelectionModel().selectedItemProperty().addListener(
                        (obs, oldVal, newVal) -> filterByCategory(newVal)
                );
            }
        } catch (Exception e) {
            AlertUtils.showErrorAlert("Erreur", "Chargement des catégories", e.getMessage());
        }
    }


    private void checkUserSession() {
        if (currentUser == null) {
            currentUser = SessionManager.getCurrentUser();
        }

        if (currentUser == null) {
            Platform.runLater(() -> {
                AlertUtils.showErrorAlert("Session expirée", "Connexion requise",
                        "Votre session a expiré. Veuillez vous reconnecter.");
                try {
                    Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
                    Stage stage = (Stage) articlesContainer.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.show();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Erreur lors du chargement de la page de connexion", e);
                }
            });
        }
    }
}