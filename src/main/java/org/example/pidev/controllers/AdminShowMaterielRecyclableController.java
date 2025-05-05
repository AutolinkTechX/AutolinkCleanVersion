package org.example.pidev.controllers;


import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.example.pidev.Enum.StatutEnum;
import org.example.pidev.entities.MaterielRecyclable;
import org.example.pidev.services.ServiceMaterielRecyclable;

import java.net.URL;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminShowMaterielRecyclableController implements Initializable {

    @FXML
    private Pagination pagination;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> dateSortComboBox;
    @FXML
    private ComboBox<String> statusFilterComboBox;
    @FXML
    private Button resetButton;

    private ServiceMaterielRecyclable materielService = new ServiceMaterielRecyclable();
    private List<MaterielRecyclable> allMateriaux;
    private final int ITEMS_PER_PAGE = 4;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initializeDateSortComboBox();
        initializeStatusFilterComboBox();

        resetButton.setOnAction(e -> resetFilters());

        searchField.textProperty().addListener((observable, oldValue, newValue) -> handleSearch());
        dateSortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> handleSearch());
        statusFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> handleSearch());

        loadAllMateriaux();
    }

    private void loadAllMateriaux() {
        try {
            allMateriaux = materielService.afficher();
            handleSearch();
        } catch (SQLException e) {
            showErrorAlert("Erreur lors du chargement des matériaux", e);
        }
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

        List<MaterielRecyclable> filteredList = allMateriaux.stream()
                .filter(materiel -> {
                    boolean matchesText = searchText.isEmpty() ||
                            materiel.getName().toLowerCase().contains(searchText);

                    boolean matchesStatus;
                    switch (statusFilter) {
                        case "En attente":
                            matchesStatus = materiel.getStatut() == StatutEnum.en_attente;
                            break;
                        case "Accepté":
                            matchesStatus = materiel.getStatut() == StatutEnum.valide;
                            break;
                        case "Refusé":
                            matchesStatus = materiel.getStatut() == StatutEnum.refuse;
                            break;
                        default: // "Tous"
                            matchesStatus = true;
                    }

                    return matchesText && matchesStatus;
                })
                .collect(Collectors.toList());

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
        handleSearch();
    }

    private void displayMateriaux(List<MaterielRecyclable> materiaux) {
        int pageCount = (int) Math.ceil((double) materiaux.size() / ITEMS_PER_PAGE);
        pageCount = pageCount == 0 ? 1 : pageCount; // Minimum 1 page

        pagination.setPageCount(pageCount);
        pagination.setStyle("-fx-page-information-visible: false;");
        pagination.setPageFactory(pageIndex -> {
            int fromIndex = pageIndex * ITEMS_PER_PAGE;
            int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, materiaux.size());
            List<MaterielRecyclable> subList = materiaux.subList(fromIndex, toIndex);

            FlowPane pageFlowPane = new FlowPane();
            pageFlowPane.setHgap(20);
            pageFlowPane.setVgap(15);
            pageFlowPane.setAlignment(Pos.CENTER);
            pageFlowPane.setMaxWidth(Double.MAX_VALUE);
            pageFlowPane.setPadding(new Insets(15, 20, 15, 20));

            if (subList.isEmpty()) {
                Label noResultsLabel = new Label("Aucun résultat trouvé");
                noResultsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #a05a2c;");
                pageFlowPane.getChildren().add(noResultsLabel);
            } else {
                for (MaterielRecyclable materiel : subList) {
                    VBox card = createAdminMaterielCard(materiel);
                    pageFlowPane.getChildren().add(card);
                }
            }

            return pageFlowPane;
        });
    }

    private VBox  createAdminMaterielCard(MaterielRecyclable materiel) {
        // Création de l'imageView
        ImageView imageView = new ImageView();
        imageView.setFitWidth(160);
        imageView.setFitHeight(140);
        imageView.setPreserveRatio(true);

        try {
            String imagePath = materiel.getImage();
            if (imagePath != null && !imagePath.isEmpty()) {
                Image image = new Image("file:src/main/resources/img/materiels/" + imagePath);
                imageView.setImage(image);
            } else {
                createDefaultImageView(imageView);
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement de l'image : " + e.getMessage());
            createDefaultImageView(imageView);
        }

        // Titre
        Label titleLabel = new Label(materiel.getName());
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #a05a2c; -fx-font-family: 'Segoe UI';");
        titleLabel.setAlignment(Pos.CENTER);

        // Description
        Label descriptionLabel = new Label(materiel.getDescription());
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(250);
        descriptionLabel.setAlignment(Pos.CENTER);
        descriptionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333; -fx-font-family: 'Segoe UI';");

        // Date formatée
        String formattedDate = materiel.getDateCreation().toString().replace("T", " ");
        Label dateCreationLabel = new Label("Date : " + formattedDate);
        dateCreationLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333; -fx-font-family: 'Segoe UI';");
        dateCreationLabel.setAlignment(Pos.CENTER);

        // Type
        Label typeLabel = new Label("Type : " + materiel.getType_materiel());
        typeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333; -fx-font-family: 'Segoe UI';");
        typeLabel.setAlignment(Pos.CENTER);

        // Entreprise
        String companyName = "Non spécifiée";
        if (materiel.getEntreprise() != null) {
            companyName = materiel.getEntreprise().getCompanyName();
        }
        Label entrepriseLabel = new Label("Entreprise : " + companyName);
        entrepriseLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333; -fx-font-family: 'Segoe UI';");
        entrepriseLabel.setAlignment(Pos.CENTER);

        // Statut
        Label statutLabel = new Label("Statut : " + materiel.getStatut());
        statutLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333; -fx-font-family: 'Segoe UI';");
        statutLabel.setAlignment(Pos.CENTER);

        // Construction finale sans boutons
        VBox card = new VBox(10, imageView, titleLabel, descriptionLabel, dateCreationLabel, typeLabel,
                entrepriseLabel, statutLabel);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-padding: 15; -fx-background-color: white; -fx-border-color: #a05a2c; "
                + "-fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 10);");

        card.setPrefWidth(240);
        card.setMinHeight(340); // ajusté en fonction du retrait des boutons

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

    private void showErrorAlert(String title, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }
}