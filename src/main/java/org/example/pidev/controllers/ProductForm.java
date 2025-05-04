package org.example.pidev.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.pidev.entities.Article;
import org.example.pidev.services.AIGenerationService;
import org.example.pidev.services.ApiKeyManager;
import org.example.pidev.services.ArticleService;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.concurrent.TimeoutException;

public class ProductForm {

    @FXML private TextField nameField;
    @FXML private TextField quantityField;
    @FXML private TextField priceField;
    @FXML private TextField categoryField;
    @FXML private TextArea descriptionField;
    @FXML private DatePicker datePicker;
    @FXML private ImageView imagePreview;
    @FXML private Button selectImageBtn;

    @FXML private Label nameError;
    @FXML private Label quantityError;
    @FXML private Label priceError;
    @FXML private Label categoryError;
    @FXML private Label dateError;
    @FXML private Label descriptionError;
    @FXML private Label imageError;
    // Ajoutez ces déclarations de champs
    @FXML private Button generateAIButton;
    @FXML private ProgressIndicator aiProgress;
    //private final AIGenerationService aiService = new AIGenerationService();

    private final ArticleService articleService = new ArticleService();
    private Article article;
    private AjoutArticleController parentController;
    private String imagePath;
    @FXML private Label apiKeyErrorLabel;
    private final ApiKeyManager apiKeyManager = new ApiKeyManager();


    @FXML
    public void initialize() {
        configureFields();
        clearErrorMessages();
        setupFieldValidators();
        checkApiKeyStatus();
    }

    private void checkApiKeyStatus() {
        boolean isApiValid = apiKeyManager.isApiKeyValid();
        apiKeyErrorLabel.setVisible(!isApiValid);

        // Désactive les fonctionnalités IA si la clé est invalide
        if (generateAIButton != null) {
            generateAIButton.setDisable(!isApiValid);
        }
    }

