package org.example.pidev.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Table;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.pidev.entities.*;
import org.example.pidev.services.ArticleService;
import org.example.pidev.services.FactureService;
import org.example.pidev.services.FavorieService;
import org.example.pidev.services.PanierService;
import org.example.pidev.utils.AlertUtils;
import org.example.pidev.utils.MyDatabase;
import org.example.pidev.utils.SessionManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class FactureController implements Initializable {

    private static final int ITEMS_PER_PAGE = 4;
    private static final int CARDS_PER_ROW = 2;
    private static final double CARD_WIDTH = 350;
    private static final double CARD_HEIGHT = 320;
    private static final double HGAP = 30;
    private static final double VGAP = 30;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");


    @FXML private FlowPane facturesContainer;
    @FXML private TextField searchField;
    @FXML private Button prevPageBtn;
    @FXML private HBox pageIndicatorsContainer;
    @FXML private Button nextPageBtn;
    @FXML private DatePicker datePicker;

    private final FactureService factureService = new FactureService(MyDatabase.getInstance().getConnection());
    private final FavorieService favorieService = new FavorieService();
    private final PanierService panierService = new PanierService(MyDatabase.getInstance().getConnection());
    private List<Facture> allFactures = new ArrayList<>();
    private int currentPage = 1;
    private int totalPages = 1;
    private User currentUser;

    private ClientDashboardController dashboardController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUIComponents();
        this.currentUser = SessionManager.getCurrentUser();

        if (currentUser != null) {
            loadFactures();
        } else {
            navigateToLogin();
        }
    }

    public void setDashboardController(ClientDashboardController controller) {
        this.dashboardController = controller;
    }
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (this.currentUser != null) {
            loadFactures();
        } else {
            navigateToLogin();
        }
    }


    private void navigateToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/LoginDashboard.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.show();

            // Fermer la fenêtre actuelle si possible
            if (facturesContainer.getScene() != null && facturesContainer.getScene().getWindow() != null) {
                facturesContainer.getScene().getWindow().hide();
            }
        } catch (IOException e) {
            AlertUtils.showErrorAlert("Erreur", "Impossible de charger la page de connexion", e.getMessage());
        }
    }



    private void setupUIComponents() {
        facturesContainer.setHgap(HGAP);
        facturesContainer.setVgap(VGAP);
        facturesContainer.setAlignment(Pos.CENTER);
        double containerWidth = (CARD_WIDTH * CARDS_PER_ROW) + (HGAP * (CARDS_PER_ROW - 1));
        facturesContainer.setPrefWrapLength(containerWidth);

        setupSearch();
        setupPagination();
        datePicker.setValue(LocalDate.now());
    }

    private void setupPagination() {
        prevPageBtn.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                updateDisplayedFactures();
            }
        });

        nextPageBtn.setOnAction(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                updateDisplayedFactures();
            }
        });
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

    private void showUserMenu(Node anchor) {
        ContextMenu userMenu = new ContextMenu();
        userMenu.getStyleClass().add("user-context-menu");

        MenuItem profileItem = new MenuItem("Profil");
        profileItem.setOnAction(e -> loadView("/profile.fxml"));

        MenuItem settingsItem = new MenuItem("Paramètres");
        settingsItem.setOnAction(e -> loadView("/settings.fxml"));

        MenuItem logoutItem = new MenuItem("Déconnexion");
        logoutItem.setOnAction(e -> loadView("/login.fxml"));

        userMenu.getItems().addAll(profileItem, new SeparatorMenuItem(), settingsItem, logoutItem);
        userMenu.show(anchor, Side.BOTTOM, 0, 0);
    }

    private void loadView(String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
        } catch (IOException e) {
            AlertUtils.showErrorAlert("Erreur", "Impossible de charger la vue", e.getMessage());
            e.printStackTrace();
        }
    }



    private int getTotalCartQuantity() throws SQLException {
        List<List_article> panier = panierService.getPanierForUser(currentUser.getId());
        return panier.stream().mapToInt(List_article::getQuantite).sum();
    }

    private void setupSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> filterFactures(newValue));
    }

    private void filterFactures(String keyword) {
        List<Facture> filtered = allFactures.stream()
                .filter(facture -> String.valueOf(facture.getId()).contains(keyword) ||
                        String.valueOf(facture.getMontant()).contains(keyword) ||
                        facture.getDatetime().format(DATE_FORMATTER).toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());

        totalPages = (int) Math.ceil((double) filtered.size() / ITEMS_PER_PAGE);
        currentPage = 1;
        updatePageIndicators();
        displayFactures(filtered.subList(0, Math.min(ITEMS_PER_PAGE, filtered.size())));
    }


    private void loadFactures() {
        try {
            allFactures = factureService.obtenirFacturesParUtilisateur(currentUser.getId());

            if (allFactures.isEmpty()) {
                AlertUtils.showInformationAlert("Information", "Aucune facture trouvée pour cet utilisateur");
            }

            totalPages = (int) Math.ceil((double) allFactures.size() / ITEMS_PER_PAGE);
            if (totalPages == 0) totalPages = 1;

            updatePageIndicators();
            updateDisplayedFactures();
        } catch (SQLException e) {
            AlertUtils.showErrorAlert("Erreur", "Impossible de charger les factures",
                    "Une erreur de base de données est survenue. Veuillez contacter l'administrateur.");
            allFactures = new ArrayList<>();
            updateDisplayedFactures();
        }
    }

    private Facture getFactureById(int factureId) throws SQLException {
        return factureService.getFactureById(factureId);
    }

    private void updateDisplayedFactures() {
        int fromIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, allFactures.size());

        List<Facture> pageFactures = allFactures.subList(fromIndex, toIndex);
        displayFactures(pageFactures);
        updatePageIndicators();
    }

    private void updatePageIndicators() {
        pageIndicatorsContainer.getChildren().clear();

        for (int i = 1; i <= totalPages; i++) {
            Label pageIndicator = new Label(String.valueOf(i));
            pageIndicator.getStyleClass().add("page-indicator");
            if (i == currentPage) {
                pageIndicator.getStyleClass().add("active");
            }

            final int page = i;
            pageIndicator.setOnMouseClicked(e -> {
                currentPage = page;
                updateDisplayedFactures();
            });

            pageIndicatorsContainer.getChildren().add(pageIndicator);
        }

        prevPageBtn.setDisable(currentPage == 1);
        nextPageBtn.setDisable(currentPage == totalPages || totalPages == 0);
    }

    private void displayFactures(List<Facture> factures) {
        facturesContainer.getChildren().clear();

        if (factures.isEmpty()) {
            Label noResults = new Label("Aucune facture trouvée");
            noResults.setStyle("-fx-font-size: 16px; -fx-text-fill: gray;");
            facturesContainer.getChildren().add(noResults);
            return;
        }

        double containerHeight = (CARD_HEIGHT * 2) + (VGAP * 1);
        facturesContainer.setPrefHeight(containerHeight);

        for (Facture facture : factures) {
            facturesContainer.getChildren().add(createFactureCard(facture));
        }
    }

    private VBox createFactureCard(Facture facture) {
        ImageView imageView = new ImageView();
        try {
            imageView.setImage(new Image(getClass().getResourceAsStream("/images/Facture.png")));
        } catch (Exception e) {
            imageView.setImage(new Image(getClass().getResourceAsStream("/images/logo.jpg")));
        }
        imageView.setFitWidth(80);
        imageView.setFitHeight(80);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCache(true);

        VBox imageContainer = new VBox(imageView);
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.setStyle("-fx-padding: 10; -fx-background-color: #f9f9f9;");

        Label idLabel = new Label("Facture #" + facture.getId());
        idLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Label dateLabel = new Label("Date: " + facture.getDatetime().format(DATE_FORMATTER));
        dateLabel.setStyle("-fx-font-size: 12px;");

        Label montantLabel = new Label(String.format("Montant: %.2f Dt", facture.getMontant()));
        montantLabel.setStyle("-fx-text-fill: #2e8b57; -fx-font-size: 14px; -fx-font-weight: bold;");

        String commandeInfo = "Commande: " + (facture.getCommande() != null ?
                "N°" + facture.getCommande().getId() : "Non spécifiée");
        Label commandeLabel = new Label(commandeInfo);
        commandeLabel.setStyle("-fx-font-size: 12px;");

        VBox infoContainer = new VBox(5, idLabel, dateLabel, montantLabel, commandeLabel);
        infoContainer.setAlignment(Pos.CENTER_LEFT);
        infoContainer.setStyle("-fx-padding: 10;");

        Button detailsBtn = new Button("Détails");
        detailsBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        detailsBtn.setOnAction(e -> showFactureDetails(facture));

        Button downloadBtn = new Button("Télécharger");
        downloadBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        downloadBtn.setOnAction(e -> {
            try {
                downloadFacture(facture.getId());
            } catch (SQLException ex) {
                AlertUtils.showErrorAlert("Erreur", "Erreur lors du téléchargement", ex.getMessage());
            }
        });

        HBox buttonBox = new HBox(10, detailsBtn, downloadBtn);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10, 0, 10, 0));

        VBox card = new VBox(imageContainer, infoContainer, buttonBox);
        card.setAlignment(Pos.TOP_CENTER);
        card.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-width: 1; " +
                "-fx-border-radius: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1);");
        card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        card.setPadding(new Insets(5));
        card.setSpacing(5);

        FadeTransition fadeTransition = new FadeTransition(Duration.millis(500), card);
        fadeTransition.setFromValue(0);
        fadeTransition.setToValue(1);
        fadeTransition.play();

        return card;
    }

    private void showFactureDetails(Facture facture) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails Facture");
        alert.setHeaderText(null);

        VBox vbox = new VBox(10);
        vbox.setStyle("-fx-padding: 10; -fx-spacing: 10;");

        Label title = new Label("Détails de la Facture");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        HBox dateBox = new HBox(5);
        Label dateLabel = new Label("Date:");
        dateLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
        Label dateValue = new Label(facture.getDatetime().format(DATE_FORMATTER));
        dateBox.getChildren().addAll(dateLabel, dateValue);

        HBox montantBox = new HBox(5);
        Label montantLabel = new Label("Montant:");
        montantLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
        Label montantValue = new Label(String.format("%.2f Dt", facture.getMontant()));
        montantBox.getChildren().addAll(montantLabel, montantValue);

        vbox.getChildren().addAll(title, dateBox, montantBox);

        if (facture.getCommande() != null) {
            Separator sep1 = new Separator();
            Label commandeTitle = new Label("Commande associée:");
            commandeTitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
            Label commandeValue = new Label("N°: " + facture.getCommande().getId());
            vbox.getChildren().addAll(sep1, commandeTitle, commandeValue);
        }

        if (facture.getClient() != null) {
            Separator sep2 = new Separator();
            Label clientTitle = new Label("Client:");
            clientTitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
            Label clientValue = new Label(facture.getClient().getName() + " " + facture.getClient().getLastName());
            vbox.getChildren().addAll(sep2, clientTitle, clientValue);
        }

        alert.getDialogPane().setContent(vbox);
        alert.showAndWait();
    }


    private byte[] generatePdfContent(Facture facture) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectMapper mapper = new ObjectMapper();

        try (PdfWriter writer = new PdfWriter(outputStream);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            // 1. En-tête avec logo et informations de l'entreprise
            Table headerTable = new Table(new float[]{1, 3});
            headerTable.setWidth(UnitValue.createPercentValue(100));

            try {
                ImageData logoData = ImageDataFactory.create(getClass().getResource("/images/logo.jpg").toURI().toString());
                com.itextpdf.layout.element.Image logo = new com.itextpdf.layout.element.Image(logoData)
                        .setWidth(80)
                        .setHeight(80);
                headerTable.addCell(new Cell().add(logo).setBorder(null));
            } catch (Exception e) {
                headerTable.addCell(new Cell().add(new Paragraph(" ")).setBorder(null));
            }

            Cell companyInfoCell = new Cell()
                    .add(createParagraph("Autol.Ink", true, 16))
                    .add(createParagraph("123 Rue de l'Innovation", false, 12))
                    .add(createParagraph("Tunis, Tunisie", false, 12))
                    .add(createParagraph("Tél: +216 48 004 881", false, 12))
                    .add(createParagraph("Email: contact@Autol.Ink.com", false, 12))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBorder(null);
            headerTable.addCell(companyInfoCell);
            document.add(headerTable);

            // Séparateur
            document.add(new Paragraph("\n"));
            document.add(new LineSeparator(new SolidLine()).setMarginBottom(10));

            // 2. Titre "FACTURE" et informations principales
            document.add(createParagraph("FACTURE", true, 18)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            // Tableau d'informations client/facture
            float[] infoWidths = {2, 3};
            Table infoTable = new Table(infoWidths);
            infoTable.setMarginBottom(20);

            infoTable.addCell(createCell("Client:", true));
            infoTable.addCell(createCell(facture.getClient() != null ?
                    facture.getClient().getName() + " " + facture.getClient().getLastName() :
                    "Non spécifié", false));

            infoTable.addCell(createCell("Email:", true));
            infoTable.addCell(createCell(facture.getClient() != null ?
                    facture.getClient().getEmail() :
                    "Non spécifié", false));

            infoTable.addCell(createCell("N° Facture:", true));
            infoTable.addCell(createCell(String.valueOf(facture.getId()), false));

            infoTable.addCell(createCell("Date:", true));
            infoTable.addCell(createCell(
                    facture.getDatetime() != null ?
                            facture.getDatetime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) :
                            "Non spécifié",
                    false));

            document.add(infoTable);

            // 3. Détails des articles
            if (facture.getCommande() != null) {
                Commande commande = facture.getCommande();
                ArticleService articleService = new ArticleService();

                try {
                    List<Integer> articleIds = new ArrayList<>();
                    Map<Integer, Integer> quantitesMap = new HashMap<>();

                    // Récupération des IDs d'articles
                    if (commande.getArticleIds() != null && !commande.getArticleIds().isEmpty()) {
                        try {
                            // Solution alternative pour format {"articles": [1,2,3]}
                            JsonNode rootNode = mapper.readTree(commande.getArticleIds());
                            if (rootNode.has("articles")) {
                                articleIds = mapper.convertValue(rootNode.get("articles"), new TypeReference<List<Integer>>() {});
                            } else {
                                // Fallback au format simple [1,2,3]
                                articleIds = mapper.readValue(commande.getArticleIds(), new TypeReference<List<Integer>>() {});
                            }
                        } catch (Exception e) {
                            document.add(createParagraph("Erreur de format pour les IDs d'articles", false, 10)
                                    .setTextAlignment(TextAlignment.CENTER));
                            // Essayez une méthode alternative de parsing
                            articleIds = parseSimpleArticleIds(commande.getArticleIds());
                        }
                    }

                    // Récupération des quantités
                    if (commande.getQuantites() != null && !commande.getQuantites().isEmpty()) {
                        try {
                            // Solution alternative pour format {"quantities": {"1":2, "3":1}}
                            JsonNode rootNode = mapper.readTree(commande.getQuantites());
                            if (rootNode.has("quantities")) {
                                quantitesMap = mapper.convertValue(rootNode.get("quantities"), new TypeReference<Map<Integer, Integer>>() {});
                            } else {
                                // Fallback au format simple {"1":2, "3":1}
                                quantitesMap = mapper.readValue(commande.getQuantites(), new TypeReference<Map<Integer, Integer>>() {});
                            }
                        } catch (Exception e) {
                            document.add(createParagraph("Erreur de format pour les quantités", false, 10)
                                    .setTextAlignment(TextAlignment.CENTER));
                            // Essayez une méthode alternative de parsing
                            quantitesMap = parseSimpleQuantites(commande.getQuantites());
                        }
                    }

                    if (!articleIds.isEmpty()) {
                        float[] columnWidths = {3, 1, 1, 1};
                        Table articlesTable = new Table(columnWidths);
                        articlesTable.setWidth(UnitValue.createPercentValue(100));
                        articlesTable.setMarginTop(20).setMarginBottom(20);

                        // En-têtes du tableau
                        articlesTable.addHeaderCell(createHeaderCell("Article"));
                        articlesTable.addHeaderCell(createHeaderCell("Prix Unitaire"));
                        articlesTable.addHeaderCell(createHeaderCell("Quantité"));
                        articlesTable.addHeaderCell(createHeaderCell("Total"));

                        double totalHT = 0;

                        // Remplissage des articles
                        for (Integer articleId : articleIds) {
                            try {
                                Article article = articleService.getArticleById(articleId);
                                if (article != null) {
                                    Integer quantite = quantitesMap.getOrDefault(articleId, 0);
                                    if (quantite > 0) {
                                        double prixUnitaire = article.getPrix();
                                        double totalArticle = prixUnitaire * quantite;
                                        totalHT += totalArticle;

                                        articlesTable.addCell(createContentCell(article.getNom()));
                                        articlesTable.addCell(createContentCell(String.format("%.2f dt", prixUnitaire)));
                                        articlesTable.addCell(createContentCell(String.valueOf(quantite)));
                                        articlesTable.addCell(createContentCell(String.format("%.2f dt", totalArticle)));
                                    }
                                }
                            } catch (Exception e) {
                                document.add(createParagraph("Erreur avec l'article ID " + articleId, false, 10));
                            }
                        }

                        document.add(articlesTable);

                        // Tableau des totaux
                        float[] totalWidths = {3, 1};
                        Table totalTable = new Table(totalWidths);
                        totalTable.setWidth(UnitValue.createPercentValue(50));
                        totalTable.setHorizontalAlignment(HorizontalAlignment.RIGHT);
                        totalTable.setMarginTop(10);

                        double tva = totalHT * 0.2;
                        double totalTTC = totalHT + tva;

                        totalTable.addCell(createTotalCell("Total HT:", true));
                        totalTable.addCell(createTotalCell(String.format("%.2f dt", totalHT), false));

                        totalTable.addCell(createTotalCell("TVA (20%):", true));
                        totalTable.addCell(createTotalCell(String.format("%.2f dt", tva), false));

                        totalTable.addCell(createTotalCell("Total TTC:", true));
                        totalTable.addCell(createTotalCell(String.format("%.2f dt", totalTTC), false));

                        document.add(totalTable);
                    } else {
                        document.add(createParagraph("Aucun article trouvé dans cette commande.", false, 12)
                                .setTextAlignment(TextAlignment.CENTER));
                    }
                } catch (Exception e) {
                    document.add(createParagraph("Erreur lors du traitement des articles: " + e.getMessage(), false, 12)
                            .setTextAlignment(TextAlignment.CENTER));
                    e.printStackTrace();
                }
            } else {
                document.add(createParagraph("Aucune commande associée à cette facture.", false, 12)
                        .setTextAlignment(TextAlignment.CENTER));
            }

            // Pied de page
            document.add(new Paragraph("\n\n"));
            document.add(new LineSeparator(new SolidLine()));

            Paragraph footer = new Paragraph()
                    .add(createParagraph("Merci pour votre confiance !", false, 10))
                    .add(createParagraph("Pour toute question, contactez-nous à contact@Autol.Ink.com", false, 10))
                    .add(createParagraph("Ou par téléphone: +216 48 004 881", false, 10))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setItalic();

            document.add(footer);
        }

        return outputStream.toByteArray();
    }

    // Méthodes utilitaires
    private List<Integer> parseSimpleArticleIds(String articleIdsJson) {
        List<Integer> result = new ArrayList<>();
        if (articleIdsJson == null || articleIdsJson.trim().isEmpty()) {
            return result;
        }

        try {
            String cleaned = articleIdsJson.replace("[", "").replace("]", "").replace("{", "").replace("}", "").trim();
            if (!cleaned.isEmpty()) {
                String[] parts = cleaned.split(",");
                for (String part : parts) {
                    try {
                        result.add(Integer.parseInt(part.trim()));
                    } catch (NumberFormatException e) {
                        // Ignorer les entrées invalides
                    }
                }
            }
        } catch (Exception e) {
            // Si le parsing échoue, retourner une liste vide
        }
        return result;
    }

    private Map<Integer, Integer> parseSimpleQuantites(String quantitesJson) {
        Map<Integer, Integer> result = new HashMap<>();
        if (quantitesJson == null || quantitesJson.trim().isEmpty()) {
            return result;
        }

        try {
            String cleaned = quantitesJson.replace("{", "").replace("}", "").trim();
            if (!cleaned.isEmpty()) {
                String[] entries = cleaned.split(",");
                for (String entry : entries) {
                    String[] keyValue = entry.split(":");
                    if (keyValue.length == 2) {
                        try {
                            int key = Integer.parseInt(keyValue[0].replace("\"", "").trim());
                            int value = Integer.parseInt(keyValue[1].trim());
                            result.put(key, value);
                        } catch (NumberFormatException e) {
                            // Ignorer les entrées invalides
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Si le parsing échoue, retourner une map vide
        }
        return result;
    }

    private Paragraph createParagraph(String text, boolean bold, int fontSize) {
        String safeText = text != null ? text : "";
        Paragraph paragraph = new Paragraph(safeText);
        paragraph.setFontSize(fontSize);
        if (bold) {
            paragraph.setBold();
        }
        return paragraph;
    }

    private Cell createCell(String text, boolean bold) {
        String safeText = text != null ? text : "";
        Paragraph paragraph = new Paragraph(safeText);
        if (bold) {
            paragraph.setBold();
        }
        return new Cell().add(paragraph)
                .setPadding(5)
                .setBorder(Border.NO_BORDER);
    }

    private Cell createHeaderCell(String text) {
        String safeText = text != null ? text : "";
        return new Cell()
                .add(new Paragraph(safeText).setBold())
                .setBackgroundColor(new DeviceRgb(240, 240, 240))
                .setPadding(5)
                .setTextAlignment(TextAlignment.CENTER);
    }

    private Cell createContentCell(String text) {
        String safeText = text != null ? text : "";
        return new Cell()
                .add(new Paragraph(safeText))
                .setPadding(5)
                .setTextAlignment(TextAlignment.CENTER);
    }

    private Cell createTotalCell(String text, boolean isLabel) {
        String safeText = text != null ? text : "";
        Paragraph paragraph = new Paragraph(safeText);
        if (isLabel) {
            paragraph.setBold();
        }
        return new Cell()
                .add(paragraph)
                .setPadding(5)
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(isLabel ? TextAlignment.LEFT : TextAlignment.RIGHT);
    }

    public boolean downloadFacture(int factureId) throws SQLException {
        try {
            Facture facture = factureService.getFactureById(factureId);
            if (facture == null) {
                AlertUtils.showErrorAlert("Erreur", "La facture #" + factureId , " n'existe pas");
                return false;
            }

            byte[] pdfContent = generatePdfContent(facture);
            String fileName = "facture_" + factureId + ".pdf";
            Path downloadsPath = Paths.get(System.getProperty("user.home"), "Downloads");

            if (!Files.exists(downloadsPath)) {
                Files.createDirectories(downloadsPath);
            }

            Path filePath = downloadsPath.resolve(fileName);
            Files.write(filePath, pdfContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Solution 1 (2 arguments)
            AlertUtils.showInformationAlert("Succès", "La facture a été sauvegardée dans:\n" + filePath);

            return true;
        } catch (Exception e) {
            AlertUtils.showErrorAlert("Erreur", "Échec du téléchargement: " , e.getMessage());
            throw new SQLException("Erreur de téléchargement", e);
        }
    }

    @FXML
    private void filterByDate() {
        LocalDate selectedDate = datePicker.getValue();

        if (selectedDate != null) {
            List<Facture> filtered = allFactures.stream()
                    .filter(facture -> facture.getDatetime().toLocalDate().equals(selectedDate))
                    .collect(Collectors.toList());

            totalPages = (int) Math.ceil((double) filtered.size() / ITEMS_PER_PAGE);
            currentPage = 1;
            updatePageIndicators();
            displayFactures(filtered.subList(0, Math.min(ITEMS_PER_PAGE, filtered.size())));
        } else {
            totalPages = (int) Math.ceil((double) allFactures.size() / ITEMS_PER_PAGE);
            currentPage = 1;
            updatePageIndicators();
            updateDisplayedFactures();
        }
    }

}