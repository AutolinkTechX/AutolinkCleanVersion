package org.example.pidev.controllers;
import javafx.geometry.Pos;
import javafx.scene.input.MouseButton;
import org.example.pidev.Enum.StatutEnum;
import org.example.pidev.entities.MaterielRecyclable;
import org.example.pidev.entities.User;
import org.example.pidev.services.ServiceMaterielRecyclable;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ComboBox;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import java.net.URL;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.io.File;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.Tooltip;
import java.util.ResourceBundle;
import javafx.stage.Modality;

public class ShowMaterielRecyclable implements Initializable {

    @FXML
    private HBox navbar;

    @FXML
    private FlowPane materielContainer;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> dateSortComboBox;

    @FXML
    private ComboBox<String> statusFilterComboBox;

    @FXML
    private Button resetButton;

    @FXML
    private Button addButton;
    private double xOffset = 0;
    private double yOffset = 0;
    @FXML
    private Button chatbotButton;

    private boolean isDragging = false;



    private ServiceMaterielRecyclable materielService = new ServiceMaterielRecyclable();
    private List<MaterielRecyclable> allMateriaux;
    private User currentUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (navbar != null) {
            // Vous pouvez ajouter ici des initialisations spécifiques pour la Navbar
        }

        // Initialiser les ComboBox avec leurs valeurs
        initializeDateSortComboBox();
        initializeStatusFilterComboBox();

        // Les matériaux seront chargés une fois que l'utilisateur sera défini
        // via setCurrentUser

        // Configuration des écouteurs d'événements
        resetButton.setOnAction(e -> resetFilters());
        addButton.setOnAction(e -> handleAddMateriel(e));

        // Recherche dynamique en temps réel
        searchField.textProperty().addListener((observable, oldValue, newValue) -> handleSearch());

