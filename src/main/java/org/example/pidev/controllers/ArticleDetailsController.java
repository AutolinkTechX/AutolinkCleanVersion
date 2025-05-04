package org.example.pidev.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.example.pidev.entities.Article;
import org.example.pidev.utils.TranslationService;

import java.io.InputStream;

public class ArticleDetailsController {
    @FXML private Label nameLabel;
    @FXML private Label quantityLabel;
    @FXML private Label priceLabel;
    @FXML private Label categoryLabel;
    @FXML private Label dateLabel;
    @FXML private TextArea descriptionLabel;
    @FXML private ImageView imagePreview;
    @FXML private Button translateButton;
    @FXML private ImageView headerImageView;
    private Article article;
    private boolean isTranslated = false;

    public void setArticle(Article article) {
        this.article = article;
        loadArticleData();
    }

    public void initialize() {
        loadHeaderImage();
    }

    private void loadArticleData() {
        if (article != null) {
            updateDisplay(false); // Afficher en français par défaut
        }
    }

    /*
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

    @FXML
    private void handleReturn() {
        Stage stage = (Stage) nameLabel.getScene().getWindow();
        stage.close();
    }

*/

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
    private void updateDisplay(boolean translateToEnglish) {
        // Nom
        nameLabel.setText(translateToEnglish ?
                TranslationService.translate(article.getNom(), "en") :
                article.getNom());

        // Catégorie
        categoryLabel.setText(translateToEnglish ?
                TranslationService.translate(article.getCategory(), "en") :
                article.getCategory());

        // Description
        descriptionLabel.setText(translateToEnglish ?
                TranslationService.translate(article.getDescription(), "en") :
                article.getDescription());

        // Quantité (pas de traduction nécessaire pour les nombres)
        quantityLabel.setText(String.valueOf(article.getQuantitestock()));

        // Prix (format différent en anglais si nécessaire)
        priceLabel.setText(String.format("%.2f %s",
                article.getPrix(),
                translateToEnglish ? "DT" : "DT"));

        // Date (pas de traduction nécessaire pour le format)
        if (article.getDatecreation() != null) {
            dateLabel.setText(article.getDatecreation().toLocalDate().toString());
        } else {
            dateLabel.setText(translateToEnglish ? "Not specified" : "Non spécifiée");
        }

        // Mettre à jour le texte du bouton
        translateButton.setText(translateToEnglish ?
                TranslationService.translate("Voir original", "fr") :
                TranslationService.translate("Traduire en anglais", "fr"));

        // Charger l'image (inchangé)
        loadArticleImage();
    }

    @FXML
    private void handleTranslate() {
        isTranslated = !isTranslated;
        updateDisplay(isTranslated);
    }

    @FXML
    private void handleReturn() {
        Stage stage = (Stage) nameLabel.getScene().getWindow();
        stage.close();
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
}