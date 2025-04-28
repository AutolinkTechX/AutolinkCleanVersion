package org.example.pidev.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import netscape.javascript.JSObject;
import org.example.pidev.entities.*;
import org.example.pidev.services.*;
import org.example.pidev.utils.AlertUtils;
import org.example.pidev.utils.MyDatabase;
import org.example.pidev.utils.SessionManager;
import org.example.pidev.utils.SmsService;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import com.stripe.exception.StripeException;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import org.example.pidev.services.StripeService;

public class Payement implements Initializable {

    @FXML private RadioButton onlineRadio;
    @FXML private RadioButton cashRadio;
    @FXML private VBox onlineForm;
    @FXML private VBox cashForm;
    @FXML private Label totalAmountLabel;
    @FXML private TextField cardNumberField;
    @FXML private ComboBox<String> expiryMonthCombo;
    @FXML private ComboBox<String> expiryYearCombo;
    @FXML private TextField cvvField;
    @FXML private TextField fullNameField;
    @FXML private TextArea addressField;
    @FXML private TextField phoneField;
    @FXML private Button confirmPaymentButton;
    @FXML private StackPane cashPopup;
    private StripeService stripeService;
    private Stage stripePaymentStage;
    private String paymentIntentId;
    @FXML private ToggleGroup paymentMethodGroup; // Keep this in controller


    @FXML private StackPane stripePopup;
    @FXML private WebView stripeWebView;



    // Navbar elements
    @FXML private Button favoriteIconButton;
    @FXML private Label favoriteBadge;
    @FXML private Button cartIconButton;
    @FXML private Label cartBadge;
    @FXML private Button invoiceIconButton;
    @FXML private Button userIconButton;
    @FXML private ListView<String> cartItemsList; // Doit correspondre au fx:id dans FXML

    private Double totalAmount;
    private User currentUser;
    private CommandeService commandeService;
    private FactureService factureService;
    private PanierService panierService;
    private FavorieService favorieService;
    private ArticleService articleService;
    private List<List_article> articlesPanier;
    @FXML
    private Button cancelButton;

    private ClientDashboardController dashboardController;

