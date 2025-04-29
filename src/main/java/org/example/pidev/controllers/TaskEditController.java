package org.example.pidev.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.pidev.entities.TaskWorkshop;
import org.example.pidev.entities.Workshop;
import org.example.pidev.services.ServiceTaskWorkshop;


import java.sql.Timestamp;
import java.time.LocalDateTime;

public class TaskEditController {

    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;
    @FXML private DatePicker startDateField;
    @FXML private DatePicker endDateField;
    @FXML private ComboBox<String> statusComboBox;

    private TaskWorkshop task;
    private Workshop workshop;
    private final ServiceTaskWorkshop taskService = new ServiceTaskWorkshop();
    private Stage stage;

    public void setTask(TaskWorkshop task) {
        this.task = task;
        populateFields();
    }

    public void setWorkshop(Workshop workshop) {
        this.workshop = workshop;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void initialize() {
        // Populate status options
        statusComboBox.getItems().addAll("À faire", "En cours", "Terminé");
    }

    private void populateFields() {
        if (task != null) {
            nameField.setText(task.getNom());
            descriptionField.setText(task.getDescription());
            if (task.getStartsAt() != null) {
                startDateField.setValue(task.getStartsAt().toLocalDateTime().toLocalDate());
            }
            if (task.getEndsAt() != null) {
                endDateField.setValue(task.getEndsAt().toLocalDateTime().toLocalDate());
            }
            statusComboBox.setValue(task.getStatus());
        }
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            try {
                task.setNom(nameField.getText().trim());
                task.setDescription(descriptionField.getText().trim());
                task.setStartsAt(Timestamp.valueOf(LocalDateTime.of(
                        startDateField.getValue(), LocalDateTime.now().toLocalTime())));
                task.setEndsAt(Timestamp.valueOf(LocalDateTime.of(
                        endDateField.getValue(), LocalDateTime.now().toLocalTime())));
                task.setStatus(statusComboBox.getValue());
                task.setWorkshopId(workshop.getId());

                taskService.updateTask(task);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Tâche mise à jour avec succès!");
                stage.close();

            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la mise à jour de la tâche: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleDelete() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText("Supprimer cette tâche ?");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cette tâche ?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    taskService.deleteTask(task.getId());
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Tâche supprimée avec succès!");
                    stage.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleCancel() {
        stage.close();
    }

    private boolean isInputValid() {
        StringBuilder errors = new StringBuilder();

        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            errors.append("Le nom est requis.\n");
        }
        if (descriptionField.getText() == null || descriptionField.getText().trim().isEmpty()) {
            errors.append("La description est requise.\n");
        }
        if (startDateField.getValue() == null) {
            errors.append("La date de début est requise.\n");
        }
        if (endDateField.getValue() == null) {
            errors.append("La date de fin est requise.\n");
        }
        if (statusComboBox.getValue() == null) {
            errors.append("Le statut est requis.\n");
        }

        if (errors.length() > 0) {
            showAlert(Alert.AlertType.ERROR, "Validation", errors.toString());
            return false;
        }
        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}