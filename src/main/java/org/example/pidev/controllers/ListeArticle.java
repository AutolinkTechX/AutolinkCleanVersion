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
import org.example.pidev.utils.TranslationService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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
            // Récupérer les prix min/max réels depuis la base de données
            double minPrice = articleService.findMinPrice();
            double maxPrice = articleService.findMaxPrice();

            // Configurer les sliders avec les valeurs réelles
            minPriceSlider.setMin(minPrice);
            minPriceSlider.setMax(maxPrice);
            minPriceSlider.setValue(minPrice);

            maxPriceSlider.setMin(minPrice);
            maxPriceSlider.setMax(maxPrice);
            maxPriceSlider.setValue(maxPrice);

            // Configurer le pas des sliders
            double increment = (maxPrice - minPrice) / 100;
            minPriceSlider.setMajorTickUnit(increment * 10);
            maxPriceSlider.setMajorTickUnit(increment * 10);
            minPriceSlider.setMinorTickCount(0);
            maxPriceSlider.setMinorTickCount(0);
            minPriceSlider.setSnapToTicks(true);
            maxPriceSlider.setSnapToTicks(true);

            // Mise à jour initiale des labels
            updatePriceLabels();

            // Écouteurs pour les sliders
            minPriceSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() > maxPriceSlider.getValue()) {
                    maxPriceSlider.setValue(newVal.doubleValue());
                }
                updatePriceLabels();
            });

            maxPriceSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() < minPriceSlider.getValue()) {
                    minPriceSlider.setValue(newVal.doubleValue());
                }
                updatePriceLabels();
            });

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de l'initialisation des sliders", e);
            AlertUtils.showErrorAlert("Erreur", "Initialisation des filtres", e.getMessage());
        }

        setupSearchListener();
    }

    private void updatePriceLabels() {
        // Formater les valeurs avec séparateur de milliers
        NumberFormat format = NumberFormat.getInstance();
        format.setGroupingUsed(true);

        minPriceLabel.setText(String.format("Min: %s DT", format.format(minPriceSlider.getValue())));
        maxPriceLabel.setText(String.format("Max: %s DT", format.format(maxPriceSlider.getValue())));
    }

    @FXML
    private void applyFilters(ActionEvent event) {
        try {
            double minPrice = minPriceSlider.getValue();
            double maxPrice = maxPriceSlider.getValue();

            // Validation des prix
            if (minPrice > maxPrice) {
                AlertUtils.showErrorAlert("Erreur", "Valeurs invalides",
                        "Le prix minimum ne peut pas être supérieur au prix maximum");
                return;
            }

            // Formater les valeurs pour l'affichage
            minPriceLabel.setText(String.format("Min: %,.0f DT", minPrice));
            maxPriceLabel.setText(String.format("Max: %,.0f DT", maxPrice));

            // Filtrer les articles
            List<Article> filteredArticles = articleService.filterArticlesByPrice(minPrice, maxPrice);

            // Mettre à jour l'affichage
            updateArticlesDisplay(filteredArticles);

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors du filtrage", e);
            AlertUtils.showErrorAlert("Erreur", "Filtrage",
                    "Une erreur est survenue lors du filtrage des articles");
        }
    }

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

    /*
    private Dialog<Void> createArticleDetailsDialog(Article article) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails de l'article");

        // Boutons
        ButtonType translateButtonType = new ButtonType(TranslationService.translate("Traduire en anglais", "fr"),
                ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(
                translateButtonType,
                new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE));

        // Contenu initial (non traduit)
        HBox content = createDetailsContent(article, false);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefSize(600, 400); // Légèrement plus grand

        // Gestion du bouton de traduction
        Button translateButton = (Button) dialog.getDialogPane().lookupButton(translateButtonType);
        AtomicBoolean isTranslated = new AtomicBoolean(false);

        translateButton.addEventFilter(ActionEvent.ACTION, event -> {
            boolean currentState = isTranslated.get();
            isTranslated.set(!currentState);

            // Mettre à jour le contenu
            HBox newContent = createDetailsContent(article, !currentState);
            dialog.getDialogPane().setContent(newContent);

            // Mettre à jour le titre
            dialog.setTitle(!currentState ?
                    TranslationService.translate("Détails de l'article", "en") :
                    "Détails de l'article");

            // Mettre à jour le texte du bouton
            translateButton.setText(!currentState ?
                    TranslationService.translate("Voir original", "fr") :
                    TranslationService.translate("Traduire en anglais", "fr"));

            event.consume();
        });

        return dialog;
    }
    private HBox createDetailsContent(Article article, boolean isTranslated) {
        try {
            // Partie image (inchangée)
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

            // Préparation des textes avec traduction
            String categoryLabelText = isTranslated ? "Category" : "Catégorie";
            String descriptionLabelText = isTranslated ? "Description" : "Description";
            String priceLabelText = isTranslated ? "Price" : "Prix";
            String stockLabelText = isTranslated ? "Available stock" : "Stock disponible";

            // Création des labels avec les libellés traduits
            Label categoryLabel = new Label();
            categoryLabel.setText(String.format("%s: %s",
                    categoryLabelText,
                    isTranslated ? TranslationService.translate(article.getCategory(), "en") : article.getCategory()));
            categoryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");

            Label descriptionLabel = new Label();
            descriptionLabel.setText(String.format("%s: %s",
                    descriptionLabelText,
                    isTranslated ? TranslationService.translate(article.getDescription(), "en") : article.getDescription()));
            descriptionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");
            descriptionLabel.setWrapText(true);
            descriptionLabel.setMaxWidth(350);

            Label priceLabel = new Label();
            priceLabel.setText(String.format("%s: %.2f %s",
                    priceLabelText,
                    article.getPrix(),
                    isTranslated ? "DT" : "DT"));
            priceLabel.setStyle("-fx-text-fill: #2e8b57; -fx-font-weight: bold; -fx-font-size: 16px;");

            Label stockLabel = new Label();
            stockLabel.setText(String.format("%s: %d",
                    stockLabelText,
                    article.getQuantitestock()));
            stockLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");

            // Conteneur des informations
            VBox infoBox = new VBox(15, categoryLabel, descriptionLabel, priceLabel, stockLabel);
            infoBox.setPadding(new Insets(20));
            infoBox.setStyle("-fx-background-color: #ffffff; -fx-border-radius: 5;");

            // Conteneur principal
            HBox content = new HBox(30, imageContainer, infoBox);
            content.setAlignment(Pos.CENTER);
            content.setPadding(new Insets(25));
            content.setStyle("-fx-background-color: #f5f5f5; -fx-border-radius: 10;");

            // Stocker l'état de traduction dans le conteneur
            content.setUserData(isTranslated);

            return content;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la création du contenu", e);
            AlertUtils.showErrorAlert("Erreur", "Création du contenu", e.getMessage());
            return new HBox();
        }
    }
*/

    private Dialog<Void> createArticleDetailsDialog(Article article) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails de l'article");

        // Appliquer les styles du panneau de dialogue avec une largeur réduite
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles/Article/liste.css").toExternalForm());
        dialogPane.setStyle("-fx-background-color: #ffffff; " +
                "-fx-border-color: #e0e0e0; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 10px; " +
                "-fx-background-radius: 10px; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 15, 0, 0, 0); " +
                "-fx-pref-width: 600px;"); // Largeur réduite de 700px à 600px

        // Styles du panneau d'en-tête
        dialogPane.setHeaderText("Détails de l'article");
        dialogPane.lookup(".header-panel").setStyle(
                "-fx-background-color: #800080; " +
                        "-fx-border-color: #6a0dad; " +
                        "-fx-border-width: 0 0 2px 0; " +
                        "-fx-border-radius: 10px 10px 0 0; " +
                        "-fx-padding: 15px 20px;");
        dialogPane.lookup(".header-panel .label").setStyle(
                "-fx-font-size: 18px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: white;");

        // Content styles
        dialogPane.setContent(createDetailsContent(article, false));
        dialogPane.lookup(".content").setStyle(
                "-fx-padding: 25px; " +
                        "-fx-spacing: 0; " +
                        "-fx-background-color: #f9f9f9;");

        // Button styles
        ButtonType translateButtonType = new ButtonType(TranslationService.translate("Traduire en anglais", "fr"),
                ButtonBar.ButtonData.OTHER);
        ButtonType closeButtonType = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().addAll(translateButtonType, closeButtonType);

        // Apply button styles
        Node closeButton = dialogPane.lookupButton(closeButtonType);
        closeButton.setStyle(
                "-fx-background-color: #f5f5f5; " +
                        "-fx-text-fill: #666; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 8px 20px; " +
                        "-fx-background-radius: 20px; " +
                        "-fx-border-color: #ddd; " +
                        "-fx-border-width: 1px;");

        closeButton.setOnMouseEntered(e -> closeButton.setStyle(
                "-fx-background-color: #e0e0e0; " +
                        "-fx-text-fill: #666; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 8px 20px; " +
                        "-fx-background-radius: 20px; " +
                        "-fx-border-color: #ddd; " +
                        "-fx-border-width: 1px;"));

        closeButton.setOnMouseExited(e -> closeButton.setStyle(
                "-fx-background-color: #f5f5f5; " +
                        "-fx-text-fill: #666; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 8px 20px; " +
                        "-fx-background-radius: 20px; " +
                        "-fx-border-color: #ddd; " +
                        "-fx-border-width: 1px;"));

        // Translate button
        Button translateButton = (Button) dialogPane.lookupButton(translateButtonType);
        translateButton.setStyle(
                "-fx-background-color: #4a90e2; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 8px 20px; " +
                        "-fx-background-radius: 20px; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 3, 0, 0, 1);");

        translateButton.setOnMouseEntered(e -> translateButton.setStyle(
                "-fx-background-color: #3a80d2; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 8px 20px; " +
                        "-fx-background-radius: 20px; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 3, 0, 0, 1);"));

        translateButton.setOnMouseExited(e -> translateButton.setStyle(
                "-fx-background-color: #4a90e2; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 8px 20px; " +
                        "-fx-background-radius: 20px; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 3, 0, 0, 1);"));

        AtomicBoolean isTranslated = new AtomicBoolean(false);
        translateButton.addEventFilter(ActionEvent.ACTION, event -> {
            boolean currentState = isTranslated.get();
            isTranslated.set(!currentState);

            // Update content
            dialogPane.setContent(createDetailsContent(article, !currentState));

            // Update title
            dialog.setTitle(!currentState ?
                    TranslationService.translate("Détails de l'article", "en") :
                    "Détails de l'article");

            // Update button text
            translateButton.setText(!currentState ?
                    TranslationService.translate("Voir original", "fr") :
                    TranslationService.translate("Traduire en anglais", "fr"));

            event.consume();
        });

        return dialog;
    }

    private HBox createDetailsContent(Article article, boolean isTranslated) {
        try {
            // Conteneur d'image avec taille fixe
            ImageView imageView = new ImageView();
            try {
                Image img = new Image(article.getImage(), true);
                imageView.setImage(img);

                // Forcer une taille fixe et centrer l'image
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                imageView.setFitWidth(200);  // Largeur fixe réduite
                imageView.setFitHeight(200); // Hauteur fixe réduite
            } catch (Exception e) {
                try {
                    Image defaultImg = new Image(getClass().getResourceAsStream("/images/logo.jpg"));
                    imageView.setImage(defaultImg);
                    imageView.setFitWidth(200);
                    imageView.setFitHeight(200);
                    imageView.setPreserveRatio(true);
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Image par défaut non trouvée", ex);
                }
            }

            // Style du conteneur d'image
            StackPane imageContainer = new StackPane(imageView);
            imageContainer.setStyle(
                    "-fx-alignment: center; " +
                            "-fx-padding: 15px; " +
                            "-fx-background-color: white; " +
                            "-fx-border-color: #e0e0e0; " +
                            "-fx-border-width: 1px; " +
                            "-fx-border-radius: 8px; " +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1); " +
                            "-fx-min-width: 230px; " +  // Largeur réduite
                            "-fx-max-width: 230px; " +  // Largeur réduite
                            "-fx-min-height: 230px; " + // Hauteur réduite
                            "-fx-max-height: 230px;");  // Hauteur réduite

            // Prepare translated texts
            String categoryLabelText = isTranslated ? "Category" : "Catégorie";
            String descriptionLabelText = isTranslated ? "Description" : "Description";
            String priceLabelText = isTranslated ? "Price" : "Prix";
            String stockLabelText = isTranslated ? "Available stock" : "Stock disponible";

            // Category label
            Label categoryLabel = new Label();
            categoryLabel.setText(String.format("%s: %s",
                    categoryLabelText,
                    isTranslated ? TranslationService.translate(article.getCategory(), "en") : article.getCategory()));
            categoryLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #555;");

            // Description label
            Label descriptionLabel = new Label();
            descriptionLabel.setText(String.format("%s: %s",
                    descriptionLabelText,
                    isTranslated ? TranslationService.translate(article.getDescription(), "en") : article.getDescription()));
            descriptionLabel.setStyle(
                    "-fx-font-size: 14px; " +
                            "-fx-text-fill: #333; " +
                            "-fx-wrap-text: true; " +
                            "-fx-max-width: 350px;");

            // Price label
            Label priceLabel = new Label();
            priceLabel.setText(String.format("%s: %.2f %s",
                    priceLabelText,
                    article.getPrix(),
                    isTranslated ? "DT" : "DT"));
            priceLabel.setStyle(
                    "-fx-font-size: 18px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-text-fill: #800080; " +
                            "-fx-padding: 10px 0;");

            // Stock label
            Label stockLabel = new Label();
            stockLabel.setText(String.format("%s: %d",
                    stockLabelText,
                    article.getQuantitestock()));

            String stockStyle = "-fx-font-size: 14px; -fx-padding: 5px 10px; -fx-background-radius: 10px; -fx-border-radius: 10px; ";
            if (article.getQuantitestock() > 10) {
                stockStyle += "-fx-text-fill: #2e8b57; -fx-background-color: #e8f5e9;";
            } else if (article.getQuantitestock() > 0) {
                stockStyle += "-fx-text-fill: #ff8c00; -fx-background-color: #fff3e0;";
            } else {
                stockStyle += "-fx-text-fill: #e74c3c; -fx-background-color: #ffebee;";
            }
            stockLabel.setStyle(stockStyle);

            // Conteneur d'informations avec largeur ajustée
            VBox infoBox = new VBox(15, categoryLabel, descriptionLabel, priceLabel, stockLabel);
            infoBox.setStyle(
                    "-fx-spacing: 15px; " +
                            "-fx-alignment: top-left; " +
                            "-fx-pref-width: 320px; " + // Largeur ajustée
                            "-fx-padding: 20px; " +
                            "-fx-background-color: white; " +
                            "-fx-border-color: #e0e0e0; " +
                            "-fx-border-width: 1px; " +
                            "-fx-border-radius: 8px;");

            // Conteneur principal
            HBox content = new HBox(20, imageContainer, infoBox); // Espacement réduit entre l'image et les infos
            content.setStyle(
                    "-fx-alignment: center; " +
                            "-fx-padding: 20px; " + // Padding réduit
                            "-fx-background-color: #f5f5f5; " +
                            "-fx-border-radius: 10px;");

            return content;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la création du contenu", e);
            AlertUtils.showErrorAlert("Erreur", "Création du contenu", e.getMessage());
            return new HBox();
        }
    }

    private Label createTranslatedLabel(String frenchLabel, String englishLabel, String value, boolean isEnglish) {
        Label label = new Label();
        label.setText(String.format("%s: %s", isEnglish ? englishLabel : frenchLabel, value));
        label.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");
        label.setPadding(new Insets(0, 0, 5, 0));
        return label;
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