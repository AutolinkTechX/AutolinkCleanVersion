package org.example.pidev.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.example.pidev.entities.Blog;
import org.example.pidev.services.ServiceBlog;

import java.io.File;
import java.io.InputStream;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;

public class AddBlogController {

    @FXML
    private TextField titleField;
    @FXML
    private TextArea contentField;
    @FXML
    private DatePicker dpPublishedDate;
    @FXML
    private TextField imageField;
    @FXML
    private TextField likesField;
    @FXML
    private TextField dislikesField;
    @FXML
    private Button selectImageBtn;
    @FXML
    private ImageView imagePreview;

    private String imagePath;
    private Runnable onAddCallback; // ✅ Manquait cette ligne !

    @FXML
    private void handleSelectImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(selectImageBtn.getScene().getWindow());
        if (selectedFile != null) {
            try {
                String imageUrl = selectedFile.toURI().toString();
                Image image = new Image(imageUrl, 100, 100, true, true);

                image.errorProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal) {
                        showAlert("Erreur", "Impossible de charger l'image sélectionnée", Alert.AlertType.ERROR);
                        loadDefaultImage();
                    }
                });

                if (!image.isError()) {
                    imagePreview.setImage(image);
                    imagePath = imageUrl;
                    imageField.setText(selectedFile.getAbsolutePath());
                }
            } catch (Exception e) {
                showAlert("Erreur", "Le fichier sélectionné n'est pas une image valide", Alert.AlertType.ERROR);
                loadDefaultImage();
            }
        }
    }


    @FXML
    private void handleAddBlog() {
        try {
            // Validation des champs
            if (titleField.getText().isEmpty() || contentField.getText().isEmpty()) {
                showAlert("Erreur", "Le titre et le contenu sont obligatoires", Alert.AlertType.ERROR);
                return;
            }

            LocalDate selectedDate = dpPublishedDate.getValue();
            if (selectedDate == null) {
                selectedDate = LocalDate.now();
            }

            int likes = parseNumber(likesField.getText(), 0);
            int dislikes = parseNumber(dislikesField.getText(), 0);

            Blog newBlog = new Blog(
                    0,
                    titleField.getText(),
                    contentField.getText(),
                    Date.valueOf(selectedDate),
                    imagePath,
                    likes,
                    dislikes
            );

            ServiceBlog service = new ServiceBlog();
            service.ajouter(newBlog);

            showAlert("Succès", "Blog ajouté avec succès", Alert.AlertType.INFORMATION);
            clearFields();

            if (onAddCallback != null) {
                onAddCallback.run(); // ✅ Appel du callback pour rafraîchir dans BlogController
            }

        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors de l'ajout du blog: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private int parseNumber(String text, int defaultValue) {
        try {
            return text.isEmpty() ? defaultValue : Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void loadDefaultImage() {
        try (InputStream is = getClass().getResourceAsStream("/images/logo.jpg")) {
            if (is != null) {
                Image defaultImage = new Image(is);
                imagePreview.setImage(defaultImage);
            } else {
                imagePreview.setImage(null);
            }
            imagePath = null;
            imageField.setText("");
        } catch (Exception e) {
            imagePreview.setImage(null);
            System.err.println("Erreur lors du chargement de l'image par défaut: " + e.getMessage());
        }
    }

    private void clearFields() {
        titleField.clear();
        contentField.clear();
        dpPublishedDate.setValue(null);
        likesField.clear();
        dislikesField.clear();
        loadDefaultImage();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ✅ Setter pour le callback
    public void setOnAddCallback(Runnable onAddCallback) {
        this.onAddCallback = onAddCallback;
    }
}
