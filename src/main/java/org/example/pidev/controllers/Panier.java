package org.example.pidev.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.pidev.entities.*;
import org.example.pidev.services.*;
import org.example.pidev.utils.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;

public class Panier implements Initializable {

    // Éléments UI
    @FXML private GridPane cartGrid;
    @FXML private Button prevButton, nextButton, passerCommandeButton;
    @FXML private Label pageLabel, totalLabel, subtotalLabel, shippingLabel;
    @FXML private VBox mainContainer, emptyCartMessage;

    // Services et données
    private final PanierService panierService = new PanierService(MyDatabase.getInstance().getConnection());
    private final FavorieService favorieService = new FavorieService();
    private List<List_article> panierItems;
    private List<List<List_article>> paginatedItems;
    private int currentPage = 0;
    private double subtotal = 0.0;
    private static final double SHIPPING_COST = 7.0;
    private User currentUser;
    private ClientDashboardController dashboardController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            // Vérification des éléments critiques
            if (mainContainer == null || emptyCartMessage == null) {
                throw new IllegalStateException("Éléments UI critiques non initialisés");
            }

            // Configuration initiale
            showEmptyCart(); // Afficher l'état vide par défaut
            setupUI();

            // Vérification de l'utilisateur
            checkUserAndLoadCart();

        } catch (Exception e) {
            showAlert("Erreur Critique", "Échec d'initialisation: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
            Platform.exit();
        }
    }

    private void checkUserAndLoadCart() {
        this.currentUser = SessionManager.getCurrentUser();

        // Vérification depuis le dashboard si nécessaire
        if (this.currentUser == null && dashboardController != null) {
            this.currentUser = dashboardController.getCurrentUser();
        }

        if (this.currentUser == null) {
            showAlert("Non connecté", "Veuillez vous connecter pour accéder au panier", Alert.AlertType.WARNING);
            navigateToLogin();
            return;
        }

        loadPanierItems();
    }

    private void setupUI() {
        if (passerCommandeButton != null) {
            passerCommandeButton.setOnAction(event -> handlePasserCommande(event));
        }
        if (prevButton != null) {
            prevButton.setOnAction(event -> showPreviousPage());
        }
        if (nextButton != null) {
            nextButton.setOnAction(event -> showNextPage());
        }
    }


    public void setCurrentUser(User user) {
        if (user == null) {
            showAlert("Erreur", "Utilisateur invalide", Alert.AlertType.ERROR);
            navigateToLogin();
            return;
        }

        this.currentUser = user;
        loadPanierItems();
    }

    public void setDashboardController(ClientDashboardController controller) {
        this.dashboardController = controller;
        // Si le currentUser n'est pas encore défini, essayer de le récupérer
        if (this.currentUser == null && controller != null) {
            this.currentUser = controller.getCurrentUser();
        }
    }

    public void loadPanierItems() {
        try {
            if (currentUser == null) {
                showAlert("Erreur", "Utilisateur non connecté", Alert.AlertType.ERROR);
                navigateToLogin();
                return;
            }

            panierItems = panierService.getPanierForUser(currentUser.getId());

            if (panierItems == null) {
                throw new Exception("Le service panier a retourné une valeur nulle");
            }

            processCartItems();

        } catch (Exception e) {
            showAlert("Erreur", "Échec du chargement du panier: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
            showEmptyCart();
        }
    }

    private void processCartItems() {
        if (cartGrid == null) {
            System.err.println("Erreur: cartGrid n'est pas initialisé");
            return;
        }

        // Configurer 3 colonnes égales
        cartGrid.getColumnConstraints().clear();
        for (int i = 0; i < 3; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(33.33);
            cartGrid.getColumnConstraints().add(column);
        }

        cartGrid.getChildren().clear();
        subtotal = 0.0;

        if (panierItems == null || panierItems.isEmpty()) {
            showEmptyCart();
            return;
        }

        showCartContent();
        calculateTotals();
        setupPagination();
        showPage(currentPage);
    }

    private void showCartContent() {
        if (emptyCartMessage != null) emptyCartMessage.setVisible(false);
        if (mainContainer != null) mainContainer.setVisible(true);

        for (List_article item : panierItems) {
            subtotal += item.getPrixUnitaire() * item.getQuantite();
        }
    }

    private void showEmptyCart() {
        if (emptyCartMessage != null) emptyCartMessage.setVisible(true);
        if (mainContainer != null) mainContainer.setVisible(false);

        if (subtotalLabel != null) subtotalLabel.setText("0.00 DT");
        if (shippingLabel != null) shippingLabel.setText("0.00 DT");
        if (totalLabel != null) totalLabel.setText("0.00 DT");
    }

    private void calculateTotals() {
        if (subtotalLabel != null) subtotalLabel.setText(String.format("%.2f DT", subtotal));
        if (shippingLabel != null) shippingLabel.setText(String.format("%.2f DT", SHIPPING_COST));
        if (totalLabel != null) totalLabel.setText(String.format("%.2f DT", subtotal + SHIPPING_COST));
    }

    private void setupPagination() {
        paginatedItems = new ArrayList<>();
        if (panierItems == null || panierItems.isEmpty()) return;

        int itemsPerPage = 6; // 6 cartes par page (2 lignes de 3)
        for (int i = 0; i < panierItems.size(); i += itemsPerPage) {
            int end = Math.min(i + itemsPerPage, panierItems.size());
            paginatedItems.add(panierItems.subList(i, end));
        }
    }

    private void showPage(int page) {
        if (paginatedItems == null || paginatedItems.isEmpty() || cartGrid == null) return;

        page = Math.max(0, Math.min(page, paginatedItems.size() - 1));
        currentPage = page;

        cartGrid.getChildren().clear();
        List<List_article> currentPageItems = paginatedItems.get(page);

        // Configuration pour 3 colonnes
        cartGrid.getColumnConstraints().clear();
        for (int i = 0; i < 3; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(33.33);
            cartGrid.getColumnConstraints().add(column);
        }

        // Ajout des cartes dans la grille (3 colonnes, 2 lignes max)
        for (int i = 0; i < currentPageItems.size(); i++) {
            List_article item = currentPageItems.get(i);
            cartGrid.add(createCartItemCard(item), i % 3, i / 3);
        }

        updatePaginationUI();
    }


    private HBox createCartItemCard(List_article item) {
        HBox card = new HBox(10); // Espacement réduit
        card.getStyleClass().add("cart-item");
        card.setMaxWidth(280); // Largeur réduite pour 3 cartes

        // Image avec taille réduite
        ImageView imageView = new ImageView(loadItemImage(item));
        imageView.setFitWidth(80); // Taille réduite
        imageView.setFitHeight(80);

        StackPane imageContainer = new StackPane(imageView);
        imageContainer.getStyleClass().add("cart-item-image-container");
        imageContainer.setPrefSize(90, 90); // Conteneur plus petit

        // Détails
        VBox details = new VBox(5); // Espacement réduit
        details.getStyleClass().add("cart-item-details");
        details.getChildren().addAll(
                createItemNameLabel(item),
                createPriceLabel(item),
                createQuantityControls(item),
                createSubtotalLabel(item),
                createActionButtons(item)
        );

        card.getChildren().addAll(imageContainer, details);
        return card;
    }
    private Image loadItemImage(List_article item) {
        try {
            String imageUrl = item.getArticle().getImage();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                if (imageUrl.startsWith("http")) {
                    return new Image(imageUrl);
                } else {
                    InputStream stream = getClass().getResourceAsStream(imageUrl);
                    return stream != null ? new Image(stream) : loadDefaultImage();
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur de chargement d'image: " + e.getMessage());
        }
        return loadDefaultImage();
    }

    private Image loadDefaultImage() {
        return new Image(getClass().getResourceAsStream("/images/logo.jpg"));
    }

    private Label createItemNameLabel(List_article item) {
        Label label = new Label(item.getArticle().getNom());
        label.getStyleClass().add("cart-item-name");
        return label;
    }

    private Label createPriceLabel(List_article item) {
        return new Label(String.format("Prix: %.2f DT", item.getPrixUnitaire()));
    }

    private HBox createQuantityControls(List_article item) {
        HBox box = new HBox(10);

        Button minusBtn = new Button("-");
        minusBtn.setOnAction(e -> adjustQuantity(item, -1));

        Label quantity = new Label(String.valueOf(item.getQuantite()));

        Button plusBtn = new Button("+");
        plusBtn.setOnAction(e -> adjustQuantity(item, 1));

        box.getChildren().addAll(minusBtn, quantity, plusBtn);
        return box;
    }

    private Label createSubtotalLabel(List_article item) {
        return new Label(String.format("Total: %.2f DT",
                item.getPrixUnitaire() * item.getQuantite()));
    }

    private HBox createActionButtons(List_article item) {
        HBox box = new HBox(10);

        Button removeBtn = new Button("Retirer");
        removeBtn.setOnAction(e -> removeItem(item.getId()));

        Button saveBtn = new Button("Enregistrer");
        saveBtn.setOnAction(e -> saveForLater(item));

        box.getChildren().addAll(removeBtn, saveBtn);
        return box;
    }

    private void updatePaginationUI() {
        if (pageLabel == null || prevButton == null || nextButton == null) return;

        pageLabel.setText(String.format("Page %d/%d",
                currentPage + 1, paginatedItems.size()));
        prevButton.setDisable(currentPage == 0);
        nextButton.setDisable(currentPage == paginatedItems.size() - 1);
    }

    private void showPreviousPage() {
        showPage(currentPage - 1);
    }

    private void showNextPage() {
        showPage(currentPage + 1);
    }

    private void adjustQuantity(List_article item, int change) {
        try {
            int newQty = item.getQuantite() + change;
            if (newQty < 1) {
                showAlert("Erreur", "Quantité minimale 1", Alert.AlertType.WARNING);
                return;
            }

            if (panierService.updateArticleQuantity(item.getId(), newQty)) {
                item.setQuantite(newQty);
                refreshCart();
            }
        } catch (Exception e) {
            showAlert("Erreur", "Impossible de modifier la quantité", Alert.AlertType.ERROR);
        }
    }

    private void removeItem(int itemId) {
        try {
            if (panierService.removeArticleFromPanier(itemId)) {
                showAlert("Succès", "Article retiré", Alert.AlertType.INFORMATION);
                refreshCart();
            }
        } catch (Exception e) {
            showAlert("Erreur", "Échec de suppression", Alert.AlertType.ERROR);
        }
    }

    private void saveForLater(List_article item) {
        try {
            favorieService.addToFavorites(currentUser.getId(), item.getArticle().getId());
            if (panierService.removeArticleFromPanier(item.getId())) {
                showAlert("Succès", "Article enregistré", Alert.AlertType.INFORMATION);
                refreshCart();
            }
        } catch (Exception e) {
            showAlert("Erreur", "Échec d'enregistrement", Alert.AlertType.ERROR);
        }
    }


    private void showPaymentPopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Payement.fxml"));
            Parent paymentContent = loader.load();
            Payement paymentController = loader.getController();
            paymentController.initData(calculerTotalPanier(), currentUser, panierItems);
            paymentController.setDashboardController(this.dashboardController);
            paymentController.setParentController(this);

            Stage paymentStage = new Stage();
            paymentStage.setTitle("Paiement");
            paymentStage.setScene(new Scene(paymentContent));
            paymentStage.initModality(Modality.APPLICATION_MODAL);

            // Passez la référence de la fenêtre au contrôleur
            paymentController.setPaymentStage(paymentStage);

            paymentStage.showAndWait();

            // Après la fermeture de la popup
            loadPanierItems();
            if (dashboardController != null) {
                dashboardController.updateBadges();
            }
        } catch (Exception e) {
            showAlert("Erreur", "Échec de l'ouverture du formulaire de paiement", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    // Modifiez handlePasserCommande pour utiliser cette méthode
    @FXML
    private void handlePasserCommande(ActionEvent event) {
        try {
            if (currentUser == null) {
                showAlert("Erreur", "Vous devez être connecté", Alert.AlertType.ERROR);
                navigateToLogin();
                return;
            }

            if (panierItems == null || panierItems.isEmpty()) {
                showAlert("Panier vide", "Votre panier est vide", Alert.AlertType.WARNING);
                return;
            }

            showPaymentPopup(); // Utilisez la nouvelle méthode
        } catch (Exception e) {
            showAlert("Erreur", "Échec du passage de commande: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    // Ajoutez cette méthode pour permettre à PayementController de rafraîchir le panier
    public void refreshCart() {
        Platform.runLater(() -> {
            loadPanierItems();
            if (dashboardController != null) {
                dashboardController.updateBadges();
            }
        });
    }

    private boolean verifierStockDisponible() {
        try {
            for (List_article item : panierItems) {
                if (item.getQuantite() > item.getArticle().getQuantitestock()) {
                    showAlert("Stock insuffisant",
                            "L'article " + item.getArticle().getNom() + " n'a pas assez de stock",
                            Alert.AlertType.WARNING);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            showAlert("Erreur", "Impossible de vérifier le stock", Alert.AlertType.ERROR);
            return false;
        }
    }

    private double calculerTotalPanier() {
        if (panierItems == null || panierItems.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;
        for (List_article item : panierItems) {
            total += item.getPrixUnitaire() * item.getQuantite();
        }
        return total + SHIPPING_COST; // Inclure les frais de livraison
    }

    private void navigateToLogin() {
        try {
            // Trouver la scène actuelle de manière plus robuste
            Scene currentScene = passerCommandeButton != null ? passerCommandeButton.getScene()
                    : (prevButton != null ? prevButton.getScene() : null);

            if (currentScene != null) {
                Stage currentStage = (Stage) currentScene.getWindow();
                currentStage.close();

                // Ouvre la fenêtre de login
                Stage loginStage = new Stage();
                Parent root = FXMLLoader.load(getClass().getResource("/LoginDashboard.fxml"));
                loginStage.setScene(new Scene(root));
                loginStage.show();
            }
        } catch (Exception e) {
            System.err.println("Échec critique de navigation: " + e.getMessage());
            e.printStackTrace();
            Platform.exit();
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}