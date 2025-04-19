package org.example.pidev.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.pidev.entities.Commande;
import org.example.pidev.services.CommandeService;
import org.example.pidev.utils.MyDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

public class Orders {
    @FXML private FlowPane cardsContainer;
    @FXML private DatePicker datePicker;
    @FXML private Button prevPageBtn;
    @FXML private Button nextPageBtn;
    @FXML private Label pageInfoLabel;

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
                imageView.setImage(new Image(is));
                is.close();
            } else {
                // Fallback to classpath loading
                imageView.setImage(new Image("/images/Facture.png"));
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

        // Part 3: Details button
        Button detailsBtn = new Button("Details");
        detailsBtn.setStyle("-fx-background-color: #ca8a62; -fx-text-fill: white; -fx-font-size: 14;");
        detailsBtn.setOnAction(event -> showOrderDetails(commande));

        VBox.setMargin(detailsBtn, new Insets(10, 0, 10, 0));
        HBox buttonBox = new HBox(detailsBtn);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);

        card.getChildren().addAll(imageView, clientLabel, dateLabel, totalLabel, buttonBox);

        return card;
    }

    private Image createPlaceholderImage(int width, int height) {
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
}