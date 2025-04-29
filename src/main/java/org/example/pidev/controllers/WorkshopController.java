package org.example.pidev.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.pidev.entities.User;
import org.example.pidev.entities.Workshop;
import org.example.pidev.services.ServiceWorkshop;


import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.example.pidev.utils.SessionManager;
public class WorkshopController {

    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;
    @FXML private DatePicker startDatePicker;
    @FXML private TextField startTimeField;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField endTimeField;
    @FXML private TextField locationField;
    @FXML private Button selectLocationButton;
    @FXML private TextField imageField;
    @FXML private TextField priceField;
    @FXML private TextField availablePlacesField;
    @FXML private Button browseImageButton;
    @FXML private ImageView imagePreview;
    @FXML private Button addButton;

    private File selectedImageFile;
    private Workshop workshopToEdit;
    private User currentUser; // This will now be set from SessionManager
    private ServiceWorkshop workshopService = new ServiceWorkshop();
    private static final String UPLOADS_DIR = "/Uploads";

    @FXML
    private void initialize() {
        // Fetch the current user from SessionManager
        currentUser = SessionManager.getCurrentUser(); // Assuming SessionManager has a method to get the current user
        if (currentUser == null || currentUser.getId() <= 0) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Utilisateur non connecté",
                    "Aucun utilisateur n'est connecté. Veuillez vous connecter avant d'ajouter un workshop.");
            disableForm();
        }

        // Initialisation des écouteurs pour la validation en temps réel
        setupFieldValidators();

        addButton.setOnAction(e -> {
            if (workshopToEdit == null) {
                handleAddWorkshop();
            } else {
                handleUpdateWorkshop();
            }
        });
    }

    private void disableForm() {
        nameField.setDisable(true);
        descriptionField.setDisable(true);
        startDatePicker.setDisable(true);
        startTimeField.setDisable(true);
        endDatePicker.setDisable(true);
        endTimeField.setDisable(true);
        locationField.setDisable(true);
        selectLocationButton.setDisable(true);
        imageField.setDisable(true);
        browseImageButton.setDisable(true);
        priceField.setDisable(true);
        availablePlacesField.setDisable(true);
        addButton.setDisable(true);
    }

    private void setupFieldValidators() {
        // Validation du nom
        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                nameField.setStyle("-fx-border-color: red;");
            } else {
                nameField.setStyle("");
            }
        });

        // Validation de la description
        descriptionField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                descriptionField.setStyle("-fx-border-color: red;");
            } else {
                descriptionField.setStyle("");
            }
        });

        // Validation des dates et heures
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> validateDates());
        startTimeField.textProperty().addListener((obs, oldVal, newVal) -> validateDates());
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> validateDates());
        endTimeField.textProperty().addListener((obs, oldVal, newVal) -> validateDates());

        // Validation du lieu
        locationField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                locationField.setStyle("-fx-border-color: red;");
            } else {
                locationField.setStyle("");
            }
        });

        // Validation du prix
        priceField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                if (newVal != null && !newVal.isEmpty()) {
                    Double.parseDouble(newVal);
                    priceField.setStyle("");
                }
            } catch (NumberFormatException e) {
                priceField.setStyle("-fx-border-color: red;");
            }
        });

        // Validation des places disponibles
        availablePlacesField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                if (newVal != null && !newVal.isEmpty()) {
                    int places = Integer.parseInt(newVal);
                    if (places < 0) {
                        availablePlacesField.setStyle("-fx-border-color: red;");
                    } else {
                        availablePlacesField.setStyle("");
                    }
                }
            } catch (NumberFormatException e) {
                availablePlacesField.setStyle("-fx-border-color: red;");
            }
        });
    }

    private void validateDates() {
        try {
            Timestamp start = parseDateTime(startDatePicker.getValue(), startTimeField.getText());
            Timestamp end = parseDateTime(endDatePicker.getValue(), endTimeField.getText());

            if (start != null && end != null) {
                if (end.before(start) || end.equals(start)) {
                    startDatePicker.setStyle("-fx-border-color: red;");
                    endDatePicker.setStyle("-fx-border-color: red;");
                    startTimeField.setStyle("-fx-border-color: red;");
                    endTimeField.setStyle("-fx-border-color: red;");
                } else {
                    startDatePicker.setStyle("");
                    endDatePicker.setStyle("");
                    startTimeField.setStyle("");
                    endTimeField.setStyle("");
                }
            }
        } catch (Exception e) {
            // En cas d'erreur de parsing, on laisse les bordures rouges
        }
    }

    @FXML
    private void handleSelectLocation() {
        try {
            // Create a new dialog for the map
            Stage mapStage = new Stage();
            mapStage.initModality(Modality.APPLICATION_MODAL);
            mapStage.setTitle("Select Location");

            // Load MapDialog.fxml using FXMLLoader
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_workshop/MapDialog.fxml"));
            Parent root = loader.load();

            // Get the MapDialogController and initialize it
            MapDialogController controller = loader.getController();
            controller.initializeController(locationField, mapStage);

            // Set up the scene
            Scene dialogScene = new Scene(root, 800, 600);
            mapStage.setScene(dialogScene);
            mapStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load map dialog", e.getMessage());
        }
    }

    @FXML
    private void handleAddWorkshop() {
        if (isInputValid()) {
            try {
                Workshop workshop = new Workshop();
                workshop.setName(nameField.getText().trim());
                workshop.setDescription(descriptionField.getText().trim());
                workshop.setLocation(locationField.getText().trim());

                // Parse and set start date/time
                Timestamp startTimestamp = parseDateTime(startDatePicker.getValue(), startTimeField.getText());
                workshop.setStartsAt(startTimestamp);

                // Parse and set end date/time
                Timestamp endTimestamp = parseDateTime(endDatePicker.getValue(), endTimeField.getText());
                workshop.setEndsAt(endTimestamp);

                // Gestion de l'image
                if (selectedImageFile != null) {
                    File uploadsDir = new File(UPLOADS_DIR);
                    if (!uploadsDir.exists()) {
                        if (!uploadsDir.mkdirs()) {
                            throw new IOException("Failed to create Uploads directory: " + UPLOADS_DIR);
                        }
                    }

                    String imageName = System.currentTimeMillis() + "_" + selectedImageFile.getName();
                    File destFile = new File(UPLOADS_DIR, imageName);
                    Files.copy(selectedImageFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    workshop.setImage(destFile.getPath());
                }

                workshop.setPrice(Double.parseDouble(priceField.getText()));
                workshop.setAvailablePlaces(Integer.parseInt(availablePlacesField.getText()));

                // Set user_id from current user
                if (currentUser != null && currentUser.getId() > 0) {
                    workshop.setUser_id(currentUser.getId());
                } else {
                    throw new IllegalStateException("No logged-in user found in session");
                }

                workshopService.addWorkshop(workshop);

                // Show success alert
                showAlert(Alert.AlertType.INFORMATION, "Succès", null, "Workshop ajouté avec succès!");

                // Ask to add to Google Calendar
                Alert calendarPrompt = new Alert(Alert.AlertType.CONFIRMATION);
                calendarPrompt.setTitle("Ajouter au Google Calendar");
                calendarPrompt.setHeaderText("Voulez-vous ajouter cet événement à Google Calendar ?");
                calendarPrompt.setContentText("Cliquer sur 'Oui' ouvrira Google Calendar dans votre navigateur avec les détails pré-remplis.");
                calendarPrompt.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

                calendarPrompt.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.YES) {
                        try {
                            // Generate Google Calendar URL
                            String calendarUrl = generateGoogleCalendarUrl(workshop);
                            // Open in default browser
                            Desktop.getDesktop().browse(new URI(calendarUrl));
                        } catch (Exception e) {
                            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir Google Calendar", e.getMessage());
                        }
                    }
                });

                clearFields();

            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'ajout du workshop", e.getMessage());
            }
        }
    }

    private String generateGoogleCalendarUrl(Workshop workshop) {
        String baseUrl = "https://calendar.google.com/calendar/render?action=TEMPLATE";

        // Encode event details
        String title = URLEncoder.encode(workshop.getName(), java.nio.charset.StandardCharsets.UTF_8);
        String description = URLEncoder.encode(workshop.getDescription(), java.nio.charset.StandardCharsets.UTF_8);
        String location = URLEncoder.encode(workshop.getLocation(), java.nio.charset.StandardCharsets.UTF_8);

        // Format dates to YYYYMMDDTHHMMSSZ (UTC)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
        ZonedDateTime startUtc = workshop.getStartsAt().toLocalDateTime()
                .atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("UTC"));
        ZonedDateTime endUtc = workshop.getEndsAt().toLocalDateTime()
                .atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("UTC"));
        String dates = startUtc.format(formatter) + "/" + endUtc.format(formatter);

        // Build URL
        return String.format("%s&text=%s&details=%s&location=%s&dates=%s",
                baseUrl, title, description, location, dates);
    }

    @FXML
    private void handleUpdateWorkshop() {
        if (workshopToEdit == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Aucun workshop sélectionné",
                    "Veuillez sélectionner un workshop à modifier depuis la liste.");
            return;
        }

        if (isInputValid()) {
            try {
                workshopToEdit.setName(nameField.getText().trim());
                workshopToEdit.setDescription(descriptionField.getText().trim());
                workshopToEdit.setLocation(locationField.getText().trim());
                workshopToEdit.setStartsAt(parseDateTime(startDatePicker.getValue(), startTimeField.getText()));
                workshopToEdit.setEndsAt(parseDateTime(endDatePicker.getValue(), endTimeField.getText()));

                if (selectedImageFile != null) {
                    File uploadsDir = new File(UPLOADS_DIR);
                    if (!uploadsDir.exists()) {
                        if (!uploadsDir.mkdirs()) {
                            throw new IOException("Failed to create Uploads directory: " + UPLOADS_DIR);
                        }
                    }
                    String imageName = System.currentTimeMillis() + "_" + selectedImageFile.getName();
                    File destFile = new File(UPLOADS_DIR, imageName);
                    Files.copy(selectedImageFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    workshopToEdit.setImage(destFile.getPath());
                }

                workshopToEdit.setPrice(Double.parseDouble(priceField.getText()));
                workshopToEdit.setAvailablePlaces(Integer.parseInt(availablePlacesField.getText()));

                // Set user_id from current user
                if (currentUser != null && currentUser.getId() > 0) {
                    workshopToEdit.setUser_id(currentUser.getId());
                } else {
                    throw new IllegalStateException("No logged-in user found in session");
                }

                workshopService.updateWorkshop(workshopToEdit);
                showAlert(Alert.AlertType.INFORMATION, "Succès", null, "Workshop mis à jour avec succès!");
                goBackToList();

            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la mise à jour", e.getMessage());
            }
        }
    }

    private boolean isInputValid() {
        StringBuilder errorMessage = new StringBuilder();

        // Validation du nom
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            errorMessage.append("- Nom invalide!\n");
            nameField.setStyle("-fx-border-color: red;");
        }

        // Validation de la description
        if (descriptionField.getText() == null || descriptionField.getText().trim().isEmpty()) {
            errorMessage.append("- Description invalide!\n");
            descriptionField.setStyle("-fx-border-color: red;");
        }

        // Validation des dates/heures de début
        Timestamp startTimestamp = null;
        if (startDatePicker.getValue() == null) {
            errorMessage.append("- Date de début requise!\n");
            startDatePicker.setStyle("-fx-border-color: red;");
        }
        if (startTimeField.getText() == null || startTimeField.getText().trim().isEmpty()) {
            errorMessage.append("- Heure de début requise!\n");
            startTimeField.setStyle("-fx-border-color: red;");
        } else {
            try {
                startTimestamp = parseDateTime(startDatePicker.getValue(), startTimeField.getText());
            } catch (Exception e) {
                errorMessage.append("- Format heure début invalide (HH:mm)!\n");
                startTimeField.setStyle("-fx-border-color: red;");
            }
        }

        // Validation des dates/heures de fin
        Timestamp endTimestamp = null;
        if (endDatePicker.getValue() == null) {
            errorMessage.append("- Date de fin requise!\n");
            endDatePicker.setStyle("-fx-border-color: red;");
        }
        if (endTimeField.getText() == null || endTimeField.getText().trim().isEmpty()) {
            errorMessage.append("- Heure de fin requise!\n");
            endTimeField.setStyle("-fx-border-color: red;");
        } else {
            try {
                endTimestamp = parseDateTime(endDatePicker.getValue(), endTimeField.getText());
            } catch (Exception e) {
                errorMessage.append("- Format heure fin invalide (HH:mm)!\n");
                endTimeField.setStyle("-fx-border-color: red;");
            }
        }

        // Validation que la date de fin est après la date de début
        if (startTimestamp != null && endTimestamp != null) {
            if (endTimestamp.before(startTimestamp) || endTimestamp.equals(startTimestamp)) {
                errorMessage.append("- La date/heure de fin doit être après la date/heure de début!\n");
                startDatePicker.setStyle("-fx-border-color: red;");
                startTimeField.setStyle("-fx-border-color: red;");
                endDatePicker.setStyle("-fx-border-color: red;");
                endTimeField.setStyle("-fx-border-color: red;");
            }
        }

        // Validation du lieu
        if (locationField.getText() == null || locationField.getText().trim().isEmpty()) {
            errorMessage.append("- Lieu invalide! Veuillez sélectionner un lieu sur la carte.\n");
            locationField.setStyle("-fx-border-color: red;");
        }

        // Validation de l'image
        if (selectedImageFile == null && (workshopToEdit == null || workshopToEdit.getImage() == null)) {
            errorMessage.append("- Veuillez sélectionner une image!\n");
        } else if (selectedImageFile != null && !selectedImageFile.getName().toLowerCase().matches(".*\\.(jpg|jpeg|png|gif)$")) {
            errorMessage.append("- Le fichier doit être une image (JPG, PNG, GIF)!\n");
        }

        // Validation du prix
        try {
            Double.parseDouble(priceField.getText());
        } catch (NumberFormatException e) {
            errorMessage.append("- Prix invalide!\n");
            priceField.setStyle("-fx-border-color: red;");
        }

        // Validation des places disponibles
        try {
            int places = Integer.parseInt(availablePlacesField.getText());
            if (places < 0) {
                errorMessage.append("- Places disponibles doit être positif!\n");
                availablePlacesField.setStyle("-fx-border-color: red;");
            }
        } catch (NumberFormatException e) {
            errorMessage.append("- Places disponibles invalides!\n");
            availablePlacesField.setStyle("-fx-border-color: red;");
        }

        // Validation de l'utilisateur connecté
        if (currentUser == null || currentUser.getId() <= 0) {
            errorMessage.append("- Aucun utilisateur connecté! Veuillez vous connecter.\n");
        }

        if (errorMessage.length() == 0) {
            return true;
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur de validation",
                    "Veuillez corriger les erreurs suivantes", errorMessage.toString());
            return false;
        }
    }

    private Timestamp parseDateTime(LocalDate date, String timeStr) throws DateTimeParseException {
        if (date == null || timeStr == null || timeStr.trim().isEmpty()) {
            throw new DateTimeParseException("Date or time is null", "", 0);
        }

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime time = LocalTime.parse(timeStr.trim(), timeFormatter);
        LocalDateTime dateTime = LocalDateTime.of(date, time);
        return Timestamp.valueOf(dateTime);
    }

    @FXML
    private void handleBrowseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif")
        );

        Stage stage = (Stage) browseImageButton.getScene().getWindow();
        selectedImageFile = fileChooser.showOpenDialog(stage);

        if (selectedImageFile != null) {
            imageField.setText(selectedImageFile.getAbsolutePath());
            try {
                Image image = new Image(selectedImageFile.toURI().toString());
                imagePreview.setImage(image);
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger l'image", e.getMessage());
            }
        }
    }

    @FXML
    private void handleClearFields() {
        clearFields();
    }

    private void clearFields() {
        nameField.clear();
        descriptionField.clear();
        startDatePicker.setValue(null);
        startTimeField.clear();
        endDatePicker.setValue(null);
        endTimeField.clear();
        locationField.clear();
        imageField.clear();
        imagePreview.setImage(null);
        selectedImageFile = null;
        priceField.clear();
        availablePlacesField.clear();

        // Réinitialiser les styles
        nameField.setStyle("");
        descriptionField.setStyle("");
        startDatePicker.setStyle("");
        startTimeField.setStyle("");
        endDatePicker.setStyle("");
        endTimeField.setStyle("");
        locationField.setStyle("");
        priceField.setStyle("");
        availablePlacesField.setStyle("");
    }

    @FXML
    private void handleShowWorkshopList(ActionEvent event) {
        goBackToList();
    }

    private void goBackToList() {
        try {
            Stage stage = (Stage) addButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_workshop/WorkshopListView.fxml"));
            Parent root = loader.load();
            stage.setScene(new Scene(root));
            stage.setTitle("Liste des Workshops");
            stage.setMaximized(true); // Ensure full-screen
            stage.show();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de Navigation",
                    "Impossible d'ouvrir la liste des workshops", e.getMessage());
        }
    }

    public void prepopulateFields(Workshop workshop) {
        if (workshop == null) return;

        nameField.setText(workshop.getName());
        descriptionField.setText(workshop.getDescription());
        locationField.setText(workshop.getLocation());
        priceField.setText(String.valueOf(workshop.getPrice()));
        availablePlacesField.setText(String.valueOf(workshop.getAvailablePlaces()));

        if (workshop.getStartsAt() != null) {
            LocalDateTime start = workshop.getStartsAt().toLocalDateTime();
            startDatePicker.setValue(start.toLocalDate());
            startTimeField.setText(start.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        }

        if (workshop.getEndsAt() != null) {
            LocalDateTime end = workshop.getEndsAt().toLocalDateTime();
            endDatePicker.setValue(end.toLocalDate());
            endTimeField.setText(end.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        }

        if (workshop.getImage() != null && !workshop.getImage().isEmpty()) {
            File imageFile = new File(workshop.getImage());
            if (imageFile.exists()) {
                selectedImageFile = imageFile;
                imageField.setText(imageFile.getAbsolutePath());
                imagePreview.setImage(new Image(imageFile.toURI().toString()));
            }
        }
    }

    public void setWorkshopToEdit(Workshop workshop) {
        this.workshopToEdit = workshop;
        prepopulateFields(workshop);
        addButton.setText("Modifier");
        addButton.setOnAction(e -> handleUpdateWorkshop());
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    private static void showAlert(Alert.AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}