    private void configureFields() {
        quantityField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                quantityField.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });

        priceField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                priceField.setText(oldVal);
            }
        });
    }

    private void setupFieldValidators() {
        nameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validateNameField();
        });

        quantityField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validateQuantityField();
        });

        priceField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validatePriceField();
        });

        categoryField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validateCategoryField();
        });

        datePicker.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validateDateField();
        });

        descriptionField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validateDescriptionField();
        });

        selectImageBtn.setOnAction(event -> {
            handleSelectImage();
            validateImageField();
        });
    }

    private void validateNameField() {
        if (nameField.getText().trim().isEmpty()) {
            showFieldError(nameField, nameError, "Veuillez saisir un nom de produit !");
        } else {
            clearFieldError(nameField, nameError);
        }
    }

    private void validateQuantityField() {
        String text = quantityField.getText().trim();
        if (text.isEmpty()) {
            showFieldError(quantityField, quantityError, "Veuillez saisir une quantité !");
        } else if (!isNonNegativeInteger(text)) {
            showFieldError(quantityField, quantityError, "La quantité doit être un nombre entier positif !");
        } else {
            clearFieldError(quantityField, quantityError);
        }
    }

    private void validatePriceField() {
        String text = priceField.getText().trim();
        if (text.isEmpty()) {
            showFieldError(priceField, priceError, "Veuillez saisir un prix !");
        } else if (!isPositiveDecimal(text)) {
            showFieldError(priceField, priceError, "Le prix doit être un nombre positif !");
        } else {
            clearFieldError(priceField, priceError);
        }
    }

    private void validateCategoryField() {
        if (categoryField.getText().trim().isEmpty()) {
            showFieldError(categoryField, categoryError, "Veuillez saisir une catégorie !");
        } else {
            clearFieldError(categoryField, categoryError);
        }
    }

    private void validateDateField() {
        if (datePicker.getValue() == null) {
            showFieldError(datePicker, dateError, "Veuillez sélectionner une date !");
        } else if (datePicker.getValue().isAfter(LocalDate.now())) {
            showFieldError(datePicker, dateError, "La date ne peut pas être dans le futur !");
        } else {
            clearFieldError(datePicker, dateError);
        }
    }

    private void validateDescriptionField() {
        if (descriptionField.getText().trim().isEmpty()) {
            showFieldError(descriptionField, descriptionError, "Veuillez saisir une description !");
        } else {
            clearFieldError(descriptionField, descriptionError);
        }
    }

    private void validateImageField() {
        if (imagePath == null || imagePath.isEmpty()) {
            showFieldError(selectImageBtn, imageError, "Veuillez sélectionner une image !");
        } else {
            clearFieldError(selectImageBtn, imageError);
        }
    }

    private void showFieldError(Node field, Label errorLabel, String message) {
        field.getStyleClass().add("error-field");
        errorLabel.setText("! " + message);
    }

    private void clearFieldError(Node field, Label errorLabel) {
        field.getStyleClass().remove("error-field");
        errorLabel.setText("");
    }

    @FXML
    private void handleSelectImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.web"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(selectImageBtn.getScene().getWindow());
        if (selectedFile != null) {
            try {
                String imageUrl = selectedFile.toURI().toString();
                Image image = new Image(imageUrl);
                if (!image.isError()) {
                    imagePreview.setImage(image);
                    imagePath = imageUrl;
                } else {
                    throw new RuntimeException("Invalid image file");
                }
            } catch (Exception e) {
                showAlert("Erreur", "Le fichier sélectionné n'est pas une image valide", Alert.AlertType.ERROR);
            }
        }
    }

    public void setParentController(AjoutArticleController controller) {
        this.parentController = controller;
    }

    public void setArticle(Article article) {
        this.article = article;
        resetForm();
        if (article != null) {
            loadArticleData(article);
        }
    }

    private void loadArticleData(Article article) {
        nameField.setText(article.getNom() != null ? article.getNom() : "");
        categoryField.setText(article.getCategory() != null ? article.getCategory() : "");
        descriptionField.setText(article.getDescription() != null ? article.getDescription() : "");

        // Toujours afficher la quantité même si 0
        quantityField.setText(String.valueOf(article.getQuantitestock()));

        // Afficher le prix sans formatage spécifique
        priceField.setText(String.valueOf(article.getPrix()));

        if (article.getDatecreation() != null) {
            datePicker.setValue(article.getDatecreation().toLocalDate());
        } else {
            datePicker.setValue(null);
        }

        loadArticleImage(article);
    }

    private void loadArticleImage(Article article) {
        if (article.getImage() != null && !article.getImage().trim().isEmpty()) {
            try {
                Image image = new Image(article.getImage());
                if (!image.isError()) {
                    imagePreview.setImage(image);
                    imagePath = article.getImage();
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
    private void handleSubmit() {
        if (!validateForm()) {
            focusFirstErrorField();
            return;
        }

        try {
            prepareArticle();

            if (article.getId() == null) {
                articleService.create(article);
                showSuccess("Produit ajouté avec succès!");
            } else {
                articleService.update(article);
                showSuccess("Produit modifié avec succès!");
            }

            closeForm();
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de la sauvegarde: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void prepareArticle() {
        if (article == null) {
            article = new Article();
        }

        article.setNom(nameField.getText().trim());
        article.setQuantitestock(Integer.parseInt(quantityField.getText()));
        article.setPrix(Double.parseDouble(priceField.getText()));
        article.setCategory(categoryField.getText().trim());
        article.setDescription(descriptionField.getText().trim());
        article.setDatecreation(datePicker.getValue().atStartOfDay());
        article.setImage(imagePath != null ? imagePath : "");
    }

    private boolean validateForm() {
        validateNameField();
        validateQuantityField();
        validatePriceField();
        validateCategoryField();
        validateDateField();
        validateDescriptionField();
        validateImageField();

        return nameError.getText().isEmpty() &&
                quantityError.getText().isEmpty() &&
                priceError.getText().isEmpty() &&
                categoryError.getText().isEmpty() &&
                dateError.getText().isEmpty() &&
                descriptionError.getText().isEmpty() &&
                imageError.getText().isEmpty();
    }

    private void focusFirstErrorField() {
        if (!nameError.getText().isEmpty()) nameField.requestFocus();
        else if (!quantityError.getText().isEmpty()) quantityField.requestFocus();
        else if (!priceError.getText().isEmpty()) priceField.requestFocus();
        else if (!categoryError.getText().isEmpty()) categoryField.requestFocus();
        else if (!dateError.getText().isEmpty()) datePicker.requestFocus();
        else if (!descriptionError.getText().isEmpty()) descriptionField.requestFocus();
        else if (!imageError.getText().isEmpty()) selectImageBtn.requestFocus();
    }

    private void clearErrorMessages() {
        nameError.setText("");
        quantityError.setText("");
        priceError.setText("");
        categoryError.setText("");
        dateError.setText("");
        descriptionError.setText("");
        imageError.setText("");
    }

    private boolean isNonNegativeInteger(String str) {
        try {
            int value = Integer.parseInt(str);
            return value >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isPositiveDecimal(String str) {
        try {
            double value = Double.parseDouble(str);
            return value > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void resetForm() {
        nameField.clear();
        quantityField.clear();
        priceField.clear();
        categoryField.clear();
        descriptionField.clear();
        datePicker.setValue(null);
        setDefaultImage();
        imagePath = null;
        clearErrorMessages();
        clearFieldErrors();
    }

    private void clearFieldErrors() {
        nameField.getStyleClass().remove("error-field");
        quantityField.getStyleClass().remove("error-field");
        priceField.getStyleClass().remove("error-field");
        categoryField.getStyleClass().remove("error-field");
        datePicker.getStyleClass().remove("error-field");
        descriptionField.getStyleClass().remove("error-field");
        selectImageBtn.getStyleClass().remove("error-field");
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        if (parentController != null) {
            parentController.showSuccessMessage(message);
        }
    }

    @FXML
    private void handleCancel() {
        closeForm();
    }

    private void closeForm() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }

    /*

    // Dans la classe ProductForm

    private final ApiKeyManager apiKeyManager = new ApiKeyManager();
    private final AIGenerationService aiService = new AIGenerationService(apiKeyManager);

    @FXML
    private void handleGenerateAI() {
        String productName = nameField.getText().trim();

        if (productName.isEmpty()) {
            showAlert("Erreur", "Veuillez d'abord saisir un nom de produit", Alert.AlertType.WARNING);
            return;
        }

        aiProgress.setVisible(true);
        generateAIButton.setDisable(true);

        // Timeout après 30 secondes
        Timeline timeout = new Timeline(new KeyFrame(Duration.seconds(30), e -> {
            handleAIError(new TimeoutException("La génération a pris trop de temps"));
        }));
        timeout.play();

        aiService.generateDescriptionAsync(productName)
                .whenComplete((description, error) -> {
                    Platform.runLater(() -> {
                        timeout.stop();
                        aiProgress.setVisible(false);
                        generateAIButton.setDisable(false);

                        if (error != null) {
                            handleAIError(error);
                            descriptionField.setText(generateFallbackDescription(productName));
                        } else {
                            descriptionField.setText(description);
                        }
                    });
                });
    }

    private void handleAIError(Throwable error) {
        String errorMessage = "Erreur lors de la génération IA:\n";

        if (error.getCause() instanceof IllegalStateException) {
            errorMessage += "Problème de configuration API:\n";
            errorMessage += "Veuillez vérifier votre clé API dans les paramètres.";
        } else {
            errorMessage += error.getMessage();
        }

        showAlert("Erreur IA", errorMessage, Alert.AlertType.ERROR);
    }
*/
    // Dans la classe ProductForm

    //private final ApiKeyManager apiKeyManager = new ApiKeyManager();
    //private final AIGenerationService aiService = new AIGenerationService(apiKeyManager);

    // Dans la classe ProductForm
    private final AIGenerationService aiService = new AIGenerationService(apiKeyManager);

    @FXML
    private void handleGenerateAI() {
        String productName = nameField.getText().trim();

        if (productName.isEmpty()) {
            showAlert("Erreur", "Veuillez d'abord saisir un nom de produit", Alert.AlertType.WARNING);
            nameField.requestFocus();
            return;
        }

        if (!apiKeyManager.isApiKeyValid()) {
            showAlert("Configuration requise",
                    "Veuillez d'abord configurer une clé API valide dans les paramètres",
                    Alert.AlertType.ERROR);
            return;
        }

        aiProgress.setVisible(true);
        generateAIButton.setDisable(true);

        // Utilisation d'un thread séparé pour éviter de bloquer l'UI
        new Thread(() -> {
            try {
                String description = aiService.generateSimpleDescription(productName);

                Platform.runLater(() -> {
                    aiProgress.setVisible(false);
                    generateAIButton.setDisable(false);
                    descriptionField.setText(description);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    aiProgress.setVisible(false);
                    generateAIButton.setDisable(false);
                    handleAIError(e);
                    descriptionField.setText(generateFallbackDescription(productName));
                });
            }
        }).start();
    }

    private void handleAIError(Throwable error) {
        String errorMessage = "Erreur lors de la génération IA:\n" + error.getMessage();
        showAlert("Erreur IA", errorMessage, Alert.AlertType.ERROR);
    }

    private String generateFallbackDescription(String productName) {
        return String.format(
                "Produit: %s\n\nDescription générée localement (l'IA n'a pas pu être contactée).",
                productName
        );
    }


}