    public void setDashboardController(ClientDashboardController controller) {
        this.dashboardController = controller;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            Connection connection = MyDatabase.getInstance().getConnection();
            this.panierService = new PanierService(connection);
            this.favorieService = new FavorieService();
            this.commandeService = new CommandeService(connection);
            this.factureService = new FactureService(connection);
            this.articleService = new ArticleService();
            this.stripeService = new StripeService();

            this.currentUser = SessionManager.getCurrentUser();
            if (!isValidUser(this.currentUser)) {
                AlertUtils.showErrorAlert("Erreur de session",
                        "Authentification requise",
                        "Veuillez vous connecter pour accéder à cette fonctionnalité");
                redirectToLogin();
                return;
            }

            // Initialize UI components after checking FXML injection
            if (expiryMonthCombo != null && expiryYearCombo != null) {
                setupExpiryDateComboBoxes();
            } else {
                System.err.println("Warning: ComboBoxes not initialized from FXML");
            }

            initializeUIComponents();

        } catch (Exception e) {
            handleUnexpectedError(e);
        }
    }

    // Méthodes auxiliaires pour une meilleure organisation
    private boolean isValidUser(User user) {
        return user != null && user.getId() > 0;
    }

    private void initializeUIComponents() {
        setupPaymentMethods();
        setupExpiryDateComboBoxes();
        setupFormValidation();
        updateBadges();
        setupNavbarActions();
        setupCartItemsList();

        // Initialisation supplémentaire si nécessaire
        if (totalAmountLabel != null) {
            totalAmountLabel.setText("0.00 DT");
        }
    }

    private void handleDatabaseError(SQLException e) {
        AlertUtils.showErrorAlert("Erreur BD",
                "Problème de connexion",
                "Impossible d'accéder à la base de données: " + e.getMessage());
        e.printStackTrace();
        // Potentiellement rediriger vers une page d'erreur
    }

    private void handleUnexpectedError(Exception e) {
        AlertUtils.showErrorAlert("Erreur",
                "Problème technique",
                "Une erreur inattendue est survenue: " + e.getMessage());
        e.printStackTrace();
    }

    public void initData(double totalAmount, User currentUser, List<List_article> articlesPanier) {
        try {
            if (currentUser == null || currentUser.getId() <= 0) {
                throw new IllegalArgumentException("Utilisateur non connecté ou invalide");
            }

            this.totalAmount = totalAmount;
            this.currentUser = currentUser;
            this.articlesPanier = articlesPanier != null ? articlesPanier : new ArrayList<>();

            // Stocker l'utilisateur dans SessionManager au cas où
            SessionManager.setCurrentUser(currentUser);

            Platform.runLater(() -> {
                updateTotalAmountLabel();
                refreshCartItems();
                fullNameField.setText(currentUser.getName() + " " + currentUser.getLastName());
                phoneField.setText(currentUser.getPhone() != 0 ? String.valueOf(currentUser.getPhone()) : "");
            });

        } catch (Exception e) {
            AlertUtils.showErrorAlert("Erreur", "Initialisation échouée", e.getMessage());
            redirectToLogin();
        }
    }

    private void redirectToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/LoginDashboard.fxml"));
            Stage stage = (Stage) confirmPaymentButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            AlertUtils.showErrorAlert("Erreur", "Impossible de rediriger vers la page de connexion", e.getMessage());
        }
    }


    private void setupCartItemsList() {
        cartItemsList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                }
            }
        });

        refreshCartItems();
    }

    /*
    private void refreshCartItems() {
        cartItemsList.getItems().clear();
        totalAmount = 0.0;

        try {
            List<Map<String, Object>> articlesDetails = panierService.getArticlesAvecDetails(currentUser.getId());

            if (!articlesDetails.isEmpty()) {
                for (Map<String, Object> details : articlesDetails) {
                    String nom = (String) details.get("nom");
                    double prix = (double) details.get("prix");
                    int quantite = (int) details.get("quantite");
                    double totalArticle = prix * quantite;

                    totalAmount += totalArticle;

                    String itemText = String.format(
                            "Nom: %s\n" +
                                    "Prix unitaire: %.2f DT\n" +
                                    "Quantité: %d\n" +
                                    "Total article: %.2f DT\n" +
                                    "----------------------------",
                            nom, prix, quantite, totalArticle
                    );

                    cartItemsList.getItems().add(itemText);
                }
            } else {
                cartItemsList.getItems().add("Votre panier est vide");
            }
        } catch (SQLException e) {
            AlertUtils.showErrorAlert("Erreur", "Impossible de charger le panier", e.getMessage());
            cartItemsList.getItems().add("Erreur lors du chargement du panier");
        }

        updateTotalAmountLabel();
    }
*/

    private void setupNavbarActions() {
        // Vérification supplémentaire
        if (favoriteIconButton == null || cartIconButton == null
                || invoiceIconButton == null || userIconButton == null) {
            System.err.println("Certains boutons de la navbar ne sont pas initialisés");
            return;
        }

        favoriteIconButton.setOnAction(event -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Favorie.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) favoriteIconButton.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
                AlertUtils.showErrorAlert("Erreur de navigation", "Impossible d'ouvrir les favoris", e.getMessage());
            }
        });


        cartIconButton.setOnAction(event -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Panier.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) cartIconButton.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
                AlertUtils.showErrorAlert("Erreur de navigation", "Impossible d'ouvrir le panier", e.getMessage());
            }
        });

        invoiceIconButton.setOnAction(event -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Facture.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) invoiceIconButton.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
                AlertUtils.showErrorAlert("Erreur de navigation", "Impossible d'ouvrir les factures", e.getMessage());
            }
        });

        userIconButton.setOnAction(event -> showUserMenu(userIconButton));
    }

    private void showUserMenu(Node anchor) {
        ContextMenu userMenu = new ContextMenu();
        userMenu.getStyleClass().add("user-context-menu");

        MenuItem profileItem = new MenuItem("Profil");
        profileItem.setOnAction(e -> showProfile());

        MenuItem settingsItem = new MenuItem("Paramètres");
        settingsItem.setOnAction(e -> showSettings());

        MenuItem logoutItem = new MenuItem("Déconnexion");
        logoutItem.setOnAction(e -> logout());

        userMenu.getItems().addAll(profileItem, new SeparatorMenuItem(), settingsItem, logoutItem);
        userMenu.show(anchor, Side.BOTTOM, 0, 0);
    }

    private void showProfile() {
        AlertUtils.showInformationAlert("Profil", "Affichage du profil utilisateur");
    }

    private void showSettings() {
        AlertUtils.showInformationAlert("Paramètres", "Affichage des paramètres");
    }

    private void logout() {
        AlertUtils.showInformationAlert("Déconnexion", "Utilisateur déconnecté");
    }

    // Modifiez la méthode updateBadges()
    private void updateBadges() {
        try {
            if (favoriteBadge == null || cartBadge == null) {
                System.err.println("Les badges ne sont pas initialisés - vérifiez votre fichier FXML");
                return;
            }

            int favoriteCount = favorieService.getFavoriteCount(currentUser.getId());
            int cartCount = panierService.getTotalQuantity(currentUser.getId());

            Platform.runLater(() -> {
                favoriteBadge.setText(String.valueOf(favoriteCount));
                favoriteBadge.setVisible(favoriteCount > 0);

                cartBadge.setText(String.valueOf(cartCount));
                cartBadge.setVisible(cartCount > 0);
            });
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour des badges: " + e.getMessage());
        }
    }

    public void initPaiement(Double totalAmount, User currentUser, List<List_article> articlesPanier) {
        this.totalAmount = totalAmount != null ? totalAmount : 0.0;
        this.currentUser = currentUser != null ? currentUser : SessionManager.getCurrentUser();

        if (this.currentUser == null) {
            AlertUtils.showErrorAlert("Erreur", "Aucun utilisateur connecté", "Veuillez vous connecter d'abord");
            redirectToLogin();
            return;
        }

        // Initialiser articlesPanier si nécessaire
        if (articlesPanier != null) {
            this.articlesPanier = articlesPanier;
        } else {
            try {
                this.articlesPanier = panierService.getPanierForUser(this.currentUser.getId());
            } catch (Exception e) {
                this.articlesPanier = new ArrayList<>();
                AlertUtils.showErrorAlert("Erreur", "Impossible de charger le panier", e.getMessage());
            }
        }

        // Vérifier les prix des articles
        for (List_article item : this.articlesPanier) {
            if (item != null && item.getArticle() != null && item.getArticle().getPrix() == null) {
                item.getArticle().setPrix(0.0);
            }
        }

        updateTotalAmountLabel();
        refreshCartItems();
        validateForm();
        updateBadges();
    }
