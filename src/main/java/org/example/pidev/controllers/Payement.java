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

    private void updateTotalAmountLabel() {
        double amount = totalAmount != null ? totalAmount : 0.0;
        totalAmountLabel.setText(String.format("Montant total: %.2f DT", amount));
    }

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

    // Modifiez showSuccessAlert pour fermer automatiquement la popup
    private void showSuccessAlert(int commandeId, double totalCommande, List<Map<String, Object>> articlesDetails) {
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

            // Fermer la popup après que l'utilisateur a cliqué sur OK
            alert.setOnHidden(event -> {
                // Fermer la fenêtre de paiement
                if (paymentStage != null) {
                    paymentStage.close();
                }

                // Rafraîchir le panier parent si nécessaire
                if (parentController != null) {
                    parentController.refreshCart();
                }

                // Rediriger vers la page d'accueil
                redirectToHome();
            });

            alert.show();

        } catch (Exception e) {
            AlertUtils.showErrorAlert("Erreur", "Problème d'affichage", "Les détails n'ont pas pu être affichés");
            redirectToHome();
        }
    }

    private void processOnlinePayment() {
        processStripePayment();
    }

    public void completeOnlinePayment() {
        try {
            Connection connection = MyDatabase.getInstance().getConnection();
            connection.setAutoCommit(false);

            try {
                // 1. Vérifier le stock
                if (!panierService.verifierStockDisponible(1)) {
                    AlertUtils.showErrorAlert("Stock insuffisant",
                            "Certains articles ne sont plus disponibles",
                            "Veuillez vérifier votre panier.");
                    return;
                }

                System.out.println("Panierrrrrrrrrrrrrrrrrr");

                // 2. Récupérer les détails
                List<Map<String, Object>> articlesDetails = panierService.getArticlesAvecDetails(currentUser.getId());
                double totalCommande = panierService.calculerTotalPanier(currentUser.getId());

                // 3. Créer la commande
                Commande commande = createCommande();
                commande.setModePaiement("card"); // Stripe payment
                commande.setTotal(totalCommande);


                // Enregistrement de la commande et de la facture
                Commande createdCommande = commandeService.createCommande(commande);
                Facture facture = createFacture(createdCommande);
                factureService.ajouterFacture(facture);

                // Vidage du panier
                panierService.viderPanier(currentUser.getId());
                connection.commit();


                // Envoi du SMS de confirmation pour le paiement cash
                if (!phoneField.getText().isEmpty()) {
                    // Créer un utilisateur temporaire avec le numéro saisi
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

                // 7. Afficher succès
                showSuccessAlert(createdCommande.getId(), totalCommande, articlesDetails);

            } catch (Exception e) {
                connection.rollback();
                AlertUtils.showErrorAlert("Erreur SQL", "Erreur lors du paiement", e.getMessage());
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (Exception e) {
            AlertUtils.showErrorAlert("Erreur", "Erreur inattendue", e.getMessage());
        }
    }

    private void processCashPayment() {
        try {
            Connection connection = MyDatabase.getInstance().getConnection();
            connection.setAutoCommit(false);

            try {
                // Vérification du stock
                if (!panierService.verifierStockDisponible(currentUser.getId())) {
                    AlertUtils.showErrorAlert("Stock insuffisant",
                            "Certains articles ne sont plus disponibles",
                            "Veuillez vérifier votre panier.");
                    return;
                }

                // Récupération des détails avant de vider le panier
                List<Map<String, Object>> articlesDetails = panierService.getArticlesAvecDetails(currentUser.getId());
                double totalCommande = panierService.calculerTotalPanier(currentUser.getId());

                // Création de la commande
                Commande commande = createCommande();
                commande.setModePaiement("especes");
                commande.setTotal(totalCommande);

                // Enregistrement de la commande et de la facture
                Commande createdCommande = commandeService.createCommande(commande);
                Facture facture = createFacture(createdCommande);
                factureService.ajouterFacture(facture);

                // Vidage du panier
                panierService.viderPanier(currentUser.getId());
                connection.commit();


                // Envoi du SMS de confirmation pour le paiement cash
                if (!phoneField.getText().isEmpty()) {
                    // Créer un utilisateur temporaire avec le numéro saisi
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

                // Affichage de l'alerte de succès avec fermeture automatique
                showSuccessAlert(createdCommande.getId(), totalCommande, articlesDetails);

            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception e) {
            AlertUtils.showErrorAlert("Erreur", "Erreur lors du paiement", e.getMessage());
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


    private void showStripePaymentForm(String clientSecret) {
        stripePopup.setVisible(true);
        WebEngine webEngine = stripeWebView.getEngine();

        String url = stripeService.createCheckoutSession(totalAmount);
        stripeWebView.getEngine().load(url);
        stripePopup.setVisible(true);

        webEngine.getLoadWorker().exceptionProperty().addListener((obs, oldEx, newEx) -> {
            if (newEx != null) {
                System.err.println("WebView load error: " + newEx.getMessage());
            }
        });

        System.out.println("stripePopup is " + (stripePopup != null ? "NOT null" : "null"));
        System.out.println("Loading Stripe payment form");


        // Java-JS bridge
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaConnector", new JavaConnector());
            }
        });
    }

    @FXML
    private void closeStripePopup() {
        stripePopup.setVisible(false);
    }

    public class JavaConnector {
        public void paymentSuccess() {
            Platform.runLater(() -> {
                closeStripePopup(); // hide popup
                completeOnlinePayment(); // do all the backend work
            });
        }
    }


}