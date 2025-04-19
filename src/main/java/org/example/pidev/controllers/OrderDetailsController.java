package org.example.pidev.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.pidev.entities.Commande;
import org.example.pidev.services.ArticleService;

import java.util.Map;

public class OrderDetailsController {

    @FXML private Label orderIdLabel;
    @FXML private Label clientNameLabel;
    @FXML private Label dateLabel;
    @FXML private Label paymentLabel;
    @FXML private VBox articlesContainer;
    @FXML private Label totalLabel;

    private final ArticleService articleService = new ArticleService();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public void setOrderData(Commande commande) {
        orderIdLabel.setText("Order ID: " + commande.getId());
        clientNameLabel.setText("Client: " + commande.getClient().getName() + " " + commande.getClient().getLastName());
        dateLabel.setText("Date: " + commande.getDateCommande().toString());
        paymentLabel.setText("Payment: " + commande.getModePaiement());
        totalLabel.setText("Total: " + commande.getTotal() + " DT");

        try {
            // Parse the JSON strings to get article IDs and quantities
            Map<Integer, Integer> articleQuantities = objectMapper.readValue(
                    commande.getQuantites(),
                    new TypeReference<Map<Integer, Integer>>() {}
            );

            // Display each article with its quantity
            for (Map.Entry<Integer, Integer> entry : articleQuantities.entrySet()) {
                int articleId = entry.getKey();
                int quantity = entry.getValue();

                String articleName = articleService.getArticleNameById(articleId);
                Label articleLabel = new Label(String.format(
                        "• %s (Quantity: %d)",
                        articleName,
                        quantity
                ));
                articleLabel.setStyle("-fx-font-size: 14;");
                articlesContainer.getChildren().add(articleLabel);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Label errorLabel = new Label("Error loading article details");
            articlesContainer.getChildren().add(errorLabel);
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) orderIdLabel.getScene().getWindow();
        stage.close();
    }
}