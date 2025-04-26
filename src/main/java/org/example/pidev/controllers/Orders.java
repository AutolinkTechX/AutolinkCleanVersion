package org.example.pidev.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.pidev.entities.Article;
import org.example.pidev.entities.Commande;
import org.example.pidev.services.ArticleService;
import org.example.pidev.services.CommandeService;
import org.example.pidev.utils.MyDatabase;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import java.awt.Desktop;
import java.util.List;
import java.util.stream.Collectors;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;

import com.itextpdf.layout.borders.Border;

public class Orders {
    @FXML
    private FlowPane cardsContainer;
    @FXML
    private DatePicker datePicker;
    @FXML
    private Button prevPageBtn;
    @FXML
    private Button nextPageBtn;
    @FXML
    private Label pageInfoLabel;

    private final CommandeService commandeService = new CommandeService(MyDatabase.getInstance().getConnection());
    private final int ITEMS_PER_PAGE = 6;
    private int currentPage = 1;
    private int totalPages = 1;
    private LocalDate searchDate = null;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @FXML
    public void initialize() {
        loadOrders();
    }

    private void loadOrders() {
        cardsContainer.getChildren().clear();

        List<Commande> commandes;
        if (searchDate != null) {
            commandes = commandeService.getCommandesByDate(searchDate, currentPage, ITEMS_PER_PAGE);
            totalPages = (int) Math.ceil((double) commandeService.getCommandesCountByDate(searchDate) / ITEMS_PER_PAGE);
        } else {
            commandes = commandeService.getAllCommandes(currentPage, ITEMS_PER_PAGE);
            totalPages = (int) Math.ceil((double) commandeService.getCommandesCount() / ITEMS_PER_PAGE);
        }

        for (Commande commande : commandes) {
            VBox card = createOrderCard(commande);
            cardsContainer.getChildren().add(card);
        }

        updatePaginationControls();
    }

    private VBox createOrderCard(Commande commande) {
        VBox card = new VBox();
        card.getStyleClass().add("order-card");
        card.setPrefSize(300, 350);
        card.setSpacing(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 0);");
        // Part 1: Default image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(300);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(false);

        try {
            InputStream is = getClass().getResourceAsStream("/images/Facture.png");
            if (is != null) {
                imageView.setImage(new javafx.scene.image.Image(is));
                is.close();
            } else {
                // Fallback using URL
                URL imageUrl = getClass().getResource("/images/Facture.png");
                if (imageUrl != null) {
                    imageView.setImage(new javafx.scene.image.Image(imageUrl.toString()));
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading image: " + e.getMessage());
            imageView.setImage(createPlaceholderImage(300, 150));
        }

        // Part 2: Client info
        Label clientLabel = new Label("Client: " + commande.getClient().getName() + " " + commande.getClient().getLastName());
        clientLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-padding: 0 10 0 10;");

        Label dateLabel = new Label("Date: " + commande.getDateCommande().toLocalDate());
        dateLabel.setStyle("-fx-font-size: 14; -fx-padding: 0 10 0 10;");

        Label totalLabel = new Label("Total: " + commande.getTotal() + " DT");
        totalLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-padding: 0 10 0 10;");

        // Part 3: Buttons
        Button detailsBtn = new Button("Details");
        detailsBtn.setStyle("-fx-background-color: #ca8a62; -fx-text-fill: white; -fx-font-size: 14;");
        detailsBtn.setOnAction(event -> showOrderDetails(commande));

        Button downloadBtn = new Button("Download PDF");
        downloadBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14;");
        downloadBtn.setOnAction(event -> downloadOrderAsPdf(commande));

        HBox buttonBox = new HBox(10, detailsBtn, downloadBtn);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
        VBox.setMargin(buttonBox, new Insets(10, 0, 10, 0));

        card.getChildren().addAll(imageView, clientLabel, dateLabel, totalLabel, buttonBox);

        return card;
    }

    private void downloadOrderAsPdf(Commande commande) {
        downloadOrder(commande);
    }

    private javafx.scene.image.Image createPlaceholderImage(int width, int height) {
        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();
        javafx.scene.paint.Color color = javafx.scene.paint.Color.LIGHTGRAY;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                writer.setColor(x, y, color);
            }
        }
        return image;
    }