        // Écouteurs de changement pour les ComboBox
        dateSortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> handleSearch());
        statusFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> handleSearch());
        // Rendre le bouton chatbot déplaçable
        setupDragHandlers();
    }

    private void setupDragHandlers() {
        // Gestion du déplacement
        chatbotButton.setOnMousePressed(event -> {
            if (event.isPrimaryButtonDown()) {
                xOffset = event.getSceneX() - chatbotButton.getLayoutX();
                yOffset = event.getSceneY() - chatbotButton.getLayoutY();
                isDragging = false;
            }
        });

        chatbotButton.setOnMouseDragged(event -> {
            if (event.isPrimaryButtonDown()) {
                isDragging = true;
                double newX = event.getSceneX() - xOffset;
                double newY = event.getSceneY() - yOffset;

                // Limites du parent
                newX = Math.max(0, Math.min(newX, chatbotButton.getParent().getLayoutBounds().getWidth() - chatbotButton.getWidth()));
                newY = Math.max(0, Math.min(newY, chatbotButton.getParent().getLayoutBounds().getHeight() - chatbotButton.getHeight()));

                chatbotButton.setLayoutX(newX);
                chatbotButton.setLayoutY(newY);
            }
        });

        // Gestion du clic
        chatbotButton.setOnMouseReleased(event -> {
            if (!isDragging && event.isPrimaryButtonDown()) {
                openChatbot();
            }
            isDragging = false;
        });
    }





   /* private void makeDraggable(Node node) {
        node.setOnMousePressed(event -> {
            xOffset = event.getSceneX() - node.getLayoutX();
            yOffset = event.getSceneY() - node.getLayoutY();
            event.consume();
        });

        node.setOnMouseDragged(event -> {
            double newX = event.getSceneX() - xOffset;
            double newY = event.getSceneY() - yOffset;

            // Garder dans les limites du parent
            newX = Math.max(0, Math.min(newX, node.getParent().getLayoutBounds().getWidth() - node.getBoundsInParent().getWidth()));
            newY = Math.max(0, Math.min(newY, node.getParent().getLayoutBounds().getHeight() - node.getBoundsInParent().getHeight()));

            node.setLayoutX(newX);
            node.setLayoutY(newY);
            event.consume();
        });
    }*/


    private void initializeDateSortComboBox() {
        dateSortComboBox.setItems(FXCollections.observableArrayList(
                "Tous",
                "Plus récent d'abord",
                "Plus ancien d'abord"
        ));
        dateSortComboBox.getSelectionModel().selectFirst();
    }

    private void initializeStatusFilterComboBox() {
        statusFilterComboBox.setItems(FXCollections.observableArrayList(
                "Tous",
                "En attente",
                "Accepté",
                "Refusé"
        ));
        statusFilterComboBox.getSelectionModel().selectFirst();
    }

    private void handleSearch() {
        String searchText = searchField.getText().toLowerCase().trim();
        String dateSort = dateSortComboBox.getValue();
        String statusFilter = statusFilterComboBox.getValue();

        // Filtrer par texte et statut
        List<MaterielRecyclable> filteredList = allMateriaux.stream()
                .filter(materiel -> {
                    // Filtrage par texte (uniquement par nom)
                    boolean matchesText = searchText.isEmpty() ||
                            materiel.getName().toLowerCase().contains(searchText);

                    // Filtrage par statut
                    boolean matchesStatus = statusFilter.equals("Tous") ||
                            (statusFilter.equals("En attente") && materiel.getStatut() == StatutEnum.en_attente) ||
                            (statusFilter.equals("Accepté") && materiel.getStatut() == StatutEnum.valide) ||
                            (statusFilter.equals("Refusé") && materiel.getStatut() == StatutEnum.refuse);

                    return matchesText && matchesStatus;
                })
                .collect(Collectors.toList());

        // Trier par date
        if (dateSort.equals("Plus récent d'abord")) {
            filteredList.sort(Comparator.comparing(MaterielRecyclable::getDateCreation).reversed());
        } else if (dateSort.equals("Plus ancien d'abord")) {
            filteredList.sort(Comparator.comparing(MaterielRecyclable::getDateCreation));
        }

        displayMateriaux(filteredList);
    }

    private void resetFilters() {
        searchField.clear();
        dateSortComboBox.getSelectionModel().selectFirst();
        statusFilterComboBox.getSelectionModel().selectFirst();
        displayMateriaux(allMateriaux);
    }

    private void displayMateriaux(List<MaterielRecyclable> materiaux) {
        materielContainer.getChildren().clear();

        if (materiaux.isEmpty()) {
            Label noResultsLabel = new Label("Aucun résultat trouvé");
            noResultsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #a05a2c;");
            materielContainer.getChildren().add(noResultsLabel);
        } else {
            for (MaterielRecyclable materiel : materiaux) {
                VBox card = createMaterielCard(materiel);
                materielContainer.getChildren().add(card);
            }
        }
    }

    public void loadMateriaux() {
        try {
            if (currentUser == null) {
                showErrorAlert("Erreur", new Exception("Aucun utilisateur connecté"));
                return;
            }

            // Charger uniquement les matériaux de l'utilisateur connecté
            allMateriaux = materielService.afficherParUtilisateur(currentUser.getId());
            displayMateriaux(allMateriaux);
        } catch (SQLException e) {
            showErrorAlert("Erreur lors du chargement des matériaux", e);
        }
    }

    private VBox createMaterielCard(MaterielRecyclable materiel) {
        // Création de l'imageView
        ImageView imageView = new ImageView();
        imageView.setFitWidth(160);
        imageView.setFitHeight(140);
        imageView.setPreserveRatio(true);

        try {
            String imagePath = materiel.getImage();
            if (imagePath != null && !imagePath.isEmpty()) {
                // Option 1: Chargement d'image statique avec options de cache et de qualité réduites
                Image image = new Image("file:src/main/resources/img/materiels/" + imagePath);
                imageView.setImage(image);
            } else {
                createDefaultImageView(imageView);
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement de l'image : " + e.getMessage());
            createDefaultImageView(imageView);
        }

        // Titre avec style amélioré
        Label titleLabel = new Label(materiel.getName());
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #a05a2c; -fx-font-family: 'Segoe UI';");
        titleLabel.setAlignment(Pos.CENTER);

        // Description avec style amélioré et centrée
        Label descriptionLabel = new Label(materiel.getDescription());
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(250);
        descriptionLabel.setAlignment(Pos.CENTER);
        descriptionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333; -fx-font-family: 'Segoe UI';");

        // Formatage de la date sans le "T"
        String formattedDate = materiel.getDateCreation().toString().replace("T", " ");
        Label dateCreationLabel = new Label("Date : " + formattedDate);
        dateCreationLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333; -fx-font-family: 'Segoe UI';");
        dateCreationLabel.setAlignment(Pos.CENTER);

        // Type de matériel avec style amélioré
        Label typeLabel = new Label("Type : " + materiel.getType_materiel());
        typeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333; -fx-font-family: 'Segoe UI';");
        typeLabel.setAlignment(Pos.CENTER);

        // Entreprise avec style amélioré
        String companyName = "Non spécifiée";
        if (materiel.getEntreprise() != null) {
            companyName = materiel.getEntreprise().getCompanyName();
        }

        Label entrepriseLabel = new Label("Entreprise : " + companyName);
        entrepriseLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333; -fx-font-family: 'Segoe UI';");
        entrepriseLabel.setAlignment(Pos.CENTER);

        // Statut avec style amélioré
        Label statutLabel = new Label("Statut : " + materiel.getStatut());
        statutLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333; -fx-font-family: 'Segoe UI';");
        statutLabel.setAlignment(Pos.CENTER);

        // Boutons avec style amélioré (sans fond marron)
        Button modifierButton = new Button();
        modifierButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #a05a2c; -fx-font-weight: bold;");
        modifierButton.setPrefWidth(100);
        modifierButton.setPrefHeight(30);

        try {
            ImageView modifierIconView = new ImageView(new Image("file:src/main/resources/icons/modif.png"));
            modifierIconView.setFitWidth(30);
            modifierIconView.setFitHeight(30);
            modifierButton.setGraphic(modifierIconView);
            modifierButton.setTooltip(new Tooltip("Modifier"));
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement de l'icône modifier : " + e.getMessage());
            modifierButton.setText("M");
        }
        modifierButton.setOnAction(e -> handleModifier(materiel));

        Button supprimerButton = new Button();
        supprimerButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #a05a2c; -fx-font-weight: bold;");
        supprimerButton.setPrefWidth(100);
        supprimerButton.setPrefHeight(30);

        try {
            ImageView supprimerIconView = new ImageView(new Image("file:src/main/resources/icons/supp2.png"));
            supprimerIconView.setFitWidth(30);
            supprimerIconView.setFitHeight(30);
            supprimerButton.setGraphic(supprimerIconView);
            supprimerButton.setTooltip(new Tooltip("Supprimer"));
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement de l'icône supprimer : " + e.getMessage());
            supprimerButton.setText("S");
        }
        supprimerButton.setOnAction(e -> handleSupprimer(materiel));

        HBox buttonContainer = new HBox(16, modifierButton, supprimerButton);
        buttonContainer.setStyle("-fx-alignment: center;");

        VBox card = new VBox(10, imageView, titleLabel, descriptionLabel, dateCreationLabel, typeLabel,
                entrepriseLabel, statutLabel, buttonContainer);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-padding: 15; -fx-background-color: white; -fx-border-color: #a05a2c; "
                + "-fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 10);");

        card.setPrefWidth(240);
        card.setMinHeight(380);

        return card;
    }

    private void createDefaultImageView(ImageView imageView) {
        // Créer une image par défaut en utilisant uniquement des composants JavaFX
        StackPane placeholder = new StackPane();
        placeholder.setPrefSize(120, 120);

        Rectangle rect = new Rectangle(120, 120);
        rect.setFill(Color.LIGHTGRAY);
        rect.setStroke(Color.DARKGRAY);

        Label iconLabel = new Label("?");
        iconLabel.setStyle("-fx-font-size: 40px; -fx-text-fill: #777777;");

        placeholder.getChildren().addAll(rect, iconLabel);

        // Capturer le contenu de StackPane dans une image
        imageView.setImage(placeholder.snapshot(null, null));
    }

    private void handleModifier(MaterielRecyclable materiel) {
        try {
            // Charger le formulaire de modification
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierMaterielRecyclable.fxml"));
            Parent root = loader.load();

            // Obtenir le contrôleur et passer le matériel à modifier
            ModifierMaterielRecyclable controller = loader.getController();
            controller.setMaterielToEdit(materiel);

            // Créer une nouvelle scène
            Scene scene = new Scene(root);
            Stage stage = new Stage();
            stage.setTitle("Modifier le Matériau");
            stage.setScene(scene);
            stage.showAndWait(); // Attendre que la fenêtre soit fermée

            // Recharger la liste des matériaux après la modification
            loadMateriaux();
        } catch (IOException e) {
            showErrorAlert("Erreur lors de l'ouverture du formulaire de modification", e);
        }
    }

    private void handleSupprimer(MaterielRecyclable materiel) {
        try {
            materielService.supprimer(materiel.getId());
            loadMateriaux(); // Recharger la liste après suppression

            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Suppression");
            alert.setHeaderText(null);
            alert.setContentText("Matériau supprimé avec succès");
            alert.showAndWait();
        } catch (SQLException e) {
            showErrorAlert("Erreur lors de la suppression", e);
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        // Charger les matériaux une fois que l'utilisateur est défini
        loadMateriaux();
    }

    public User getCurrentUser() {
        return this.currentUser;
    }

    private void handleAddMateriel(ActionEvent event) {
        try {
            // Vérifier si l'utilisateur est connecté
            if (currentUser == null) {
                showErrorAlert("Erreur", new Exception("Aucun utilisateur connecté"));
                return;
            }

            // Charger le formulaire d'ajout avec le chemin absolu
            URL fxmlUrl = getClass().getResource("/AjouterMaterielRecyclable.fxml");
            if (fxmlUrl == null) {
                throw new IOException("Impossible de trouver le fichier FXML");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            // Créer une nouvelle scène
            Scene scene = new Scene(root);
            Stage stage = new Stage();
            stage.setTitle("Ajouter un Matériel Recyclable");

            // Get the controller and set the reference
            AjouterMaterielRecyclable controller = loader.getController();
            controller.setShowMaterielRecyclableController(this);

            // Configuration de la fenêtre modale
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(scene);

            // Afficher la nouvelle fenêtre et attendre qu'elle soit fermée
            stage.showAndWait();

            // Recharger la liste des matériaux après l'ajout
            loadMateriaux();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur de chargement FXML: " + e.getMessage());
            System.err.println("Chemin de la ressource: " + getClass().getResource("/AjouterMaterielRecyclable.fxml"));
            showErrorAlert("Erreur lors de l'ouverture du formulaire d'ajout", e);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur générale: " + e.getMessage());
            showErrorAlert("Erreur inattendue", e);
        }
    }

    private void showErrorAlert(String message, Exception e) {
        e.printStackTrace();
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message + " : " + e.getMessage());
        alert.showAndWait();
    }







    private ClientDashboardController dashboardController;

    public void setDashboardController(ClientDashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }


    @FXML
    private void openChatbot() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ChatbotWindow.fxml"));
            Parent root = loader.load();
            // Passer l'utilisateur actuel si nécessaire
            // ChatbotController controller = loader.getController();
            //  controller.setCurrentUser(this.currentUser);

            Stage stage = new Stage();
            stage.setTitle("Assistant Recyclage");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}