/*
    private void updateTotalAmountLabel() {
        double amount = totalAmount != null ? totalAmount : 0.0;
        totalAmountLabel.setText(String.format("Montant total: %.2f DT", amount));
    }
*/
    private void setupPaymentMethods() {
        paymentMethodGroup = new ToggleGroup();
        onlineRadio.setToggleGroup(paymentMethodGroup);
        cashRadio.setToggleGroup(paymentMethodGroup);
        onlineRadio.setUserData("online");
        cashRadio.setUserData("cash");

        onlineRadio.setSelected(true);
        cashForm.setVisible(false); // Masquer le formulaire cash au départ

        paymentMethodGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                String method = newValue.getUserData().toString();
                onlineForm.setVisible(method.equals("online"));
                cashForm.setVisible(method.equals("cash"));
                validateForm();
            }
        });
    }

    private void setupExpiryDateComboBoxes() {
        expiryMonthCombo.getItems().addAll("01", "02", "03", "04", "05", "06",
                "07", "08", "09", "10", "11", "12");

        int currentYear = LocalDateTime.now().getYear();
        for (int i = 0; i < 10; i++) {
            expiryYearCombo.getItems().add(String.valueOf(currentYear + i));
        }
    }

    private void setupFormValidation() {
        cardNumberField.textProperty().addListener((observable, oldValue, newValue) -> {
            String filteredValue = newValue.replaceAll("[^0-9]", "");
            if (!newValue.equals(filteredValue)) {
                cardNumberField.setText(filteredValue);
            }
            if (filteredValue.length() > 16) {
                cardNumberField.setText(oldValue);
            }
            validateForm();
        });

        cvvField.textProperty().addListener((observable, oldValue, newValue) -> {
            String filteredValue = newValue.replaceAll("[^0-9]", "");
            if (!newValue.equals(filteredValue)) {
                cvvField.setText(filteredValue);
            }
            if (filteredValue.length() > 3) {
                cvvField.setText(oldValue);
            }
            validateForm();
        });

        expiryMonthCombo.valueProperty().addListener((obs, oldVal, newVal) -> validateForm());
        expiryYearCombo.valueProperty().addListener((obs, oldVal, newVal) -> validateForm());

        fullNameField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        addressField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        phoneField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
    }

    private void validateForm() {
        boolean isValid;
        if (onlineRadio.isSelected()) {
            isValid = validateOnlinePaymentForm();
        } else {
            isValid = validateCashPaymentForm();
        }
        confirmPaymentButton.setDisable(!isValid);
    }

    private boolean validateOnlinePaymentForm() {
        return cardNumberField.getText().length() == 16 &&
                expiryMonthCombo.getValue() != null &&
                expiryYearCombo.getValue() != null &&
                cvvField.getText().length() == 3;
    }

    private boolean checkUIComponents() {
        return favoriteIconButton != null && cartIconButton != null
                && invoiceIconButton != null && userIconButton != null
                && favoriteBadge != null && cartBadge != null;
    }

    private boolean validateCashPaymentForm() {
        if (!phoneField.getText().matches("\\d+")) {
            return false;
        }

        return !fullNameField.getText().trim().isEmpty() &&
                !addressField.getText().trim().isEmpty() &&
                !phoneField.getText().trim().isEmpty();
    }

    @FXML
    private void handleConfirmPayment() {
        if (onlineRadio.isSelected()) {
            processOnlinePayment();
        } else {
            // Supprimer handleShowCashForm() et traiter directement le paiement cash
            if (validateCashPaymentForm()) {
                processCashPayment();
            } else {
                AlertUtils.showErrorAlert("Erreur", "Champs manquants",
                        "Veuillez remplir tous les champs obligatoires pour la livraison");
            }
        }
    }

    private void processStripePayment() {
        try {
            // Create payment intent with Stripe
            String clientSecret = stripeService.createPaymentIntent(
                    totalAmount,
                    "eur",
                    "Payment for order from " + currentUser.getName()
            );

            // Show Stripe payment form in a WebView
            showStripePaymentForm(clientSecret);

        } catch (StripeException e) {
            AlertUtils.showErrorAlert("Stripe Error", "Payment Error",
                    "Failed to process payment: " + e.getMessage());
        }
    }

    @FXML
    private void handleShowCashForm() {
        cashPopup.setVisible(true);
    }

    @FXML
    private void handleCancelCash() {
        cashPopup.setVisible(false);
    }

    @FXML
    private void handleConfirmCash() {
        if (!validateCashPaymentForm()) {
            AlertUtils.showErrorAlert("Erreur", "Champs manquants",
                    "Veuillez remplir tous les champs obligatoires");
            return;
        }

        if (!phoneField.getText().matches("^[0-9]{8}$")) {
            AlertUtils.showErrorAlert("Erreur", "Téléphone invalide",
                    "Le numéro de téléphone doit contenir 8 chiffres");
            return;
        }

        cashPopup.setVisible(false);
        processCashPayment();
    }

    private Commande createCommande() throws SQLException {
        Commande commande = new Commande();
        commande.setClient(currentUser);
        commande.setDateCommande(LocalDateTime.now());
        commande.setTotal(panierService.calculerTotalPanier(currentUser.getId()));

        List<Map<String, Object>> articlesDetails = panierService.getArticlesAvecDetails(currentUser.getId());
        List<Integer> articleIds = new ArrayList<>();
        Map<Integer, Integer> quantites = new HashMap<>();

        for (Map<String, Object> details : articlesDetails) {
            int articleId = (int) details.get("article_id");
            int quantite = (int) details.get("quantite");

            articleIds.add(articleId);
            quantites.put(articleId, quantite);
            articleService.mettreAJourStock(articleId, quantite);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            commande.setArticleIds(objectMapper.writeValueAsString(articleIds));
            commande.setQuantites(objectMapper.writeValueAsString(quantites));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erreur lors de la sérialisation des données de commande", e);
        }

        return commande;
    }

    private Facture createFacture(Commande commande) {
        Facture facture = new Facture();
        facture.setCommande(commande);
        facture.setDatetime(LocalDateTime.now());
        facture.setMontant(commande.getTotal());
        facture.setClient(currentUser);
        return facture;
    }

    // Ajoutez ce champ
    private Panier parentController;
    private Stage paymentStage;

    // Ajoutez cette méthode
    public void setParentController(Panier parentController) {
        this.parentController = parentController;
    }

    @FXML private Label tvaLabel;
    @FXML private Label grandTotalLabel;

    // Modifiez la méthode updateTotalAmountLabel
    private void updateTotalAmountLabel() {
        double amount = totalAmount != null ? totalAmount : 0.0;
        double tva = amount * 0.20; // TVA de 20%
        double grandTotal = amount + tva;

        totalAmountLabel.setText(String.format("Montant total: %.2f DT", amount));
        tvaLabel.setText(String.format("TVA (20%%): %.2f DT", tva));
        grandTotalLabel.setText(String.format("Grand Total: %.2f DT", grandTotal));
    }

    // Modifiez la méthode refreshCart pour recalculer le total avec TVA
    private void refreshCartItems() {
        cartItemsList.getItems().clear();
        totalAmount = 0.0;

        try {
            List<Map<String, Object>> articlesDetails = panierService.getArticlesAvecDetails(currentUser.getId());

            if (!articlesDetails.isEmpty()) {
                for (Map<String, Object> details : articlesDetails) {
                    String nom = (String) details.get("nom");
                    double prix = (double) details.get("prix");
                    int quantite = (int) details.get("quantite");
                    double totalArticle = prix * quantite;

                    totalAmount += totalArticle;

                    String itemText = String.format(
                            "Nom: %s\n" +
                                    "Prix unitaire: %.2f DT\n" +
                                    "Quantité: %d\n" +
                                    "Total article: %.2f DT\n" +
                                    "----------------------------",
                            nom, prix, quantite, totalArticle
                    );

                    cartItemsList.getItems().add(itemText);
                }
            } else {
                cartItemsList.getItems().add("Votre panier est vide");
            }
        } catch (SQLException e) {
            AlertUtils.showErrorAlert("Erreur", "Impossible de charger le panier", e.getMessage());
            cartItemsList.getItems().add("Erreur lors du chargement du panier");
        }

        updateTotalAmountLabel();
    }

    // Modifiez la méthode showSuccessAlert pour inclure la TVA et le grand total
    private void showSuccessAlert(int commandeId, double totalCommande, List<Map<String, Object>> articlesDetails) {
        try {
            double tva = totalCommande * 0.20;
            double grandTotal = totalCommande + tva;

            StringBuilder details = new StringBuilder();
            details.append("Détails de la commande #").append(commandeId).append(":\n\n");
            details.append("----------------------------------------\n");

            if (articlesDetails != null && !articlesDetails.isEmpty()) {
                for (Map<String, Object> article : articlesDetails) {
                    String nom = (String) article.get("nom");
                    double prixUnitaire = (double) article.get("prix");
                    int quantite = (int) article.get("quantite");
                    double totalArticle = prixUnitaire * quantite;

                    details.append(String.format(
                            "Nom: %s\n" +
                                    "Prix unitaire: %.2f DT\n" +
                                    "Quantité: %d\n" +
                                    "Total article: %.2f DT\n" +
                                    "----------------------------------------\n",
                            nom, prixUnitaire, quantite, totalArticle
                    ));
                }
            }

            details.append(String.format("\nSOUS-TOTAL: %.2f DT", totalCommande));
            details.append(String.format("\nTVA (20%%): %.2f DT", tva));
            details.append(String.format("\nGRAND TOTAL: %.2f DT", grandTotal));

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Paiement réussi");
            alert.setHeaderText("Votre commande a été enregistrée avec succès");
            alert.setContentText(details.toString());
            alert.getDialogPane().setPrefSize(400, 400);

            alert.setOnHidden(event -> {
                redirectToHome();
            });

            alert.show();
        } catch (Exception e) {
            AlertUtils.showErrorAlert("Erreur", "Problème d'affichage", "Les détails n'ont pas pu être affichés");
            redirectToHome();
        }
    }

    public void completeOnlinePayment() {
        Connection connection = null;
        try {
            // Obtenir une nouvelle connexion
            connection = MyDatabase.getInstance().getConnection();

            // Créer de nouvelles instances des services avec cette connexion
            PanierService panierService = new PanierService(connection);
            CommandeService commandeService = new CommandeService(connection);
            FactureService factureService = new FactureService(connection);
            ArticleService articleService = new ArticleService();

            connection.setAutoCommit(false);

            // 1. Vérifier le stock
            if (!panierService.verifierStockDisponible(currentUser.getId())) {
                AlertUtils.showErrorAlert("Stock insuffisant",
                        "Certains articles ne sont plus disponibles",
                        "Veuillez vérifier votre panier.");
                return;
            }

            // 2. Récupérer les détails
            List<Map<String, Object>> articlesDetails = panierService.getArticlesAvecDetails(currentUser.getId());
            double totalCommande = panierService.calculerTotalPanier(currentUser.getId());

            // 3. Créer la commande
            Commande commande = new Commande();
            commande.setClient(currentUser);
            commande.setDateCommande(LocalDateTime.now());
            commande.setTotal(totalCommande);
            commande.setModePaiement("card");

            List<Integer> articleIds = new ArrayList<>();
            Map<Integer, Integer> quantites = new HashMap<>();

            for (Map<String, Object> details : articlesDetails) {
                int articleId = (int) details.get("article_id");
                int quantite = (int) details.get("quantite");

                articleIds.add(articleId);
                quantites.put(articleId, quantite);
                articleService.mettreAJourStock(articleId, quantite);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            try {
                commande.setArticleIds(objectMapper.writeValueAsString(articleIds));
                commande.setQuantites(objectMapper.writeValueAsString(quantites));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Erreur lors de la sérialisation des données de commande", e);
            }

            // Enregistrement de la commande et de la facture
            Commande createdCommande = commandeService.createCommande(commande);
            Facture facture = new Facture();
            facture.setCommande(createdCommande);
            facture.setDatetime(LocalDateTime.now());
            facture.setMontant(createdCommande.getTotal());
            facture.setClient(currentUser);
            factureService.ajouterFacture(facture);

            // Vidage du panier
            panierService.viderPanier(currentUser.getId());
            connection.commit();

            // Envoi du SMS de confirmation
            if (!phoneField.getText().isEmpty()) {
                User tempUser = new User();
                tempUser.setPhone(Integer.parseInt(phoneField.getText()));
                SmsService.sendPaymentConfirmation(tempUser,
                        String.valueOf(createdCommande.getId()),
                        totalCommande);
            } else if (currentUser.getPhone() != 0) {
                SmsService.sendPaymentConfirmation(currentUser,
                        String.valueOf(createdCommande.getId()),
                        totalCommande);
            }

            // Afficher le succès
            showSuccessAlert(createdCommande.getId(), totalCommande, articlesDetails);

        } catch (Exception e) {
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            AlertUtils.showErrorAlert("Erreur", "Erreur lors du paiement", e.getMessage());
        } finally {
            try {
                if (connection != null) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
/*
    private void processOnlinePayment() {
        try {
            double tva = totalAmount * 0.20;
            double grandTotal = totalAmount + tva;

            String clientSecret = stripeService.createPaymentIntent(
                    grandTotal, // Utilisez le grand total pour le paiement
                    "eur",
                    "Payment for order from " + currentUser.getName()
            );
            showStripePaymentForm(clientSecret);
        } catch (StripeException e) {
            AlertUtils.showErrorAlert("Erreur Stripe", "Échec du paiement", e.getMessage());
        }
    }
*/

    private void processOnlinePayment() {
        try {
            double tva = totalAmount * 0.20;
            double grandTotal = totalAmount + tva;

            String clientSecret = stripeService.createPaymentIntent(
                    grandTotal, // Utilisez le grand total pour le paiement
                    "eur",
                    "Payment for order from " + currentUser.getName()
            );
            showStripePaymentForm(clientSecret);
        } catch (StripeException e) {
            AlertUtils.showErrorAlert("Erreur Stripe", "Échec du paiement", e.getMessage());
        }
    }

    private void processCashPayment() {
        Connection connection = null;
        try {
            connection = MyDatabase.getInstance().getConnection();
            connection.setAutoCommit(false); // Démarrer la transaction

            // 1. Vérifier le stock
            if (!panierService.verifierStockDisponible(currentUser.getId())) {
                AlertUtils.showErrorAlert("Stock insuffisant",
                        "Certains articles ne sont plus disponibles",
                        "Veuillez vérifier votre panier.");
                return;
            }

            // 2. Récupérer les détails du panier
            List<Map<String, Object>> articlesDetails = panierService.getArticlesAvecDetails(currentUser.getId());
            double totalCommande = panierService.calculerTotalPanier(currentUser.getId());
            double tva = totalCommande * 0.20;
            double grandTotal = totalCommande + tva;

            // 3. Créer la commande avec le grand total
            Commande commande = new Commande();
            commande.setClient(currentUser);
            commande.setDateCommande(LocalDateTime.now());
            commande.setTotal(grandTotal); // Utilisez le grand total
            commande.setModePaiement("especes");

            // Sérialiser les articles et quantités
            List<Integer> articleIds = new ArrayList<>();
            Map<Integer, Integer> quantites = new HashMap<>();
            for (Map<String, Object> details : articlesDetails) {
                int articleId = (int) details.get("article_id");
                int quantite = (int) details.get("quantite");
                articleIds.add(articleId);
                quantites.put(articleId, quantite);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            commande.setArticleIds(objectMapper.writeValueAsString(articleIds));
            commande.setQuantites(objectMapper.writeValueAsString(quantites));

            // 4. Enregistrer la commande et la facture
            Commande createdCommande = commandeService.createCommande(commande);
            Facture facture = createFacture(createdCommande);
            factureService.ajouterFacture(facture);

            // 5. Mettre à jour les stocks et vider le panier
            for (Map.Entry<Integer, Integer> entry : quantites.entrySet()) {
                articleService.mettreAJourStock(entry.getKey(), entry.getValue());
            }
            panierService.viderPanier(currentUser.getId());

            // Valider la transaction
            connection.commit();

            // Envoyer la confirmation SMS
            sendPaymentConfirmation(createdCommande.getId(), totalCommande);

            // Fermer les popups
            cashPopup.setVisible(false);
            if (paymentStage != null) {
                paymentStage.close();
            }

            // Actualiser le panier parent
            if (parentController != null) {
                parentController.refreshCart();
            }

            // Afficher la confirmation
            showSuccessAlert(createdCommande.getId(), totalCommande, articlesDetails);

        } catch (Exception e) {
            try {
                // Annuler la transaction en cas d'erreur
                if (connection != null) {
                    connection.rollback();
                }
                AlertUtils.showErrorAlert("Erreur", "Erreur lors du paiement",
                        "Le paiement a échoué : " + e.getMessage() +
                                "\nVotre panier n'a pas été modifié.");
            } catch (SQLException ex) {
                AlertUtils.showErrorAlert("Erreur", "Problème technique",
                        "Une erreur grave est survenue : " + ex.getMessage());
            }
        } finally {
            try {
                if (connection != null) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendPaymentConfirmation(int commandeId, double total) {
        try {
            if (!phoneField.getText().isEmpty()) {
                User tempUser = new User();
                tempUser.setPhone(Integer.parseInt(phoneField.getText()));
                SmsService.sendPaymentConfirmation(tempUser, String.valueOf(commandeId), total);
            } else if (currentUser.getPhone() != 0) {
                SmsService.sendPaymentConfirmation(currentUser, String.valueOf(commandeId), total);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi du SMS: " + e.getMessage());
        }
    }

    private void showSuccessAlerte(int commandeId, double totalCommande, List<Map<String, Object>> articlesDetails) {
        try {
            StringBuilder details = new StringBuilder();
            details.append("Détails de la commande #").append(commandeId).append(":\n\n");
            details.append("----------------------------------------\n");

            if (articlesDetails != null && !articlesDetails.isEmpty()) {
                for (Map<String, Object> article : articlesDetails) {
                    String nom = (String) article.get("nom");
                    double prixUnitaire = (double) article.get("prix");
                    int quantite = (int) article.get("quantite");
                    double totalArticle = prixUnitaire * quantite;

                    details.append(String.format(
                            "Nom: %s\n" +
                                    "Prix unitaire: %.2f DT\n" +
                                    "Quantité: %d\n" +
                                    "Total article: %.2f DT\n" +
                                    "----------------------------------------\n",
                            nom, prixUnitaire, quantite, totalArticle
                    ));
                }
            }

            details.append(String.format("\nTOTAL COMMANDE: %.2f DT", totalCommande));

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Paiement réussi");
            alert.setHeaderText("Votre commande a été enregistrée avec succès");
            alert.setContentText(details.toString());
            alert.getDialogPane().setPrefSize(400, 400);

            // Fermer la fenêtre de paiement après confirmation
            alert.setOnHidden(event -> {
                if (paymentStage != null) {
                    paymentStage.close();
                }

                // Actualiser le panier parent si disponible
                if (parentController != null) {
                    parentController.refreshCart();
                }

                // Rediriger vers l'accueil
                redirectToHome();
            });

            alert.show();

        } catch (Exception e) {
            AlertUtils.showErrorAlert("Erreur", "Problème d'affichage", "Les détails n'ont pas pu être affichés");
            redirectToHome();
        }
    }

    // Ajoutez cette méthode pour récupérer la référence à la fenêtre
    public void setPaymentStage(Stage stage) {
        this.paymentStage = stage;
    }

    private void redirectToHome() {
        try {
            // Charger le ClientDashboard qui contient la navbar
            FXMLLoader dashboardLoader = new FXMLLoader(getClass().getResource("/ClientDashboard.fxml"));
            Parent dashboardRoot = dashboardLoader.load();

            // Obtenir le contrôleur du dashboard
            ClientDashboardController dashboardController = dashboardLoader.getController();

            // Charger le contenu Home
            FXMLLoader homeLoader = new FXMLLoader(getClass().getResource("/Home.fxml"));
            Parent homeContent = homeLoader.load();

            // MODIFICATION ICI - Utiliser VBox au lieu de StackPane
            VBox contentArea = (VBox) dashboardRoot.lookup("#contentArea");

            if (contentArea != null) {
                // Mettre à jour le contentArea avec le contenu Home
                contentArea.getChildren().setAll(homeContent);

                // Mettre à jour les badges
                dashboardController.updateBadges();

                // Obtenir la scène actuelle
                Scene currentScene = getCurrentScene();

                // Mettre à jour la scène
                if (currentScene != null) {
                    currentScene.setRoot(dashboardRoot);
                } else {
                    // Fallback ultime - nouvelle fenêtre
                    Stage stage = new Stage();
                    stage.setScene(new Scene(dashboardRoot));
                    stage.show();
                }
            } else {
                throw new RuntimeException("ContentArea introuvable dans le dashboard");
            }
        } catch (IOException e) {
            System.err.println("Erreur de redirection: " + e.getMessage());
            AlertUtils.showErrorAlert("Erreur", "Redirection impossible",
                    "La commande est validée mais nous n'avons pas pu charger la page d'accueil. Veuillez redémarrer l'application.");
        }
    }

    // Méthode utilitaire pour obtenir la scène actuelle (peut être placée dans la classe)
    private Scene getCurrentScene() {
        if (confirmPaymentButton != null && confirmPaymentButton.getScene() != null) {
            return confirmPaymentButton.getScene();
        }
        if (cashRadio != null && cashRadio.getScene() != null) {
            return cashRadio.getScene();
        }
        // Fallback pour récupérer une scène active
        for (Window window : Window.getWindows()) {
            if (window.isShowing() && window instanceof Stage) {
                return ((Stage) window).getScene();
            }
        }
        return null;
    }

    private void redirectToListeArticles() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ListeArticle.fxml"));
            Parent listeArticleContent = loader.load();

            // Obtenir le contentArea de la scène actuelle
            Scene currentScene = confirmPaymentButton.getScene();
            StackPane contentArea = (StackPane) currentScene.lookup("#contentArea");

            if (contentArea != null) {
                // Vider le contentArea et ajouter le nouveau contenu
                contentArea.getChildren().clear();
                contentArea.getChildren().add(listeArticleContent);
            } else {
                // Fallback si contentArea n'est pas trouvé
                throw new RuntimeException("Zone de contenu introuvable");
            }

        } catch (IOException e) {
            AlertUtils.showErrorAlert("Erreur", "Chargement impossible",
                    "Impossible de charger la liste des articles: " + e.getMessage());
        } catch (Exception e) {
            AlertUtils.showErrorAlert("Erreur", "Problème technique",
                    "Une erreur inattendue est survenue: " + e.getMessage());
        }
    }

    public void navigateToArticles(ActionEvent event) { // Corriger le paramètre
        try {
            // Charger le fichier FXML de la page ListeArticle
            Parent root = FXMLLoader.load(getClass().getResource("/ListeArticle.fxml"));

            // Récupérer la scène actuelle à partir du bouton cliqué
            Scene currentScene = ((Node) event.getSource()).getScene();

            // Remplacer le contenu de la scène actuelle
            currentScene.setRoot(root);

            // Optionnel: redimensionner la fenêtre si nécessaire
            Stage stage = (Stage) currentScene.getWindow();
            stage.sizeToScene();
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtils.showErrorAlert("Erreur", "Impossible d'ouvrir la liste des articles", e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Panier.fxml"));
            Parent panierContent = loader.load();
            Panier panierController = loader.getController();
            panierController.setCurrentUser(currentUser);

            // Retour au contentArea du dashboard
            Scene currentScene = cancelButton.getScene();
            StackPane contentArea = (StackPane) currentScene.lookup("#contentArea");

            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(panierContent);
            } else {
                // Fallback si contentArea n'est pas trouvé
                Stage stage = (Stage) cancelButton.getScene().getWindow();
                stage.setScene(new Scene(panierContent));
            }

        } catch (IOException e) {
            AlertUtils.showErrorAlert("Erreur", "Impossible de retourner au panier", e.getMessage());
        }
    }

    @FXML
    private void handlePaymentMethodChange() {
        boolean isOnline = onlineRadio.isSelected();
        onlineForm.setVisible(isOnline);
        cashForm.setVisible(!isOnline);
    }

/*
    @FXML
    private void handleStripePayment() {
        try {
            String clientSecret = stripeService.createPaymentIntent(
                    totalAmount,
                    "eur",
                    "Payment for order from " + currentUser.getName()
            );
            showStripePaymentForm(clientSecret);
        } catch (StripeException e) {
            AlertUtils.showErrorAlert("Erreur Stripe", "Échec du paiement", e.getMessage());
        }
    }
*/

    @FXML
    private void handleStripePayment() {
        try {
            // Calculer le grand total (total + TVA)
            double tva = totalAmount * 0.20;
            double grandTotal = totalAmount + tva;

            String clientSecret = stripeService.createPaymentIntent(
                    grandTotal, // Utiliser le grand total ici
                    "eur",
                    "Payment for order from " + currentUser.getName()
            );
            showStripePaymentForm(clientSecret);
        } catch (StripeException e) {
            AlertUtils.showErrorAlert("Erreur Stripe", "Échec du paiement", e.getMessage());
        }
    }
    /*
    private void showStripePaymentForm(String clientSecret) {
        stripePopup.setVisible(true);
        WebEngine webEngine = stripeWebView.getEngine();

        // Utilisez des URLs locales pour gérer le résultat
        String successUrl = "http://localhost/success?session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = "http://localhost/cancel";

        String url = stripeService.createCheckoutSession(totalAmount, successUrl, cancelUrl);
        stripeWebView.getEngine().load(url);

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                // Surveiller les changements d'URL pour détecter la complétion
                webEngine.locationProperty().addListener((obs2, oldLocation, newLocation) -> {
                    if (newLocation.contains("success")) {
                        // Extraire l'ID de session de l'URL
                        String sessionId = newLocation.split("session_id=")[1];
                        handleSuccessfulPayment(sessionId);
                    } else if (newLocation.contains("cancel")) {
                        Platform.runLater(() -> {
                            closeStripePopup();
                            AlertUtils.showInformationAlert(
                                    "Paiement annulé",
                                    "Vous avez annulé le processus de paiement"
                            );
                        });
                    }
                });
            }
        });
    }
*/

    private void showStripePaymentForm(String clientSecret) {
        stripePopup.setVisible(true);
        WebEngine webEngine = stripeWebView.getEngine();

        // Calculer le grand total
        double tva = totalAmount * 0.20;
        double grandTotal = totalAmount + tva;

        // Utilisez des URLs locales pour gérer le résultat
        String successUrl = "http://localhost/success?session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = "http://localhost/cancel";

        // Passer le grand total à createCheckoutSession
        String url = stripeService.createCheckoutSession(grandTotal, successUrl, cancelUrl);
        stripeWebView.getEngine().load(url);

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                // Surveiller les changements d'URL pour détecter la complétion
                webEngine.locationProperty().addListener((obs2, oldLocation, newLocation) -> {
                    if (newLocation.contains("success")) {
                        // Extraire l'ID de session de l'URL
                        String sessionId = newLocation.split("session_id=")[1];
                        handleSuccessfulPayment(sessionId);
                    } else if (newLocation.contains("cancel")) {
                        Platform.runLater(() -> {
                            closeStripePopup();
                            AlertUtils.showInformationAlert(
                                    "Paiement annulé",
                                    "Vous avez annulé le processus de paiement"
                            );
                        });
                    }
                });
            }
        });
    }

    private void handleSuccessfulPayment(String sessionId) {
        Platform.runLater(() -> {
            try {
                // Vérifier le statut du paiement avec Stripe
                Session session = Session.retrieve(sessionId);

                if ("paid".equals(session.getPaymentStatus())) {
                    // Fermer la popup Stripe
                    closeStripePopup();

                    // Traiter le paiement
                    completeOnlinePayment();

                    // Fermer aussi le dialog de paiement principal si nécessaire
                    if (paymentStage != null) {
                        paymentStage.close();
                    }
                } else {
                    closeStripePopup();
                    AlertUtils.showErrorAlert(
                            "Paiement incomplet",
                            "Erreur",
                            "Le paiement n'a pas été complété. Statut: " + session.getPaymentStatus()
                    );
                }
            } catch (StripeException e) {
                closeStripePopup();
                AlertUtils.showErrorAlert(
                        "Erreur Stripe",
                        "Problème technique",
                        "Impossible de vérifier le statut du paiement: " + e.getMessage()
                );
            }
        });
    }

    @FXML
    private void closeStripePopup() {
        stripePopup.setVisible(false);
        stripeWebView.getEngine().loadContent(""); // Vider le contenu

        // Optionnel: fermer aussi le dialog principal si nécessaire
        if (paymentStage != null) {
            paymentStage.close();
        }
    }

    public class JavaConnector {
        public void paymentSuccess(String sessionId) {
            Platform.runLater(() -> {
                try {
                    // Vérifier le statut du paiement avec Stripe
                    Session session = Session.retrieve(sessionId);

                    if ("paid".equals(session.getPaymentStatus())) {
                        // Traiter le paiement - cela inclut l'affichage de la confirmation
                        // et la fermeture des popups via completeOnlinePayment()
                        completeOnlinePayment();
                    } else {
                        closeStripePopup();
                        AlertUtils.showErrorAlert(
                                "Paiement incomplet",
                                "Erreur",
                                "Le paiement n'a pas été complété. Statut: " + session.getPaymentStatus()
                        );
                    }
                } catch (StripeException e) {
                    closeStripePopup();
                    AlertUtils.showErrorAlert(
                            "Erreur Stripe",
                            "Problème technique",
                            "Impossible de vérifier le statut du paiement: " + e.getMessage()
                    );
                }
            });
        }

        public void paymentCanceled() {
            Platform.runLater(() -> {
                closeStripePopup();
                AlertUtils.showInformationAlert(
                        "Paiement annulé",
                        "Vous avez annulé le processus de paiement"
                );
            });
        }
    }
}