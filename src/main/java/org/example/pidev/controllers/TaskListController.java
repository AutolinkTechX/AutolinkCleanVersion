package org.example.pidev.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.example.pidev.entities.TaskWorkshop;
import org.example.pidev.entities.Workshop;
import org.example.pidev.services.ServiceTaskWorkshop;


import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class TaskListController implements Initializable {

    @FXML private ListView<TaskWorkshop> taskList;
    @FXML private Button deleteButton;

    private Workshop workshop;
    private final ServiceTaskWorkshop taskService = new ServiceTaskWorkshop();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy - HH:mm");

    public void setWorkshop(Workshop workshop) {
        this.workshop = workshop;
        loadTasks();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupListCellFactory();
        setupListSelectionListener();
    }

    private void loadTasks() {
        if (workshop != null) {
            ObservableList<TaskWorkshop> tasks = FXCollections.observableArrayList(
                    taskService.getTasksForWorkshop(workshop.getId())
            );
            taskList.setItems(tasks);
        }
    }

    private void setupListCellFactory() {
        taskList.setCellFactory(new Callback<ListView<TaskWorkshop>, ListCell<TaskWorkshop>>() {
            @Override
            public ListCell<TaskWorkshop> call(ListView<TaskWorkshop> param) {
                return new ListCell<TaskWorkshop>() {
                    @Override
                    protected void updateItem(TaskWorkshop task, boolean empty) {
                        super.updateItem(task, empty);

                        if (empty || task == null) {
                            setGraphic(null);
                        } else {
                            setGraphic(createTaskCard(task));
                        }
                    }
                };
            }
        });
    }

    private VBox createTaskCard(TaskWorkshop task) {
        // Conteneur principal
        VBox card = new VBox();
        card.setStyle("-fx-background-color: white; " +
                "-fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2); " +
                "-fx-padding: 20; " +
                "-fx-spacing: 12;");

        // Header avec titre et statut
        HBox headerBox = new HBox();
        headerBox.setSpacing(15);
        headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Titre
        Text title = new Text(task.getNom());
        title.setFont(Font.font("Segoe UI Semibold", 18));
        title.setStyle("-fx-fill: #333;");

        // Statut
        Label status = new Label(task.getStatus().toUpperCase());
        status.setStyle("-fx-background-color: #b5602c; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 12px; " +
                "-fx-padding: 3 10; " +
                "-fx-background-radius: 10;");

        headerBox.getChildren().addAll(title, status);

        // Description
        Text description = new Text(task.getDescription());
        description.setFont(Font.font("Segoe UI", 14));
        description.setStyle("-fx-fill: #666;");
        description.setWrappingWidth(650);

        // Dates
        HBox datesBox = new HBox(25);
        datesBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        LocalDateTime start = task.getStartsAt().toLocalDateTime();
        LocalDateTime end = task.getEndsAt().toLocalDateTime();

        HBox startBox = createDateBox("DÉBUT", start.format(dateFormatter), "#b5602c");
        HBox endBox = createDateBox("FIN", end.format(dateFormatter), "#b5602c");

        datesBox.getChildren().addAll(startBox, endBox);

        // Boutons d'action (Edit)
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button editButton = new Button("Modifier");
        editButton.setStyle("-fx-background-color: #b5602c; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 12px; " +
                "-fx-padding: 5 15; " +
                "-fx-background-radius: 8px; " +
                "-fx-cursor: hand;");
        editButton.setOnAction(event -> handleEdit(task));

        buttonBox.getChildren().add(editButton);

        // Ajout des éléments à la carte
        card.getChildren().addAll(headerBox, description, datesBox, buttonBox);

        return card;
    }

    private HBox createDateBox(String label, String date, String color) {
        HBox box = new HBox(8);
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label dateLabel = new Label(label + ":");
        dateLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 12px;");

        Text dateText = new Text(date);
        dateText.setStyle("-fx-fill: #555; -fx-font-size: 14px;");

        box.getChildren().addAll(dateLabel, dateText);
        return box;
    }

    private void setupListSelectionListener() {
        taskList.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean isSelected = newSelection != null;
            deleteButton.setDisable(!isSelected);
        });
    }

    @FXML
    private void handleEdit(TaskWorkshop task) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_workshop/TaskEditView.fxml"));
            Parent root = loader.load();

            TaskEditController controller = loader.getController();
            controller.setTask(task); // Pass the task to edit
            controller.setWorkshop(workshop); // Pass the workshop for context

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Modifier Tâche");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(taskList.getScene().getWindow());

            // Refresh tasks after edit
            stage.setOnHidden(event -> loadTasks());
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la fenêtre de modification: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        Stage stage = (Stage) taskList.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleDelete() {
        TaskWorkshop selectedTask = taskList.getSelectionModel().getSelectedItem();
        if (selectedTask != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation de suppression");
            confirm.setHeaderText("Supprimer cette tâche ?");
            confirm.setContentText("Êtes-vous sûr de vouloir supprimer cette tâche ?");

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    taskService.deleteTask(selectedTask.getId());
                    loadTasks();
                }
            });
        } else {
            showAlert("Aucune sélection", "Veuillez sélectionner une tâche à supprimer.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}