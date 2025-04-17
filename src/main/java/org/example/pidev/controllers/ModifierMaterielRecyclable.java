package org.example.pidev.controllers;
import org.example.pidev.Enum.Type_materiel;
import org.example.pidev.entities.Entreprise;
import org.example.pidev.entities.MaterielRecyclable;
import org.example.pidev.services.EntrepriseService;
import org.example.pidev.services.ServiceMaterielRecyclable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ModifierMaterielRecyclable {

    @FXML
    private TextField nameField;

    @FXML
    private TextArea descriptionField;

    @FXML
    private ComboBox<String> entrepriseComboBox;

    @FXML
    private ComboBox<Type_materiel> typeMaterielComboBox;

    @FXML
    private Button chooseImageButton;

    @FXML
    private Label imageLabel;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    private File selectedImageFile;
    private MaterielRecyclable materielToEdit;
    private ServiceMaterielRecyclable materielService = new ServiceMaterielRecyclable();
    private EntrepriseService entrepriseService = new EntrepriseService();

    public void setMaterielToEdit(MaterielRecyclable materiel) {
        this.materielToEdit = materiel;
        // Remplir les champs avec les données du matériel
        nameField.setText(materiel.getName());
        descriptionField.setText(materiel.getDescription());
        entrepriseComboBox.setValue(materiel.getEntreprise().getCompanyName());
        typeMaterielComboBox.setValue(materiel.getType_materiel());
        imageLabel.setText(materiel.getImage());
    }

    @FXML
    public void initialize() {
        // Charger les entreprises
        ObservableList<String> entreprises = FXCollections.observableArrayList();
        try {
            for (Entreprise entreprise : entrepriseService.getSuppliers()) {
                entreprises.add(entreprise.getCompanyName());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors du chargement des entreprises : " + e.getMessage(), Alert.AlertType.ERROR);
        }
        entrepriseComboBox.setItems(entreprises);

        // Charger les types de matériaux
        ObservableList<Type_materiel> typesMateriel = FXCollections.observableArrayList(Type_materiel.values());
        typeMaterielComboBox.setItems(typesMateriel);

        // Ajouter un écouteur sur le ComboBox de type de matériel
        typeMaterielComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                filterSuppliersByType(newVal);
            }
        });
    }

    @FXML
    private void chooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));

        Stage stage = (Stage) chooseImageButton.getScene().getWindow();
        selectedImageFile = fileChooser.showOpenDialog(stage);

        if (selectedImageFile != null) {
            imageLabel.setText(selectedImageFile.getName());
        }
    }

    private void filterSuppliersByType(Type_materiel type) {
        try {
            // Récupérer toutes les entreprises
            List<Entreprise> allSuppliers = entrepriseService.getSuppliersWithField();
            System.out.println("Nombre total d'entreprises fournisseurs : " + allSuppliers.size());

            // Filtrer les entreprises en fonction du type sélectionné
            List<String> filteredSupplierNames = allSuppliers.stream()
                    .filter(entreprise -> {
                        if (entreprise.getField() == null) {
                            System.out.println("Entreprise " + entreprise.getCompanyName() + " n'a pas de champ défini");
                            return false;
                        }

                        // Convertir le champ de l'entreprise et le type en minuscules pour une comparaison insensible à la casse
                        String entrepriseField = entreprise.getField().toLowerCase().trim();
                        String typeName = type.name().toLowerCase().trim();

                        // Vérifier si le champ de l'entreprise contient le nom du type sélectionné
                        boolean matches = entrepriseField.contains(typeName);
                        System.out.println("Entreprise " + entreprise.getCompanyName() +
                                " (champ: " + entrepriseField + ") correspond au type " +
                                typeName + " : " + matches);
                        return matches;
                    })
                    .map(Entreprise::getCompanyName)
                    .collect(Collectors.toList());

            System.out.println("Nombre d'entreprises filtrées : " + filteredSupplierNames.size());

            // Mettre à jour le ComboBox des entreprises
            ObservableList<String> supplierNames = FXCollections.observableArrayList(filteredSupplierNames);
            entrepriseComboBox.setItems(supplierNames);

            // Sélectionner la première entreprise si disponible
            if (!supplierNames.isEmpty()) {
                entrepriseComboBox.getSelectionModel().selectFirst();
                System.out.println("Première entreprise sélectionnée : " + entrepriseComboBox.getValue());
            } else {
                System.out.println("Aucune entreprise ne correspond au type sélectionné");
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Aucun fournisseur");
                alert.setHeaderText(null);
                alert.setContentText("Aucune entreprise fournisseur ne correspond au type de matériel sélectionné.");
                alert.showAndWait();
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du filtrage des entreprises : " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Une erreur est survenue lors du filtrage des entreprises : " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void saveMateriel(ActionEvent event) {
        try {
            // Récupérer les valeurs des champs
            String name = nameField.getText();
            String description = descriptionField.getText();
            String companyName = entrepriseComboBox.getValue();
            Type_materiel selectedType = typeMaterielComboBox.getValue();

            // 🔒 Contrôle de saisie amélioré sans style rouge
            if (name == null || name.trim().isEmpty()) {
                showAlert("Champ manquant", "Veuillez entrer un nom pour le matériel.", Alert.AlertType.WARNING);
                return;
            }
            if (name.trim().length() < 4) {
                showAlert("Nom invalide", "Le nom doit contenir au moins 4 caractères.", Alert.AlertType.WARNING);
                return;
            }

            if (description == null || description.trim().isEmpty()) {
                showAlert("Champ manquant", "Veuillez entrer une description pour le matériel.", Alert.AlertType.WARNING);
                return;
            }
            if (description.trim().length() < 7) {
                showAlert("Description invalide", "La description doit contenir au moins 7 caractères.", Alert.AlertType.WARNING);
                return;
            }

            if (companyName == null) {
                showAlert("Champ manquant", "Veuillez sélectionner une entreprise.", Alert.AlertType.WARNING);
                return;
            }

            if (selectedType == null) {
                showAlert("Champ manquant", "Veuillez sélectionner un type de matériel.", Alert.AlertType.WARNING);
                return;
            }

            // Vérifier si l'entreprise a changé
            boolean entrepriseChanged = !materielToEdit.getEntreprise().getCompanyName().equals(companyName);

            // Mettre à jour le matériel
            materielToEdit.setName(name);
            materielToEdit.setDescription(description);
            materielToEdit.setType_materiel(selectedType);

            // Si l'entreprise a changé, mettre à jour l'entreprise
           /* if (entrepriseChanged) {
                Entreprise newEntreprise = entrepriseService.getEntrepriseByName(companyName);
                if (newEntreprise != null) {
                    materielToEdit.setEntreprise(newEntreprise);
                }
            }*/

            // Gérer l'image si une nouvelle a été sélectionnée
            if (selectedImageFile != null) {
                String imagePath = saveImage(selectedImageFile);
                materielToEdit.setImage(imagePath);
            }

            // Sauvegarder les modifications
            materielService.modifier(materielToEdit);

            // Fermer la fenêtre de modification
            closeWindow();

        } catch (Exception e) {
            showAlert("Erreur", "Une erreur est survenue lors de la modification : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void cancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String saveImage(File imageFile) throws IOException {
        File destinationDir = new File("src/main/resources/img/materiels");
        if (!destinationDir.exists()) {
            destinationDir.mkdirs();
        }

        String fileExtension = imageFile.getName().substring(imageFile.getName().lastIndexOf("."));
        String uniqueFileName = "img_" + UUID.randomUUID() + fileExtension;
        File destinationFile = new File(destinationDir, uniqueFileName);

        Files.copy(imageFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return uniqueFileName;
    }
}
