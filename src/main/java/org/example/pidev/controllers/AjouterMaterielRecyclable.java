package org.example.pidev.controllers;

import org.example.pidev.Enum.StatutEnum;
import org.example.pidev.Enum.Type_materiel;
import org.example.pidev.entities.Entreprise;
import org.example.pidev.entities.MaterielRecyclable;
import org.example.pidev.entities.Accord;
import org.example.pidev.entities.User;
import org.example.pidev.entities.Role;
import org.example.pidev.services.EntrepriseService;
import org.example.pidev.services.ServiceMaterielRecyclable;
import org.example.pidev.services.ServiceAccord;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.DatePicker;
import java.time.LocalDate;
import java.time.ZoneId;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.stream.Collectors;

public class AjouterMaterielRecyclable implements Initializable {

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

    private File selectedImageFile;

    private ServiceMaterielRecyclable m = new ServiceMaterielRecyclable();
    private EntrepriseService serviceEntreprise = new EntrepriseService();
    private ServiceAccord serviceAccord = new ServiceAccord();

    // Add a field to hold the reference to ShowMaterielRecyclable controller
    private ShowMaterielRecyclable showMaterielRecyclableController;

    // Add a method to set the controller
    public void setShowMaterielRecyclableController(ShowMaterielRecyclable controller) {
        this.showMaterielRecyclableController = controller;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadSuppliers();
        ObservableList<Type_materiel> typesMateriel = FXCollections.observableArrayList(Type_materiel.values());
        typeMaterielComboBox.setItems(typesMateriel);

        // Ajouter un écouteur sur le ComboBox de type de matériel
        typeMaterielComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                filterSuppliersByType(newVal);
            }
        });
    }

    // Nouvelle méthode pour afficher le message de bienvenue
    public void displayWelcomeMessage() {
        if (showMaterielRecyclableController == null) {
            showAlert("Erreur", "Erreur de configuration: contrôleur non initialisé", Alert.AlertType.ERROR);
            return;
        }

        User loggedInUser = showMaterielRecyclableController.getCurrentUser();
        if (loggedInUser == null) {
            showAlert("Erreur", "Aucun utilisateur connecté", Alert.AlertType.ERROR);
            return;
        }

        if (loggedInUser.getName() == null || loggedInUser.getLastName() == null) {
            showAlert("Erreur", "Informations utilisateur incomplètes", Alert.AlertType.ERROR);
            return;
        }

        showAlert("Bienvenue", "Bienvenue " + loggedInUser.getName() + " " + loggedInUser.getLastName() + " !", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void chooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));

        Stage stage = (Stage) chooseImageButton.getScene().getWindow();
        selectedImageFile = fileChooser.showOpenDialog(stage);

        if (selectedImageFile != null) {
            imageLabel.setText(selectedImageFile.getName());
        } else {
            imageLabel.setText("Aucun fichier choisi");
        }
    }

    @FXML
    private void saveMateriel(ActionEvent event) {
        try {
            // 🔹 1. Récupérer les valeurs des champs
            String name = nameField.getText();
            String description = descriptionField.getText();
            String company_name = entrepriseComboBox.getValue();
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

            if (company_name == null) {
                showAlert("Champ manquant", "Veuillez sélectionner une entreprise.", Alert.AlertType.WARNING);
                return;
            }

            if (selectedType == null) {
                showAlert("Champ manquant", "Veuillez sélectionner un type de matériel.", Alert.AlertType.WARNING);
                return;
            }

            if (selectedImageFile == null) {
                showAlert("Champ manquant", "Veuillez sélectionner une image pour le matériel.", Alert.AlertType.WARNING);
                return;
            }

            // 🔹 2. Trouver l'entreprise correspondante
            Entreprise entrepriseObj = serviceEntreprise.getEntrepriseByName(company_name);
            if (entrepriseObj == null) {
                showAlert("Erreur", "Entreprise non trouvée.", Alert.AlertType.ERROR);
                return;
            }

            // 3️⃣ Sauvegarde de l'image
            String imagePath = "";
            if (selectedImageFile != null) {
                File destinationDir = new File("src/main/resources/img/materiels");
                if (!destinationDir.exists()) {
                    destinationDir.mkdirs();
                }

                try {
                    String fileExtension = selectedImageFile.getName().substring(selectedImageFile.getName().lastIndexOf("."));
                    String uniqueFileName = "img_" + UUID.randomUUID() + fileExtension;
                    File destinationFile = new File(destinationDir, uniqueFileName);

                    Files.copy(selectedImageFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    imagePath = uniqueFileName;

                } catch (IOException e) {
                    System.out.println("Erreur lors de la copie du fichier : " + e.getMessage());
                    e.printStackTrace();
                    showAlert("Erreur", "Impossible de sauvegarder l'image.", Alert.AlertType.ERROR);
                    return;
                }
            }

            // 4️⃣ Créer l'objet MaterielRecyclable
            MaterielRecyclable materiel = new MaterielRecyclable(
                    name, description, LocalDateTime.now(),
                    selectedType, imagePath, StatutEnum.en_attente,
                    entrepriseObj
            );
            User loggedInUser = showMaterielRecyclableController.getCurrentUser();
            materiel.setUser(loggedInUser); // Associer l'utilisateur statique au matériel

            // 5️⃣ Ajouter à la base de données
            m.ajouter(materiel);

            // Récupérer tous les matériaux et trouver celui qui vient d'être ajouté
            List<MaterielRecyclable> allMateriaux = m.afficher();
            MaterielRecyclable materielAvecId = allMateriaux.stream()
                    .filter(m -> m.getName().equals(name) && m.getDescription().equals(description))
                    .findFirst()
                    .orElse(null);

            if (materielAvecId == null) {
                showAlert("Erreur", "Impossible de récupérer l'ID du matériel.", Alert.AlertType.ERROR);
                return;
            }

            // 6️⃣ Créer un accord pour ce matériel
            Accord accord = new Accord();
            accord.setDateCreation(LocalDateTime.now());
            accord.setQuantity(0.0f); // Quantité initiale à 0
            accord.setOutput("output"); // Utiliser le statut du matériel
            accord.setMaterielRecyclable(materielAvecId);
            accord.setEntreprise(entrepriseObj);

            serviceAccord.ajouter(accord);

            // 7️⃣ Afficher un message de succès
            showAlert("Succès", "Matériau ajouté avec succès et accord créé!", Alert.AlertType.INFORMATION);

            // 8️⃣ Fermer la fenêtre d'ajout
            closeWindow();

            // 9️⃣ Mettre à jour le statut du matériel recyclable dans la base de données
            m.modifier(materielAvecId);

        } catch (SQLException e) {
            showAlert("Erreur", "Problème de base de données : " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String message, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message + "\n" + e.getMessage());
        alert.showAndWait();
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void closeWindow() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }

    private void navigateToMaterielList() {
        try {
            // Charger la vue de la liste des matériaux
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ShowMaterielRecyclable.fxml"));
            Parent root = loader.load();

            // Créer une nouvelle scène
            Scene scene = new Scene(root);

            // Obtenir la scène actuelle
            Stage currentStage = (Stage) saveButton.getScene().getWindow();

            // Définir la nouvelle scène
            currentStage.setScene(scene);
            currentStage.setTitle("Liste des Matériaux Recyclables");
            currentStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de naviguer vers la liste des matériaux: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void filterSuppliersByType(Type_materiel type) {
        try {
            // Récupérer toutes les entreprises
            List<Entreprise> allSuppliers = serviceEntreprise.getSuppliersWithField();
            /*  System.out.println("Nombre total d'entreprises fournisseurs : " + allSuppliers.size());*/

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
                   /* System.out.println("Entreprise " + entreprise.getCompanyName() +
                                     " (champ: " + entrepriseField + ") correspond au type " +
                                     typeName + " : " + matches);*/
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

    private void loadSuppliers() {
        try {
            List<Entreprise> suppliers = serviceEntreprise.getSuppliersWithField();
            ObservableList<String> supplierNames = FXCollections.observableArrayList();

            // Convertir les objets Entreprise en noms d'entreprises
            suppliers.forEach(entreprise -> {
                if (entreprise.getCompanyName() != null) {
                    supplierNames.add(entreprise.getCompanyName());
                }
            });

            // Configurer le ComboBox
            entrepriseComboBox.setItems(supplierNames);

            // Sélectionner la première entreprise si disponible
            if (!supplierNames.isEmpty()) {
                entrepriseComboBox.getSelectionModel().selectFirst();
            }

            /*  System.out.println("Nombre d'entreprises chargées : " + supplierNames.size());*/
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des fournisseurs : " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Une erreur est survenue lors du chargement des fournisseurs : " + e.getMessage());
            alert.showAndWait();
        }
    }
}
