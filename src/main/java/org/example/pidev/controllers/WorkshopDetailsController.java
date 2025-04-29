// WorkshopDetailsController.java
package org.example.pidev.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.pidev.entities.TaskWorkshop;
import org.example.pidev.entities.Workshop;
import org.example.pidev.services.ServiceTaskWorkshop;
import org.example.pidev.services.ServiceWorkshop;

import java.io.File;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Optional;

public class WorkshopDetailsController {
    @FXML private Text titleText;
    @FXML private Text descriptionText;
    @FXML private Text startsAtText;
    @FXML private Text endsAtText;
    @FXML private Text locationText;
    @FXML private Text priceText;
    @FXML private Text availablePlacesText;
    @FXML private ImageView workshopImageView;
    @FXML private Button closeButton;
    @FXML private Button addTaskButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button tasksButton;
    @FXML private HBox buttonBox;

    private Stage stage;
    private Workshop currentWorkshop;
    private ServiceTaskWorkshop taskService = new ServiceTaskWorkshop();
    private ServiceWorkshop serviceWorkshop = new ServiceWorkshop();
    private Stage primaryStage;

    @FXML
    public void initialize() {
        if (closeButton != null) {
            if (workshopImageView != null) {
                FadeTransition fadeTransition = new FadeTransition(Duration.millis(800), workshopImageView);
                fadeTransition.setFromValue(0);
                fadeTransition.setToValue(1);
                fadeTransition.play();
            }

            closeButton.setOnMouseEntered(e -> {
                ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(200), closeButton);
                scaleTransition.setToX(1.05);
                scaleTransition.setToY(1.05);
                scaleTransition.play();
            });

            closeButton.setOnMouseExited(e -> {
                ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(200), closeButton);
                scaleTransition.setToX(1);
                scaleTransition.setToY(1);
                scaleTransition.play();
            });
        }
    }

    public void setWorkshop(Workshop workshop) {
        this.currentWorkshop = workshop;
        titleText.setText(workshop.getName());
        descriptionText.setText(workshop.getDescription());

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        startsAtText.setText(workshop.getStartsAt() != null ? dateFormat.format(workshop.getStartsAt()) : "N/A");
        endsAtText.setText(workshop.getEndsAt() != null ? dateFormat.format(workshop.getEndsAt()) : "N/A");

        locationText.setText(workshop.getLocation());
        priceText.setText(String.format("%.2f €", workshop.getPrice()));
        availablePlacesText.setText(String.valueOf(workshop.getAvailablePlaces()));

        loadWorkshopImage(workshop.getImage());
    }

    private void loadWorkshopImage(String imagePath) {
        try {
            if (imagePath != null && !imagePath.isEmpty()) {
                File file = new File(imagePath);
                if (file.exists()) {
                    workshopImageView.setImage(new Image(file.toURI().toString()));
                } else {
                    loadDefaultImage();
                }
            } else {
                loadDefaultImage();
            }
        } catch (Exception e) {
            System.err.println("Erreur de chargement de l'image du workshop: " + e.getMessage());
            loadDefaultImage();
        }
    }

    private void loadDefaultImage() {
        try {
            String defaultImagePath = "/images/default-workshop.png";
            InputStream is = getClass().getResourceAsStream(defaultImagePath);
            if (is != null) {
                workshopImageView.setImage(new Image(is));
            }
        } catch (Exception e) {
            System.err.println("Erreur de chargement de l'image par défaut: " + e.getMessage());
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void handleClose() {
        stage.close();
    }

    @FXML
    private void handleAddTask() {
        Stage taskStage = new Stage();
        taskStage.initModality(Modality.APPLICATION_MODAL);
        taskStage.setTitle("Ajouter une nouvelle tâche");

        // Champs du formulaire
        TextField nomField = new TextField();
        nomField.setPromptText("Nom de la tâche");
        nomField.getStyleClass().add("text-field");
        nomField.setPrefWidth(350);

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Description");
        descriptionArea.setWrapText(true);
        descriptionArea.getStyleClass().add("text-area");
        descriptionArea.setPrefRowCount(3);

        DatePicker startDatePicker = new DatePicker();
        startDatePicker.setPromptText("Date de début");
        startDatePicker.getStyleClass().add("date-picker");

        DatePicker endDatePicker = new DatePicker();
        endDatePicker.setPromptText("Date de fin");
        endDatePicker.getStyleClass().add("date-picker");

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("À faire", "En cours", "Terminé");
        statusCombo.setValue("À faire");
        statusCombo.getStyleClass().add("filter-combo-box");

        // Labels d'erreur avec style rouge
        Label nomError = new Label();
        nomError.getStyleClass().add("error-label");
        nomError.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        nomError.setVisible(false);

        Label descriptionError = new Label();
        descriptionError.getStyleClass().add("error-label");
        descriptionError.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        descriptionError.setVisible(false);

        Label dateError = new Label();
        dateError.getStyleClass().add("error-label");
        dateError.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        dateError.setVisible(false);

        Button saveButton = new Button("Enregistrer");
        saveButton.getStyleClass().addAll("button", "button-green");
        saveButton.setPrefWidth(120);

        saveButton.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), saveButton);
            st.setToX(1.05);
            st.setToY(1.05);
            st.play();
        });

        saveButton.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), saveButton);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        saveButton.setOnAction(e -> {
            // Réinitialiser les styles d'erreur
            resetErrorStyles(nomField, descriptionArea, startDatePicker, endDatePicker);
            nomError.setVisible(false);
            descriptionError.setVisible(false);
            dateError.setVisible(false);

            boolean isValid = true;

            // Validation du nom
            if (nomField.getText().isEmpty()) {
                nomError.setText("Le nom est obligatoire");
                nomError.setVisible(true);
                setErrorStyle(nomField);
                isValid = false;
            } else if (nomField.getText().length() > 50) {
                nomError.setText("Le nom ne doit pas dépasser 50 caractères");
                nomError.setVisible(true);
                setErrorStyle(nomField);
                isValid = false;
            }

            // Validation de la description
            if (descriptionArea.getText().isEmpty()) {
                descriptionError.setText("La description est obligatoire");
                descriptionError.setVisible(true);
                setErrorStyle(descriptionArea);
                isValid = false;
            } else if (descriptionArea.getText().length() > 255) {
                descriptionError.setText("La description ne doit pas dépasser 255 caractères");
                descriptionError.setVisible(true);
                setErrorStyle(descriptionArea);
                isValid = false;
            }

            // Validation des dates
            if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
                dateError.setText("Les dates sont obligatoires");
                dateError.setVisible(true);
                if (startDatePicker.getValue() == null) setErrorStyle(startDatePicker);
                if (endDatePicker.getValue() == null) setErrorStyle(endDatePicker);
                isValid = false;
            } else if (endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
                dateError.setText("La date de fin doit être après la date de début");
                dateError.setVisible(true);
                setErrorStyle(startDatePicker);
                setErrorStyle(endDatePicker);
                isValid = false;
            }

            if (isValid) {
                try {
                    Timestamp startsAt = Timestamp.valueOf(startDatePicker.getValue().atStartOfDay());
                    Timestamp endsAt = Timestamp.valueOf(endDatePicker.getValue().atStartOfDay());

                    TaskWorkshop newTask = new TaskWorkshop();
                    newTask.setNom(nomField.getText());
                    newTask.setDescription(descriptionArea.getText());
                    newTask.setStartsAt(startsAt);
                    newTask.setEndsAt(endsAt);
                    newTask.setStatus(statusCombo.getValue());
                    newTask.setWorkshopId(currentWorkshop.getId());

                    taskService.addTask(newTask);

                    showAlert("Succès", "Tâche ajoutée avec succès!", AlertType.INFORMATION);
                    taskStage.close();
                } catch (Exception ex) {
                    showAlert("Erreur", "Erreur lors de l'ajout de la tâche: " + ex.getMessage(), AlertType.ERROR);
                    ex.printStackTrace();
                }
            }
        });

        // Contrôle de saisie en temps réel
        nomField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 50) {
                nomField.setText(oldValue);
            }
        });

        descriptionArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 255) {
                descriptionArea.setText(oldValue);
            }
        });

        Label titleLabel = new Label("Nouvelle Tâche pour: " + currentWorkshop.getName());
        titleLabel.getStyleClass().add("title");
        titleLabel.setStyle("-fx-font-size: 20; -fx-padding: 0 0 15 0;");

        VBox layout = new VBox(10);
        layout.getStyleClass().add("vbox");
        layout.setPadding(new Insets(25));
        layout.getChildren().addAll(
                titleLabel,
                new Label("Nom:"), nomField, nomError,
                new Label("Description:"), descriptionArea, descriptionError,
                new Label("Date de début:"), startDatePicker,
                new Label("Date de fin:"), endDatePicker, dateError,
                new Label("Statut:"), statusCombo,
                new HBox(saveButton) {{
                    setAlignment(Pos.CENTER_RIGHT);
                    setPadding(new Insets(15, 0, 0, 0));
                }}
        );

        DropShadow shadow = new DropShadow();
        shadow.setRadius(20);
        shadow.setColor(Color.color(0, 0, 0, 0.15));
        layout.setEffect(shadow);

        Scene scene = new Scene(layout, 450, 550);
        scene.getStylesheets().add(getClass().getResource("/gestion_workshop/styles.css").toExternalForm());

        // Ajout des styles pour les champs invalides
        scene.getStylesheets().add("data:text/css," +
                ".error-field {" +
                "   -fx-border-color: red;" +
                "   -fx-border-width: 2px;" +
                "   -fx-border-radius: 3px;" +
                "}" +
                ".error-label {" +
                "   -fx-text-fill: red;" +
                "   -fx-font-size: 12px;" +
                "}");

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), layout);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        taskStage.setScene(scene);
        taskStage.show();
        fadeIn.play();
    }

    private void setErrorStyle(Control control) {
        if (!control.getStyleClass().contains("error-field")) {
            control.getStyleClass().add("error-field");
        }
    }

    private void resetErrorStyles(Control... controls) {
        for (Control control : controls) {
            control.getStyleClass().remove("error-field");
        }
    }

    private void showAlert(String title, String message, AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setWorkshopData(Workshop workshop, Stage primaryStage) {
        this.currentWorkshop = workshop;
        this.primaryStage = primaryStage;
        setWorkshop(workshop);

        editButton.setOnAction(e -> handleEditWorkshop());
        deleteButton.setOnAction(e -> handleDeleteWorkshop());
        tasksButton.setOnAction(e -> handleShowTasks());
    }

    @FXML
    private void handleEditWorkshop() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_workshop/WorkshopView.fxml"));
            Parent root = loader.load();

            WorkshopController controller = loader.getController();
            if (controller != null) {
                controller.setWorkshopToEdit(currentWorkshop);
            }

            primaryStage.setScene(new Scene(root));
            primaryStage.setTitle("Modifier Workshop");
            stage.close();
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erreur d'ouverture", "Impossible d'ouvrir la vue de modification: " + e.getMessage());
        }
    }

    private void handleDeleteWorkshop() {
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de Suppression");
        confirm.setHeaderText("Supprimer le workshop : " + currentWorkshop.getName());
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer ce workshop ? Cette action est irréversible.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                serviceWorkshop.deleteWorkshop(currentWorkshop.getId());
                showAlert("Suppression Réussie", "Workshop '" + currentWorkshop.getName() + "' supprimé avec succès !", AlertType.INFORMATION);
                stage.close();

                if (primaryStage != null) {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_workshop/WorkshopListView.fxml"));
                    Parent root = loader.load();
                    primaryStage.setScene(new Scene(root));
                    primaryStage.show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Erreur de Suppression", "Impossible de supprimer le workshop: " + e.getMessage(), AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleShowTasks() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_workshop/TaskListView.fxml"));
            Parent root = loader.load();

            TaskListController controller = loader.getController();
            controller.setWorkshop(currentWorkshop);

            Stage taskStage = new Stage();
            taskStage.setScene(new Scene(root));
            taskStage.setTitle("Tâches du Workshop: " + currentWorkshop.getName());
            taskStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erreur", "Impossible d'ouvrir la vue des tâches: " + e.getMessage());
        }
    }

    private void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}