    private void showOrderDetails(Commande commande) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/OrderDetails.fxml"));
            VBox detailsRoot = loader.load();
            OrderDetailsController controller = loader.getController();
            controller.setOrderData(commande);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UTILITY);
            stage.setTitle("Order Details");
            stage.setScene(new javafx.scene.Scene(detailsRoot));
            stage.showAndWait();
        } catch (IOException e) {
            System.err.println("Error loading order details: " + e.getMessage());
            showAlert("Error", "Could not load order details", e.getMessage());
        }
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleSearch() {
        searchDate = datePicker.getValue();
        currentPage = 1;
        loadOrders();
    }

    @FXML
    private void handleClearSearch() {
        datePicker.setValue(null);
        searchDate = null;
        currentPage = 1;
        loadOrders();
    }

    @FXML
    private void handlePreviousPage() {
        if (currentPage > 1) {
            currentPage--;
            loadOrders();
        }
    }

    @FXML
    private void handleNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            loadOrders();
        }
    }

    private void updatePaginationControls() {
        pageInfoLabel.setText("Page " + currentPage + " of " + totalPages);
        prevPageBtn.setDisable(currentPage <= 1);
        nextPageBtn.setDisable(currentPage >= totalPages);
    }


    private byte[] generateOrderPdfContent(Commande commande) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ArticleService articleService = new ArticleService();

        try (PdfWriter writer = new PdfWriter(outputStream);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            // General style
            document.setMargins(50, 50, 50, 50);

            // 1. Header with logo and company info
            float[] headerWidths = {3, 1};
            Table headerTable = new Table(headerWidths);

            // Company info on left
            Paragraph companyInfo = new Paragraph()
                    .add(new Text("Autol.Ink\n").setBold().setFontSize(14))
                    .add("123 Rue de l'Innovation\n")
                    .add("Tunis, Tunisie\n")
                    .add("Tél: +216 48 004 881\n")
                    .add("Email: contact@autol.ink.com\n")
                    .add("Site: www.autol.ink.com");

            headerTable.addCell(new Cell().add(companyInfo).setBorder(Border.NO_BORDER));

            // Logo on right
            try {
                URL logoUrl = getClass().getResource("/images/logo.jpg");
                if (logoUrl != null) {
                    ImageData logoData = ImageDataFactory.create(logoUrl.toURI().toString());
                    com.itextpdf.layout.element.Image pdfImage = new com.itextpdf.layout.element.Image(logoData)
                            .setWidth(80)
                            .setHeight(80);
                    headerTable.addCell(new Cell().add(pdfImage).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));
                }
            } catch (Exception e) {
                System.err.println("Logo error: " + e.getMessage());
                headerTable.addCell(new Cell().add(new Paragraph("")).setBorder(Border.NO_BORDER));
            }

            document.add(headerTable);
            document.add(new Paragraph().setMarginBottom(20));

            // 2. Centered Order title with separator line
            document.add(new Paragraph("COMMANDE")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(5));

            // Separator line after title
            document.add(new LineSeparator(new SolidLine()).setMarginBottom(20));

            // 3. Table for client info + QR Code (same line)
            float[] clientInfoWidths = {3, 1}; // 3/4 for client info, 1/4 for QR code
            Table clientInfoTable = new Table(clientInfoWidths);
            clientInfoTable.setWidth(UnitValue.createPercentValue(100));
            clientInfoTable.setMarginBottom(30);

            // Corrected Left cell: Client and order info
            Paragraph clientInfo = new Paragraph()
                    .add(new Text("Date: ").setBold())
                    .add(commande.getDateCommande().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    .add("\n")
                    .add(new Text("N° Commande: ").setBold())
                    .add(String.valueOf(commande.getId()))
                    .add("\n")
                    .add(new Text("Client: ").setBold())
                    .add(commande.getClient().getName() + " " + commande.getClient().getLastName())
                    .add("\n")
                    .add(new Text("Email: ").setBold())
                    .add(commande.getClient().getEmail() != null ? commande.getClient().getEmail() : "Non spécifié")
                    .add("\n\n")
                    .add(new Text("Montant total: ").setBold())
                    .add(String.format("%.2f dt", commande.getTotal()));

            clientInfoTable.addCell(new Cell().add(clientInfo).setBorder(Border.NO_BORDER));

            // Right cell: QR Code
            try {
                String qrContent = generateQRContentForOrder(commande);
                byte[] qrCodeBytes = generateQRCodeImage(qrContent, 150, 150);
                ImageData qrImageData = ImageDataFactory.create(qrCodeBytes);
                com.itextpdf.layout.element.Image qrImage = new com.itextpdf.layout.element.Image(qrImageData)
                        .setWidth(100)
                        .setHeight(100)
                        .setHorizontalAlignment(HorizontalAlignment.RIGHT);

                // Text under QR code
                Paragraph qrText = new Paragraph("Scanner pour vérifier\nla commande")
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(8)
                        .setMarginTop(5);

                // Using Div instead of VBox
                Div qrContainer = new Div();
                qrContainer.add(qrImage);
                qrContainer.add(qrText);
                qrContainer.setTextAlignment(TextAlignment.RIGHT);

                clientInfoTable.addCell(new Cell().add(qrContainer).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));
            } catch (Exception e) {
                System.err.println("QR code generation error: " + e.getMessage());
                clientInfoTable.addCell(new Cell().add(new Paragraph("QR Code non généré")).setBorder(Border.NO_BORDER));
            }

            document.add(clientInfoTable);

            // 4. Articles section
            document.add(new Paragraph("Détails des articles")
                    .setBold()
                    .setMarginBottom(10));

            // 5. Articles table with improved style
            try {
                List<Integer> articleIds = parseArticleIds(commande.getArticleIds());
                Map<Integer, Integer> quantites = parseQuantites(commande.getQuantites());

                if (!articleIds.isEmpty()) {
                    float[] columnWidths = {3, 1, 1, 1};
                    Table articlesTable = new Table(columnWidths);
                    articlesTable.setWidth(UnitValue.createPercentValue(100));
                    articlesTable.setMarginBottom(20);

                    // Colors
                    DeviceRgb headerBgColor = new DeviceRgb(70, 130, 180); // Steel blue
                    DeviceRgb whiteColor = new DeviceRgb(255, 255, 255);
                    DeviceRgb lightBlueColor = new DeviceRgb(240, 248, 255);

                    // Table headers
                    articlesTable.addHeaderCell(
                            new Cell().add(new Paragraph("Article").setBold())
                                    .setBackgroundColor(headerBgColor)
                                    .setFontColor(whiteColor)
                                    .setTextAlignment(TextAlignment.CENTER)
                                    .setPadding(5));

                    articlesTable.addHeaderCell(
                            new Cell().add(new Paragraph("Prix unitaire").setBold())
                                    .setBackgroundColor(headerBgColor)
                                    .setFontColor(whiteColor)
                                    .setTextAlignment(TextAlignment.CENTER)
                                    .setPadding(5));

                    articlesTable.addHeaderCell(
                            new Cell().add(new Paragraph("Quantité").setBold())
                                    .setBackgroundColor(headerBgColor)
                                    .setFontColor(whiteColor)
                                    .setTextAlignment(TextAlignment.CENTER)
                                    .setPadding(5));

                    articlesTable.addHeaderCell(
                            new Cell().add(new Paragraph("Total").setBold())
                                    .setBackgroundColor(headerBgColor)
                                    .setFontColor(whiteColor)
                                    .setTextAlignment(TextAlignment.CENTER)
                                    .setPadding(5));

                    double totalHT = 0;
                    boolean alternate = false;

                    for (Integer articleId : articleIds) {
                        try {
                            Article article = articleService.getArticleById(articleId);
                            if (article != null) {
                                int quantite = quantites.getOrDefault(articleId, 1);
                                double prix = article.getPrix();
                                double totalArticle = prix * quantite;
                                totalHT += totalArticle;

                                DeviceRgb rowColor = alternate ? lightBlueColor : whiteColor;
                                alternate = !alternate;

                                articlesTable.addCell(
                                        new Cell().add(new Paragraph(article.getNom()))
                                                .setBackgroundColor(rowColor)
                                                .setPadding(5)
                                                .setTextAlignment(TextAlignment.LEFT));

                                articlesTable.addCell(
                                        new Cell().add(new Paragraph(String.format("%.2f dt", prix)))
                                                .setBackgroundColor(rowColor)
                                                .setPadding(5)
                                                .setTextAlignment(TextAlignment.CENTER));

                                articlesTable.addCell(
                                        new Cell().add(new Paragraph(String.valueOf(quantite)))
                                                .setBackgroundColor(rowColor)
                                                .setPadding(5)
                                                .setTextAlignment(TextAlignment.CENTER));

                                articlesTable.addCell(
                                        new Cell().add(new Paragraph(String.format("%.2f dt", totalArticle)))
                                                .setBackgroundColor(rowColor)
                                                .setPadding(5)
                                                .setTextAlignment(TextAlignment.CENTER));
                            }
                        } catch (Exception e) {
                            System.err.println("Error with article ID " + articleId + ": " + e.getMessage());
                        }
                    }

                    document.add(articlesTable);

                    // 6. Totals
                    float[] totalWidths = {3, 1};
                    Table totalTable = new Table(totalWidths);
                    totalTable.setWidth(UnitValue.createPercentValue(40));
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
                }
            } catch (Exception e) {
                System.err.println("Error retrieving articles: " + e.getMessage());
                document.add(new Paragraph("Erreur lors de la récupération des données des articles"));
            }

            // 7. Footer
            document.add(new Paragraph("\n\n"));
            document.add(new LineSeparator(new SolidLine()));

            Paragraph footer = new Paragraph()
                    .add(new Text("Merci pour votre commande !").setItalic())
                    .setTextAlignment(TextAlignment.CENTER)
                    .add("\n")
                    .add(new Text("Pour toute question, contactez-nous à contact@autol.ink.com").setFontSize(10))
                    .add("\n")
                    .add(new Text("Tél: +216 48 004 881").setFontSize(10));

            document.add(footer);

        } catch (Exception e) {
            throw new IOException("Erreur lors de la génération du PDF", e);
        }

        return outputStream.toByteArray();
    }

    public boolean downloadOrder(Commande commande) {
        try {
            // 1. Generate PDF
            byte[] pdfContent = generateOrderPdfContent(commande);

            // 2. Save to downloads folder
            String fileName = "commande_" + commande.getId() + "_" +
                    commande.getDateCommande().toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE) + ".pdf";
            Path downloadsPath = Paths.get(System.getProperty("user.home"), "Downloads", fileName);

            // Create file with verification
            if (Files.exists(downloadsPath)) {
                Files.delete(downloadsPath);
            }
            Files.write(downloadsPath, pdfContent);

            // 3. Open file
            if (Desktop.isDesktopSupported()) {
                new Thread(() -> {
                    try {
                        Thread.sleep(1000); // Wait for complete writing
                        Desktop.getDesktop().open(downloadsPath.toFile());
                    } catch (Exception e) {
                        Platform.runLater(() ->
                                showAlert("Erreur", "Impossible d'ouvrir le PDF",
                                        "Le PDF a été généré mais n'a pas pu s'ouvrir automatiquement.\n" +
                                                "Emplacement: " + downloadsPath));
                    }
                }).start();
            }

            // 4. Notification
            Platform.runLater(() -> {
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Téléchargement réussi");
                successAlert.setHeaderText(null);
                successAlert.setContentText("La commande a été sauvegardée dans:\n" + downloadsPath);
                successAlert.showAndWait();
            });

            return true;

        } catch (Exception e) {
            Platform.runLater(() ->
                    showAlert("Erreur", "Erreur technique",
                            "Erreur lors de la génération du PDF:\n" + e.getMessage()));
            e.printStackTrace();
            return false;
        }
    }

    private String generateQRContentForOrder(Commande commande) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== COMMANDE AUTOL.INK ===\n\n");
        sb.append("N° Commande: ").append(commande.getId()).append("\n");
        sb.append("Date: ").append(commande.getDateCommande().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
        sb.append("Client: ").append(commande.getClient().getName()).append(" ").append(commande.getClient().getLastName()).append("\n");
        sb.append("Email: ").append(commande.getClient().getEmail()).append("\n");
        sb.append("Montant total: ").append(String.format("%.2f dt", commande.getTotal())).append("\n\n");

        // Articles details
        try {
            ArticleService articleService = new ArticleService();
            List<Integer> articleIds = parseArticleIds(commande.getArticleIds());
            Map<Integer, Integer> quantites = parseQuantites(commande.getQuantites());

            if (!articleIds.isEmpty()) {
                sb.append("=== ARTICLES ===\n");

                for (Integer articleId : articleIds) {
                    try {
                        Article article = articleService.getArticleById(articleId);
                        if (article != null) {
                            int quantite = quantites.getOrDefault(articleId, 1);
                            sb.append("- ").append(article.getNom())
                                    .append(" (x").append(quantite)
                                    .append(") - ").append(String.format("%.2f dt", article.getPrix()))
                                    .append("\n");
                        }
                    } catch (Exception e) {
                        System.err.println("Error retrieving article " + articleId);
                    }
                }

                sb.append("\nTotal articles: ").append(articleIds.size()).append("\n");
            }
        } catch (Exception e) {
            System.err.println("Error generating QR content for articles: " + e.getMessage());
        }

        sb.append("\nCe QR code est une preuve d'authenticité de votre commande.");
        return sb.toString();
    }

    // Utility methods (same as in Facture controller)
    private Cell createTotalCell(String text, boolean isLabel) {
        Paragraph p = new Paragraph(text);
        if (isLabel) p.setBold();
        return new Cell().add(p).setBorder(Border.NO_BORDER);
    }

    private List<Integer> parseArticleIds(String json) {
        try {
            if (json.startsWith("[") && json.endsWith("]")) {
                return new ObjectMapper().readValue(json, new TypeReference<List<Integer>>() {});
            }
            String cleaned = json.replace("[", "").replace("]", "").trim();
            if (!cleaned.isEmpty()) {
                return Arrays.stream(cleaned.split(","))
                        .map(String::trim)
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            System.err.println("Error parsing article IDs: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    private Map<Integer, Integer> parseQuantites(String json) {
        try {
            if (json.startsWith("{") && json.endsWith("}")) {
                return new ObjectMapper().readValue(json, new TypeReference<Map<Integer, Integer>>() {});
            }
            String cleaned = json.replace("(", "").replace(")", "")
                    .replace("\"", "").trim();
            if (cleaned.contains(":")) {
                Map<Integer, Integer> map = new HashMap<>();
                String[] pairs = cleaned.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split(":");
                    map.put(Integer.parseInt(kv[0].trim()), Integer.parseInt(kv[1].trim()));
                }
                return map;
            }
        } catch (Exception e) {
            System.err.println("Error parsing quantities: " + e.getMessage());
        }
        return new HashMap<>();
    }

    private byte[] generateQRCodeImage(String text, int width, int height) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = new QRCodeWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                width,
                height,
                hints
        );

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        return pngOutputStream.toByteArray();
    }
}