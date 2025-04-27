package org.example.pidev.controllers;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import org.example.pidev.entities.Accord;
import org.example.pidev.entities.Entreprise;
import org.example.pidev.services.ServiceAccord;
import org.example.pidev.services.ServiceMaterielRecyclable;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Insets;
import javafx.collections.FXCollections;
import org.example.pidev.Enum.StatutEnum;
import javafx.scene.control.DatePicker;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.geometry.Pos;
import org.example.pidev.websocket.NotificationClient;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;
import javafx.collections.ObservableList;
import java.util.HashSet;
import java.util.Set;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.HashMap;

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


    @FXML
    private ImageView notificationIcon;
    @FXML
    private Label notificationLabel;
    @FXML
    private Button notificationButton;

    @FXML
    private ListView<String> recentAccordsListView;

    private boolean newNotification = false;


    private ServiceAccord accordService = new ServiceAccord();
    private ServiceMaterielRecyclable materielService = new ServiceMaterielRecyclable();
    private List<Accord> allAccords;

    private int notificationCount = 0;

    private NotificationClient client;

    private Set<Integer> seenAccordIds = new HashSet<>();
    private LocalDateTime lastCheckTime;
    private static final String NOTIFICATIONS_FILE = "notifications.dat";

    private void saveSeenNotifications() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(NOTIFICATIONS_FILE))) {
            // Sauvegarder l'ID de l'entreprise avec les notifications lues
            Map<Integer, Set<Integer>> enterpriseNotifications = new HashMap<>();
            enterpriseNotifications.put(entreprise.getId(), seenAccordIds);
            oos.writeObject(enterpriseNotifications);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadSeenNotifications() {
        File file = new File(NOTIFICATIONS_FILE);
        if (!file.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(NOTIFICATIONS_FILE))) {
            Map<Integer, Set<Integer>> enterpriseNotifications = (Map<Integer, Set<Integer>>) ois.readObject();
            if (entreprise != null && enterpriseNotifications.containsKey(entreprise.getId())) {
                seenAccordIds = enterpriseNotifications.get(entreprise.getId());
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setEntreprise(Entreprise entreprise) {
        this.entreprise = entreprise;
        loadSeenNotifications(); // Charger les notifications lues au moment de définir l'entreprise
        loadAccordsForEntreprise();
        updateNotificationCount();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialisation des éléments...
        initializeDateSortComboBox();
        initializeStatusFilterComboBox();
        lastCheckTime = LocalDateTime.now();

        try {
            allAccords = accordService.afficher();
            displayAccords(allAccords);
        } catch (SQLException e) {
            showErrorAlert("Erreur lors du chargement des accords", e);
        }

        resetButton.setOnAction(e -> resetFilters());

        searchField.textProperty().addListener((observable, oldValue, newValue) -> handleSearch());
        dateSortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> handleSearch());
        statutFilter.valueProperty().addListener((obs, oldVal, newVal) -> handleSearch());

        // Initialize notification count
        updateNotificationCount();

        notificationButton.setOnAction(e -> {
            showNewAccordPopup();
            updateNotificationCount();
            saveSeenNotifications(); // Sauvegarder après avoir vu les notifications
        });

        // Augmenter la taille de l'icône de notification
        notificationIcon.setFitWidth(22);  // Augmenté de 20 à 25
        notificationIcon.setFitHeight(22); // Augmenté de 20 à 25

        // Améliorer le style du label de notification
        notificationLabel.setStyle("-fx-text-fill: white; " +
                "-fx-font-size: 12px; " +  // Augmenté de 10px à 12px
                "-fx-font-weight: bold; " +
                "-fx-background-color: red; " +
                "-fx-padding: 2 6; " +     // Augmenté le padding
                "-fx-background-radius: 12; " + // Augmenté pour garder la forme ronde
                "-fx-min-width: 22; " +    // Augmenté de 18 à 22
                "-fx-min-height: 22;");    // Ajouté pour maintenir la forme circulaire

        notificationButton.setStyle("-fx-background-color: transparent; -fx-padding: 5;");
    }

    private void updateNotificationCount() {
        if (entreprise != null) {
            List<Accord> recentAccords = accordService.getRecentAccordsByEntrepriseId(entreprise.getId());
            int unseenCount = 0;

            for (Accord accord : recentAccords) {
                if (!seenAccordIds.contains(accord.getId())) {
                    unseenCount++;
                }
            }

            notificationCount = unseenCount;

            Platform.runLater(() -> {
                if (notificationCount > 0) {
                    notificationLabel.setText(String.valueOf(notificationCount));
                    notificationLabel.setVisible(true);

                    // Améliorer l'effet visuel de l'icône
                    ColorAdjust colorAdjust = new ColorAdjust();
                    colorAdjust.setHue(0.7);
                    colorAdjust.setSaturation(1);
                    colorAdjust.setBrightness(0.1); // Ajouté pour plus de contraste
                    notificationIcon.setEffect(colorAdjust);

                    // Ajouter un effet de mise en évidence
                    notificationIcon.setStyle("-fx-effect: dropshadow(gaussian, #ff0000, 10, 0, 0, 0);");
                } else {
                    notificationLabel.setVisible(false);
                    notificationIcon.setEffect(null);
                    notificationIcon.setStyle(null);
                }
            });
        }
    }

    private void showNewAccordPopup() {
        Dialog<ButtonType> popup = new Dialog<>();
        popup.setTitle("Centre de notifications");

        List<Accord> recentAccords = accordService.getRecentAccordsByEntrepriseId(entreprise.getId());

        // En-tête personnalisé avec taille augmentée
        Label headerLabel = new Label("Notifications");
        headerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2b2b2b; -fx-padding: 15 0 15 0;");

        // Création du ComboBox pour le filtre avec style amélioré
        ComboBox<String> filterComboBox = new ComboBox<>();
        filterComboBox.getItems().addAll("Toutes les notifications", "Non lues", "Lues");
        filterComboBox.setValue("Toutes les notifications");
        filterComboBox.setPrefWidth(250); // Augmentation de la largeur du ComboBox
        filterComboBox.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #e0e0e0; " +
                "-fx-border-radius: 5; " +
                "-fx-background-radius: 5; " +
                "-fx-padding: 8; " +  // Padding augmenté
                "-fx-font-size: 14px;"); // Taille de police augmentée

        // Conteneur pour le filtre avec icône
        HBox filterContainer = new HBox(15); // Espacement augmenté
        ImageView filterIcon = new ImageView(new Image("file:src/main/resources/icons/filter.png"));
        filterIcon.setFitHeight(20); // Taille d'icône augmentée
        filterIcon.setFitWidth(20);
        Label filterLabel = new Label("Filtrer par :");
        filterLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;"); // Police augmentée
        filterContainer.getChildren().addAll(filterIcon, filterLabel, filterComboBox);
        filterContainer.setAlignment(Pos.CENTER_LEFT);
        filterContainer.setStyle("-fx-padding: 15; -fx-background-color: #f8f9fa; -fx-background-radius: 5;"); // Padding augmenté

        // Création de la ListView avec taille augmentée
        ListView<Accord> accordListView = new ListView<>();
        accordListView.setPrefHeight(300); // Reduced height
        accordListView.setPrefWidth(400); // Reduced width
        accordListView.setMinHeight(200); // Reduced minimum height
        accordListView.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #e0e0e0; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8; " +
                "-fx-padding: 10;"); // Padding augmenté

        // Style personnalisé pour chaque élément de la liste
        accordListView.setCellFactory(lv -> new ListCell<Accord>() {
            @Override
            protected void updateItem(Accord accord, boolean empty) {
                super.updateItem(accord, empty);
                if (empty || accord == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    VBox container = new VBox(10); // Espacement augmenté entre les éléments

                    // Informations de l'accord avec taille de police augmentée
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    Label nameLabel = new Label(accord.getMaterielRecyclable().getName());
                    Label dateLabel = new Label(accord.getDateCreation().format(formatter));
                    Label statusLabel = new Label(accord.getMaterielRecyclable().getStatut().toString());

                    // Style des labels avec taille augmentée
                    nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;"); // Police augmentée
                    dateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;"); // Police augmentée
                    statusLabel.setStyle("-fx-font-size: 14px; -fx-background-color: #e9ecef; " +
                            "-fx-padding: 4 12; -fx-background-radius: 12;"); // Padding augmenté

                    // Conteneur pour le statut
                    HBox statusContainer = new HBox(8);
                    statusContainer.setAlignment(Pos.CENTER_LEFT);
                    statusContainer.getChildren().addAll(statusLabel);

                    // Ajout des éléments au conteneur
                    container.getChildren().addAll(nameLabel, dateLabel, statusContainer);
                    container.setPadding(new Insets(12)); // Padding augmenté

                    boolean isRead = seenAccordIds.contains(accord.getId());

                    // Style du conteneur selon l'état de lecture
                    if (isRead) {
                        container.setStyle("-fx-background-color: #ffffff; " +
                                "-fx-padding: 15; " + // Padding augmenté
                                "-fx-border-color: transparent transparent #dee2e6 transparent; " +
                                "-fx-border-width: 0 0 1 0;");
                    } else {
                        container.setStyle("-fx-background-color: #e7f3ff; " +
                                "-fx-padding: 15; " + // Padding augmenté
                                "-fx-border-color: transparent transparent #dee2e6 transparent; " +
                                "-fx-border-width: 0 0 1 0;");
                    }

                    // Effet de survol
                    container.setOnMouseEntered(e -> {
                        if (isRead) {
                            container.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15;");
                        } else {
                            container.setStyle("-fx-background-color: #d4e5f9; -fx-padding: 15;");
                        }
                    });

                    container.setOnMouseExited(e -> {
                        if (isRead) {
                            container.setStyle("-fx-background-color: #ffffff; -fx-padding: 15;");
                        } else {
                            container.setStyle("-fx-background-color: #e7f3ff; -fx-padding: 15;");
                        }
                    });

                    setGraphic(container);
                }
            }
        });

        // Création du conteneur principal avec espacement augmenté
        VBox content = new VBox(15); // Reduced padding
        content.setPadding(new Insets(15)); // Reduced padding
        content.setSpacing(10); // Reduced spacing between elements
        content.getChildren().addAll(headerLabel, filterContainer, accordListView);
        content.setStyle("-fx-background-color: white;");

        // Style du DialogPane avec taille augmentée
        DialogPane dialogPane = popup.getDialogPane();
        dialogPane.setContent(content);
        dialogPane.setPrefWidth(500); // Reduced width
        dialogPane.setPrefHeight(500); // Reduced height
        dialogPane.setMinWidth(400); // Reduced minimum width
        dialogPane.setMinHeight(400); // Reduced minimum height

        // Style du bouton de fermeture
        ButtonType fermerButton = new ButtonType("Fermer", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().add(fermerButton);
        Button closeButton = (Button) dialogPane.lookupButton(fermerButton);
        closeButton.setStyle("-fx-background-color: #007bff; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 10 25; " + // Padding augmenté
                "-fx-font-size: 14px; " + // Taille de police augmentée
                "-fx-background-radius: 5;");

        // Mise à jour de la liste et gestion des notifications
        filterComboBox.setOnAction(e -> {
            String filter = filterComboBox.getValue();
            ObservableList<Accord> filteredAccords = FXCollections.observableArrayList();

            for (Accord accord : recentAccords) {
                boolean isRead = seenAccordIds.contains(accord.getId());
                if (filter.equals("Toutes les notifications") ||
                        (filter.equals("Non lues") && !isRead) ||
                        (filter.equals("Lues") && isRead)) {
                    filteredAccords.add(accord);
                }
            }

            if (filteredAccords.isEmpty()) {
                VBox emptyContainer = new VBox(15);
                emptyContainer.setAlignment(Pos.CENTER);

                ImageView emptyIcon = new ImageView(new Image("file:src/main/resources/icons/empty.png"));
                emptyIcon.setFitHeight(60); // Taille d'icône augmentée
                emptyIcon.setFitWidth(60);

                Label emptyLabel = new Label("Aucune notification dans cette catégorie");
                emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #6c757d;"); // Police augmentée

                emptyContainer.getChildren().addAll(emptyIcon, emptyLabel);
                accordListView.setPlaceholder(emptyContainer);
            }

            accordListView.setItems(filteredAccords);
        });

        // Déclencher le filtre initial
        filterComboBox.fireEvent(new javafx.event.ActionEvent());

        // Gestion de la fermeture
        popup.setResultConverter(dialogButton -> {
            if (dialogButton == fermerButton) {
                for (Accord accord : recentAccords) {
                    if (!seenAccordIds.contains(accord.getId())) {
                        seenAccordIds.add(accord.getId());
                    }
                }
                updateNotificationCount();
            }
            return dialogButton;
        });

        popup.showAndWait();
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

    public void updateNotification() {
        Platform.runLater(() -> {
            if (entreprise != null) {
                // Récupérer les accords récents
                List<Accord> recentAccords = accordService.getRecentAccordsByEntrepriseId(entreprise.getId());
                
                // Compter les nouveaux accords non vus
                int newCount = 0;
                for (Accord accord : recentAccords) {
                    if (!seenAccordIds.contains(accord.getId())) {
                        newCount++;
                    }
                }
                
                // Mettre à jour le compteur seulement s'il y a de nouveaux accords
                if (newCount > 0) {
                    notificationCount = newCount;
                    notificationLabel.setText(String.valueOf(notificationCount));
                    notificationLabel.setVisible(true);
                    
                    ColorAdjust colorAdjust = new ColorAdjust();
                    colorAdjust.setHue(0.7); // teinte rouge
                    colorAdjust.setSaturation(1);
                    notificationIcon.setEffect(colorAdjust);
                    
                    // Jouer un son ou ajouter une animation si souhaité
                    // ...
                }
            }
        });
    }

  /*  @FXML
    private void handleNotificationClick() {
        notificationBadge.setVisible(false); // Masque la notification
        // Rediriger ou mettre en avant les nouveaux accords
    }*/


}
