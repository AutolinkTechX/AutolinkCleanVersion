package org.example.pidev.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.example.pidev.entities.Article;

import java.io.InputStream;

public class ArticleDetailsController {
    @FXML private Label nameLabel;
    @FXML private Label quantityLabel;
    @FXML private Label priceLabel;
    @FXML private Label categoryLabel;
    @FXML private Label dateLabel;
    @FXML private TextArea descriptionLabel;
    @FXML private ImageView imagePreview;
    @FXML private ImageView headerImageView;

    private Article article;

    public void setArticle(Article article) {
        this.article = article;
        loadArticleData();
    }

    public void initialize() {
        loadHeaderImage();
    }

    private void loadArticleData() {
        if (article != null) {
            nameLabel.setText(article.getNom() != null ? article.getNom() : "Non spécifié");
            categoryLabel.setText(article.getCategory() != null ? article.getCategory() : "Non spécifié");
            descriptionLabel.setText(article.getDescription() != null ? article.getDescription() : "Aucune description");

            quantityLabel.setText(String.valueOf(article.getQuantitestock()));
            priceLabel.setText(String.format("%.2f DT", article.getPrix()));

            if (article.getDatecreation() != null) {
                dateLabel.setText(article.getDatecreation().toLocalDate().toString());
            } else {
                dateLabel.setText("Non spécifiée");
            }

            loadArticleImage();
        }
    }

    public void setArticleData(String name, int quantity, double price,
                               String category, String date, String description) {
        nameLabel.setText(name);
        quantityLabel.setText(String.valueOf(quantity));
        priceLabel.setText(String.format("%.2f DT", price));
        categoryLabel.setText(category);
        dateLabel.setText(date);
        descriptionLabel.setText(description);
    }

    private void loadArticleImage() {
        if (article.getImage() != null && !article.getImage().trim().isEmpty()) {
            try {
                Image image = new Image(article.getImage());
                if (!image.isError()) {
                    imagePreview.setImage(image);
                    return;
                }
            } catch (Exception e) {
                System.err.println("Error loading article image: " + e.getMessage());
            }
        }
        setDefaultImage();
    }

    private void setDefaultImage() {
        try {
            InputStream is = getClass().getResourceAsStream("/images/logo.jpg");
            if (is != null) {
                imagePreview.setImage(new Image(is));
            } else {
                imagePreview.setImage(null);
            }
        } catch (Exception e) {
            System.err.println("Error loading default image: " + e.getMessage());
            imagePreview.setImage(null);
        }
    }

    private void loadHeaderImage() {
        try {
            InputStream is = getClass().getResourceAsStream("/images/logo.jpg");
            if (is != null) {
                Image image = new Image(is);
                headerImageView.setImage(image);
            }
        } catch (Exception e) {
            System.err.println("Error loading header image: " + e.getMessage());
        }
    }

    @FXML
    private void handleReturn() {
        Stage stage = (Stage) nameLabel.getScene().getWindow();
        stage.close();
    }


}