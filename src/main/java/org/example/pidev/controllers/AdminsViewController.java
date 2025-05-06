package org.example.pidev.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.example.pidev.entities.User;
import org.example.pidev.services.UserService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AdminsViewController implements Initializable {

    @FXML
    private FlowPane adminsContainer;

    @FXML
    private ImageView addAdminButton;

    @FXML
    private TextField searchField;

    @FXML
    private HBox paginationContainer;

    private final UserService userService = new UserService();
    private ObservableList<User> fullAdminsList = FXCollections.observableArrayList();

    private int currentPage = 0;
    private final int itemsPerPage = 6;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadAdminsData();

        if (addAdminButton != null) {
            addAdminButton.setOnMouseClicked(event -> openAddAdminWindow());
        }

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterAdmins(newValue.trim().toLowerCase());
        });
    }

    private void loadAdminsData() {
        try {
            fullAdminsList = FXCollections.observableArrayList(userService.getAllAdmins());

            if (fullAdminsList.isEmpty()) {
                adminsContainer.getChildren().clear();
                Label noAdminsLabel = new Label("No admins found in the database");
                noAdminsLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 16px;");
                adminsContainer.getChildren().add(noAdminsLabel);
                paginationContainer.getChildren().clear();
                return;
            }

            currentPage = 0;
            updatePaginatedAdmins();

        } catch (Exception e) {
            e.printStackTrace();
            Label errorLabel = new Label("Error loading admin data: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");
            adminsContainer.getChildren().add(errorLabel);
            paginationContainer.getChildren().clear();
        }
    }

    private void updatePaginatedAdmins() {
        adminsContainer.getChildren().clear();
        paginationContainer.getChildren().clear();

        int fromIndex = currentPage * itemsPerPage;
        int toIndex = Math.min(fromIndex + itemsPerPage, fullAdminsList.size());

        ObservableList<User> pageList = FXCollections.observableArrayList(fullAdminsList.subList(fromIndex, toIndex));
        createAdminCards(pageList);

        addPaginationControls();
    }

    private void addPaginationControls() {
        int totalItems = fullAdminsList.size();
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);

        if (totalPages <= 1) {
            return; // Don't show pagination if there's only one page
        }

        HBox paginationControls = new HBox(5);
        paginationControls.setAlignment(Pos.CENTER);
        paginationControls.setPadding(new Insets(5));

        Button prevButton = new Button("Previous");
        prevButton.setDisable(currentPage == 0);
        prevButton.setOnAction(e -> {
            currentPage--;
            updatePaginatedAdmins();
        });

        paginationControls.getChildren().add(prevButton);

        int maxPageButtons = 7;
        int startPage = Math.max(0, currentPage - 3);
        int endPage = Math.min(totalPages, currentPage + 4);

        if (startPage > 0) {
            addPageButton(paginationControls, 0);
            if (startPage > 1) {
                paginationControls.getChildren().add(new Label("..."));
            }
        }

        for (int i = startPage; i < endPage; i++) {
            addPageButton(paginationControls, i);
        }

        if (endPage < totalPages) {
            if (endPage < totalPages - 1) {
                paginationControls.getChildren().add(new Label("..."));
            }
            addPageButton(paginationControls, totalPages - 1);
        }

        Button nextButton = new Button("Next");
        nextButton.setDisable(currentPage >= totalPages - 1);
        nextButton.setOnAction(e -> {
            currentPage++;
            updatePaginatedAdmins();
        });

        paginationControls.getChildren().add(nextButton);
        paginationContainer.getChildren().add(paginationControls);
    }

    private void addPageButton(HBox container, int pageIndex) {
        Button pageButton = new Button(String.valueOf(pageIndex + 1));
        pageButton.setStyle(pageIndex == currentPage
                ? "-fx-background-color: #2a9d8f; -fx-text-fill: white; -fx-font-weight: bold;"
                : "-fx-background-color: lightgray; -fx-text-fill: black;");
        pageButton.setOnAction(e -> {
            currentPage = pageIndex;
            updatePaginatedAdmins();
        });
        container.getChildren().add(pageButton);
    }

    private void createAdminCards(ObservableList<User> list) {
        if (list.isEmpty()) {
            Label noResults = new Label("No matching admins found.");
            noResults.setStyle("-fx-text-fill: gray; -fx-font-size: 16px;");
            adminsContainer.getChildren().add(noResults);
            return;
        }

        for (User admin : list) {
            if (admin == null) continue;

            VBox card = new VBox(10);
            card.setStyle("-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");
            card.setPadding(new Insets(15));
            card.setPrefWidth(300);
            card.setPrefHeight(250);
            card.setUserData(admin);
            card.setAlignment(Pos.CENTER);

            VBox imageContainer = new VBox();
            imageContainer.setAlignment(Pos.CENTER);

            ImageView userImageView = new ImageView();
            userImageView.setFitWidth(100);
            userImageView.setFitHeight(100);
            userImageView.setPreserveRatio(true);

            try {
                String image_path = admin.getImage_path();
                if (image_path != null && !image_path.isEmpty()) {
                    Image image = new Image("file:" + image_path);
                    userImageView.setImage(image);
                } else {
                    Image defaultImage = new Image(getClass().getResourceAsStream("/images/logo.jpg"));
                    userImageView.setImage(defaultImage);
                }
            } catch (Exception e) {
                Image defaultImage = new Image(getClass().getResourceAsStream("/images/logo.jpg"));
                userImageView.setImage(defaultImage);
            }

            imageContainer.getChildren().add(userImageView);

            Label nameLabel = new Label(admin.getName() + " " + admin.getLastName());
            nameLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
            nameLabel.setAlignment(Pos.CENTER);

            Label emailLabel = new Label(admin.getEmail());
            emailLabel.setFont(Font.font("System", 14));
            emailLabel.setAlignment(Pos.CENTER);

            Label phoneLabel = new Label("Phone: " + admin.getPhone());
            phoneLabel.setFont(Font.font("System", 14));
            phoneLabel.setTextFill(Color.GRAY);
            phoneLabel.setAlignment(Pos.CENTER);

            HBox buttonContainer = new HBox(10);
            buttonContainer.setPadding(new Insets(10, 0, 0, 0));
            buttonContainer.setAlignment(Pos.CENTER);

            Button modifyButton = new Button("Modify");
            modifyButton.setStyle("-fx-background-color: rgb(202,138,98); -fx-text-fill: white; -fx-background-radius: 5px;");
            modifyButton.setOnAction(e -> handleModifyAdmin(admin));

            Button deleteButton = new Button("Delete");
            deleteButton.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white; -fx-background-radius: 5px;");
            deleteButton.setOnAction(e -> handleDeleteAdmin(card));

            buttonContainer.getChildren().addAll(modifyButton, deleteButton);

            card.getChildren().addAll(imageContainer, nameLabel, emailLabel, phoneLabel, buttonContainer);
            adminsContainer.getChildren().add(card);
        }
    }

    @FXML
    private void handleModifyAdmin(User admin) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifyAdminView.fxml"));
            Parent root = loader.load();

            ModifyAdminViewController controller = loader.getController();
            Stage stage = new Stage();
            controller.setStage(stage);
            controller.setAdminToModify(admin);

            stage.setTitle("Modify Admin");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadAdminsData();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open modify window");
            alert.setContentText("An error occurred while opening the modify window: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void handleDeleteAdmin(VBox card) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Admin");
        alert.setContentText("Are you sure you want to delete this admin?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    User admin = (User) card.getUserData();
                    if (admin != null) {
                        userService.supprimer(admin.getId());
                        loadAdminsData();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Error");
                    errorAlert.setHeaderText("Failed to delete admin");
                    errorAlert.setContentText("An error occurred while deleting the admin. Please try again.");
                    errorAlert.showAndWait();
                }
            }
        });
    }

    private void openAddAdminWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AddAdminView.fxml"));
            Parent root = loader.load();
            AddAdminViewController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Add New Admin");
            stage.setScene(new Scene(root));
            stage.setResizable(false);

            controller.setStage(stage);
            stage.showAndWait();

            loadAdminsData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void filterAdmins(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            try {
                fullAdminsList = FXCollections.observableArrayList(userService.getAllAdmins());
            } catch (SQLException e) {
                System.out.println("Error extracting admins data list :" + e.getMessage());
                e.printStackTrace();
            }
            currentPage = 0;
            updatePaginatedAdmins();
            return;
        }

        ObservableList<User> filteredList = FXCollections.observableArrayList();

        for (User admin : fullAdminsList) {
            if (admin != null) {
                String fullName = (admin.getName() + " " + admin.getLastName()).toLowerCase();
                if (fullName.contains(keyword) ||
                        admin.getEmail().toLowerCase().contains(keyword) ||
                        String.valueOf(admin.getPhone()).contains(keyword)) {
                    filteredList.add(admin);
                }
            }
        }

        fullAdminsList = filteredList;
        currentPage = 0;
        updatePaginatedAdmins();
    }
}