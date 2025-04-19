package org.example.pidev.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import org.example.pidev.entities.Article;
import org.example.pidev.services.ArticleService;
import org.example.pidev.services.CommandeService;
import org.example.pidev.utils.MyDatabase;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Statistique implements Initializable {

    @FXML
    private BarChart<String, Number> productSalesChart;
    @FXML
    private PieChart paymentPieChart;
    @FXML
    private BarChart<String, Number> outOfStockChart;
    @FXML
    private Label hoverLabel;

    private final ArticleService articleService = new ArticleService();
    private final CommandeService commandeService = new CommandeService(MyDatabase.getInstance().getConnection());
    private static final Logger logger = Logger.getLogger(Statistique.class.getName());

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            loadProductSalesStatistics();
            loadPaymentStatistics();
            loadOutOfStockStatistics();
            setupHoverListeners();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Error loading statistics: " + ex.getMessage(), ex);
            hoverLabel.setText("Error loading statistics. Please try again.");
        }
    }

    private void loadProductSalesStatistics() throws SQLException {
        Map<String, Integer> productSales = articleService.getProductSales();
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        // Ajouter les données des produits
        for (Map.Entry<String, Integer> entry : productSales.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        productSalesChart.getData().add(series);

        // Style les barres
        for (XYChart.Data<String, Number> data : series.getData()) {
            data.getNode().setStyle("-fx-bar-fill: #3498db;");
        }
    }

    private void loadPaymentStatistics() throws SQLException {
        Map<String, Long> paymentStats = commandeService.getPaymentMethodStatistics();

        ObservableList<PieChart.Data> paymentData = FXCollections.observableArrayList();
        for (Map.Entry<String, Long> entry : paymentStats.entrySet()) {
            paymentData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }

        paymentPieChart.setData(paymentData);
    }

    private void loadOutOfStockStatistics() throws SQLException {
        List<Article> outOfStockArticles = articleService.getOutOfStockArticles();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Article article : outOfStockArticles) {
            series.getData().add(new XYChart.Data<>(article.getNom(), article.getQuantitestock()));
        }

        // Ajouter l'exemple de votre capture si nécessaire
       /* if (outOfStockArticles.isEmpty()) {
            series.getData().add(new XYChart.Data<>("Sonde Lambda", 0));
        }*/

        outOfStockChart.getData().add(series);

        // Style les barres en rouge pour rupture de stock
        for (XYChart.Data<String, Number> data : series.getData()) {
            data.getNode().setStyle("-fx-bar-fill: #e74c3c;");
        }
    }

    private void setupHoverListeners() {
        // Pour le graphique des ventes par produit
        for (XYChart.Series<String, Number> series : productSalesChart.getData()) {
            for (XYChart.Data<String, Number> data : series.getData()) {
                data.getNode().setOnMouseEntered(event -> {
                    hoverLabel.setText(String.format("%s: %d ventes",
                            data.getXValue(),
                            data.getYValue().intValue()));
                });
            }
        }

        // Pour le graphique des modes de paiement
        for (PieChart.Data data : paymentPieChart.getData()) {
            data.getNode().setOnMouseEntered(event -> {
                hoverLabel.setText(String.format("%s: %.1f%%",
                        data.getName(),
                        (data.getPieValue() / getTotal(paymentPieChart)) * 100));
            });
        }

        // Pour le graphique des ruptures de stock
        for (XYChart.Series<String, Number> series : outOfStockChart.getData()) {
            for (XYChart.Data<String, Number> data : series.getData()) {
                data.getNode().setOnMouseEntered(event -> {
                    hoverLabel.setText(String.format("%s: rupture de stock", data.getXValue()));
                });
            }
        }

        // Réinitialiser le label quand la souris quitte
        productSalesChart.setOnMouseExited(event -> hoverLabel.setText(""));
        paymentPieChart.setOnMouseExited(event -> hoverLabel.setText(""));
        outOfStockChart.setOnMouseExited(event -> hoverLabel.setText(""));
    }

    private double getTotal(PieChart chart) {
        double total = 0;
        for (PieChart.Data data : chart.getData()) {
            total += data.getPieValue();
        }
        return total;
    }
}