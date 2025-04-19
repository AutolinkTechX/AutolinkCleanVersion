package org.example.pidev.controllers;
import javafx.scene.control.*;
import org.example.pidev.entities.Accord;
import org.example.pidev.entities.Entreprise;
import org.example.pidev.services.ServiceAccord;
import org.example.pidev.services.ServiceMaterielRecyclable;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;
import javafx.collections.FXCollections;
import org.example.pidev.Enum.StatutEnum;
import javafx.scene.control.DatePicker;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.geometry.Pos;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;

public class ShowAccords implements Initializable {

    @FXML
    private FlowPane accordContainer;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> dateSortComboBox;

    @FXML
    private ComboBox<String> statutFilter;

    @FXML
    private Button resetButton;
    private Entreprise entreprise;

    private ServiceAccord accordService = new ServiceAccord();
    private ServiceMaterielRecyclable materielService = new ServiceMaterielRecyclable();
    private List<Accord> allAccords;

    public void setEntreprise(Entreprise entreprise) {
        this.entreprise = entreprise;
        loadAccordsForEntreprise();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialiser les ComboBox avec leurs valeurs
        initializeDateSortComboBox();
        initializeStatusFilterComboBox();

        // Charger tous les accords
        try {
            allAccords = accordService.afficher();
            displayAccords(allAccords);
        } catch (SQLException e) {
            showErrorAlert("Erreur lors du chargement des accords", e);
        }

        // Configuration des écouteurs d'événements
        resetButton.setOnAction(e -> resetFilters());

        // Recherche dynamique en temps réel
        searchField.textProperty().addListener((observable, oldValue, newValue) -> handleSearch());

        // Écouteurs de changement pour les ComboBox
        dateSortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> handleSearch());
        statutFilter.valueProperty().addListener((obs, oldVal, newVal) -> handleSearch());
    }

    private void initializeDateSortComboBox() {
        dateSortComboBox.setItems(FXCollections.observableArrayList(
                "Tous",
                "Plus récent d'abord",
                "Plus ancien d'abord"
        ));
        dateSortComboBox.getSelectionModel().selectFirst();
    }

    private void initializeStatusFilterComboBox() {
        statutFilter.setItems(FXCollections.observableArrayList(
                "Tous",
                "En attente",
                "Accepté",
                "Refusé"
        ));
        statutFilter.getSelectionModel().selectFirst();
    }

    private void handleSearch() {
        String searchText = searchField.getText().toLowerCase().trim();
        String dateSort = dateSortComboBox.getValue();
        String statusFilter = statutFilter.getValue();

        // Récupérer uniquement les accords de l'entreprise connectée
        List<Accord> entrepriseAccords = accordService.getAccordsByEntrepriseId(entreprise.getId());

        // Filtrer par texte et statut
        List<Accord> filteredList = entrepriseAccords.stream()
                .filter(accord -> {
                    // Filtrage par texte
                    boolean matchesText = searchText.isEmpty() ||
                            accord.getMaterielRecyclable().getName().toLowerCase().contains(searchText);

                    // Filtrage par statut
                    boolean matchesStatus = statusFilter.equals("Tous") ||
                            (statusFilter.equals("En attente") && accord.getMaterielRecyclable().getStatut() == StatutEnum.en_attente) ||
                            (statusFilter.equals("Accepté") && accord.getMaterielRecyclable().getStatut() == StatutEnum.valide) ||
                            (statusFilter.equals("Refusé") && accord.getMaterielRecyclable().getStatut() == StatutEnum.refuse);

                    return matchesText && matchesStatus;
                })
                .collect(Collectors.toList());

        // Trier par date
        if (dateSort.equals("Plus récent d'abord")) {
            filteredList.sort(Comparator.comparing(Accord::getDateCreation).reversed());
        } else if (dateSort.equals("Plus ancien d'abord")) {
            filteredList.sort(Comparator.comparing(Accord::getDateCreation));
        }

        displayAccords(filteredList);
    }

    private void resetFilters() {
        searchField.clear();
        dateSortComboBox.getSelectionModel().selectFirst();
        statutFilter.getSelectionModel().selectFirst();
        loadAccordsForEntreprise();
    }

    private void displayAccords(List<Accord> accords) {
        accordContainer.getChildren().clear();

        if (accords.isEmpty()) {
            Label noResultsLabel = new Label("Aucun résultat trouvé");
            noResultsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #a05a2c;");
            accordContainer.getChildren().add(noResultsLabel);
        } else {
            for (Accord accord : accords) {
                VBox card = createAccordCard(accord);
                accordContainer.getChildren().add(card);
            }
        }
    }

    private VBox createAccordCard(Accord accord) {
        // Formatage des dates
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String dateCreation = accord.getDateCreation().format(formatter);
        String dateReception = accord.getDateReception() != null ?
                accord.getDateReception().format(formatter) : "En attente de décision";

        // Création de l'image du matériel avec gestion d'erreur améliorée
        ImageView materielImageView = new ImageView();
        materielImageView.setFitWidth(140);
        materielImageView.setFitHeight(140);
        materielImageView.setPreserveRatio(true);

        try {
            String imagePath = accord.getMaterielRecyclable().getImage();
            if (imagePath != null && !imagePath.isEmpty()) {
                Image image = new Image("file:src/main/resources/img/materiels/" + imagePath);
                materielImageView.setImage(image);
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement de l'image : " + e.getMessage());
        }

        // Création des labels pour afficher les informations
        Label materielLabel = new Label(accord.getMaterielRecyclable().getName());
        materielLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #a05a2c;");

        Label dateCreationLabel = new Label("Date de création : " + dateCreation);
        dateCreationLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #333;");

        Label dateReceptionLabel = new Label("Date de réception : " + dateReception);
        dateReceptionLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #333;");

        Label statutLabel = new Label("Statut : " + accord.getMaterielRecyclable().getStatut());
        statutLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #333;");

        Label entrepriseLabel = new Label("Entreprise : " + accord.getEntreprise().getCompanyName());
        entrepriseLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #333;");

        // Bouton Modifier avec icône
        Button modifierButton = new Button();
        modifierButton.setStyle("-fx-background-color: transparent;");
        try {
            ImageView modifierIconView = new ImageView(new Image("file:src/main/resources/icons/modif.png"));
            modifierIconView.setFitWidth(30);
            modifierIconView.setFitHeight(30);
            modifierIconView.setPreserveRatio(true);
            modifierButton.setGraphic(modifierIconView);
            modifierButton.setTooltip(new Tooltip("Modifier"));
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement de l'icône modifier : " + e.getMessage());
            modifierButton.setText("M");
        }
        modifierButton.setOnAction(e -> showModifyDialog(accord));

        // Bouton Supprimer avec icône
        Button supprimerButton = new Button();
        supprimerButton.setStyle("-fx-background-color: transparent;");
        try {
            ImageView supprimerIconView = new ImageView(new Image("file:src/main/resources/icons/supp2.png"));
            supprimerIconView.setFitWidth(30);
            supprimerIconView.setFitHeight(30);
            supprimerIconView.setPreserveRatio(true);
            supprimerButton.setGraphic(supprimerIconView);
            supprimerButton.setTooltip(new Tooltip("Supprimer"));
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement de l'icône supprimer : " + e.getMessage());
            supprimerButton.setText("S");
        }
        supprimerButton.setOnAction(e -> handleSupprimer(accord));

        // Bouton Accepter avec icône
        Button accepterButton = new Button();
        accepterButton.setStyle("-fx-background-color: transparent;");
        try {
            ImageView accepterIconView = new ImageView(new Image("file:src/main/resources/icons/accept.png", 23, 23, true, true));
            accepterButton.setGraphic(accepterIconView);
            accepterButton.setTooltip(new Tooltip("Accepter"));
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement de l'icône accepter : " + e.getMessage());
            accepterButton.setText("A");
        }
        accepterButton.setOnAction(e -> handleAccepter(accord));

        // Bouton Refuser avec icône
        Button refuserButton = new Button();
        refuserButton.setStyle("-fx-background-color: transparent;");
        try {
            ImageView refuserIconView = new ImageView(new Image("file:src/main/resources/icons/refuse.png", 23, 23, true, true));
            refuserButton.setGraphic(refuserIconView);
            refuserButton.setTooltip(new Tooltip("Refuser"));
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement de l'icône refuser : " + e.getMessage());
            refuserButton.setText("R");
        }
        refuserButton.setOnAction(e -> handleRefuser(accord));

        HBox buttonContainer = new HBox(15, modifierButton, supprimerButton, accepterButton, refuserButton);
        buttonContainer.setStyle("-fx-alignment: center;");

        VBox card = new VBox(8, materielImageView, materielLabel, dateCreationLabel, dateReceptionLabel,
                statutLabel, entrepriseLabel, buttonContainer);
        card.setStyle("-fx-padding: 12; -fx-background-color: white; -fx-border-color: #a05a2c; "
                + "-fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-alignment: center;");
        card.setPrefWidth(220);
        card.setMinHeight(350);

        return card;
    }

    private void showModifyDialog(Accord accord) {
        Dialog<Accord> dialog = new Dialog<>();
        dialog.setTitle("Modifier l'accord");
        dialog.setHeaderText("Modifier les détails de l'accord");

        // Création des champs de formulaire
        ComboBox<String> statutComboBox = new ComboBox<>(FXCollections.observableArrayList(
                "en_attente", "valide", "refuse"
        ));
        statutComboBox.setValue(accord.getMaterielRecyclable().getStatut().toString());

        // Ajout du champ Date de réception
        DatePicker dateReceptionPicker = new DatePicker();
        if (accord.getDateReception() != null) {
            dateReceptionPicker.setValue(accord.getDateReception().toLocalDate());
        }

        // Création de la grille de formulaire
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Statut:"), 0, 0);
        grid.add(statutComboBox, 1, 0);
        grid.add(new Label("Date de réception:"), 0, 1);
        grid.add(dateReceptionPicker, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Ajout des boutons OK et Annuler
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okButton, cancelButton);

        // Gestion de la réponse
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButton) {
                try {
                    // Ne plus modifier la quantité
                    accord.getMaterielRecyclable().setStatut(StatutEnum.valueOf(statutComboBox.getValue()));
                    materielService.modifier(accord.getMaterielRecyclable());

                    // Mise à jour de la date de réception
                    if (dateReceptionPicker.getValue() != null) {
                        LocalDate selectedDate = dateReceptionPicker.getValue();
                        LocalDateTime dateTime = LocalDateTime.of(selectedDate, LocalTime.now());
                        accord.setDateReception(dateTime);
                    }

                    accord.setOutput("output");
                    accordService.modifier(accord);
                    loadAccordsForEntreprise();
                    return accord;
                } catch (SQLException e) {
                    showErrorAlert("Erreur lors de la modification", e);
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void handleAccepter(Accord accord) {
        try {
            // Mettre à jour le statut du matériel recyclable
            accord.getMaterielRecyclable().setStatut(StatutEnum.valide);
            materielService.modifier(accord.getMaterielRecyclable());

            // Mettre à jour l'accord
            accord.setDateReception(LocalDateTime.now());
            accord.setOutput("output");
            accordService.modifier(accord);

            loadAccordsForEntreprise();
            showSuccess("Accord accepté avec succès");
        } catch (SQLException e) {
            showErrorAlert("Erreur lors de l'acceptation", e);
        }
    }

    private void handleRefuser(Accord accord) {
        try {
            // Mettre à jour le statut du matériel recyclable
            accord.getMaterielRecyclable().setStatut(StatutEnum.refuse);
            materielService.modifier(accord.getMaterielRecyclable());

            // Mettre à jour l'accord
            accord.setDateReception(LocalDateTime.now());
            accord.setOutput("output");
            accordService.modifier(accord);

            loadAccordsForEntreprise();
            showSuccess("Accord refusé avec succès");
        } catch (SQLException e) {
            showErrorAlert("Erreur lors du refus", e);
        }
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String message, Exception e) {
        e.printStackTrace();
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message + " : " + e.getMessage());
        alert.showAndWait();
    }

    private void handleSupprimer(Accord accord) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText(null);
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cet accord ?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                accordService.supprimer(accord.getId());
                loadAccordsForEntreprise();
                showSuccess("Accord supprimé avec succès");
            } catch (SQLException e) {
                showErrorAlert("Erreur lors de la suppression", e);
            }
        }
    }

    /**
     * Formate une date LocalDateTime en chaîne de caractères
     *
     * @param date La date à formater
     * @return La date formatée au format "dd/MM/yyyy HH:mm"
     */
    private String formatDate(LocalDateTime date) {
        if (date == null) {
            return "Non spécifiée";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return date.format(formatter);
    }

    private void loadAccordsForEntreprise() {
        if (entreprise == null) return;

        int entrepriseId = entreprise.getId();

        // Récupère uniquement les accords liés à l'entreprise connectée
        List<Accord> accords = accordService.getAccordsByEntrepriseId(entrepriseId);

        accordContainer.getChildren().clear(); // Vide le container avant d'ajouter

        for (Accord accord : accords) {
            // Tu peux créer ici un card personnalisé (on peut faire ça aussi si tu veux)
            VBox card = createAccordCard(accord);
            accordContainer.getChildren().add(card);
        }
    }

}
