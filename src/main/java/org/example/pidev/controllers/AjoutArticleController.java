package org.example.pidev.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.example.pidev.entities.Article;
import org.example.pidev.services.ArticleService;
import org.example.pidev.utils.MyDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AjoutArticleController {

    @FXML private GridPane cardsContainer;
    @FXML private HBox paginationContainer;

    private final ArticleService articleService = new ArticleService();
    private List<Article> allArticles;
    private int currentPage = 1;
    private final int itemsPerPage = 6;
    private List<StackPane> pageIndicators = new ArrayList<>();

    private static final double CARD_WIDTH = 240;
    private static final double CARD_HEIGHT = 320;

    @FXML
    private void initialize() {
        refreshCards();
    }

    private void refreshCards() {
        try {
            allArticles = articleService.getAllArticle();
            currentPage = 1;
            displayPage(currentPage);
            setupPagination();
        } catch (Exception e) {
            showAlert("Error", "Error while loading products: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void displayPage(int page) {
        cardsContainer.getChildren().clear();
        cardsContainer.setHgap(20);
        cardsContainer.setVgap(20);
        cardsContainer.setPadding(new Insets(20));
        cardsContainer.setAlignment(Pos.CENTER);

        int fromIndex = (page - 1) * itemsPerPage;
        int toIndex = Math.min(fromIndex + itemsPerPage, allArticles.size());

        if (fromIndex >= allArticles.size() || allArticles.isEmpty()) {
            showEmptyState();
            return;
        }

        List<Article> pageArticles = allArticles.subList(fromIndex, toIndex);

        int row = 0;
        int col = 0;
        for (Article article : pageArticles) {
            VBox card = createProductCard(article);
            cardsContainer.add(card, col, row);

            col++;
            if (col >= 3) {
                col = 0;
                row++;
            }
        }

        updatePageIndicators(page);
    }

    private VBox createProductCard(Article article) {
        VBox card = new VBox();
        card.getStyleClass().add("product-card");
        card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        card.setAlignment(Pos.TOP_CENTER);
        card.setSpacing(10);
        card.setPadding(new Insets(10));

        // Image part
        ImageView imageView = createProductImageView(article);
        StackPane imageContainer = new StackPane(imageView);
        imageContainer.getStyleClass().add("image-container");
        imageContainer.setAlignment(Pos.CENTER);

        // Name part
        Label nameLabel = new Label(article.getNom());
        nameLabel.getStyleClass().add("product-name");
        nameLabel.setMaxWidth(CARD_WIDTH - 20);
        nameLabel.setWrapText(true);

        // Price part
        Label priceLabel = new Label(String.format("%.2f DT", article.getPrix()));
        priceLabel.getStyleClass().add("product-price");

        // Buttons part
        HBox buttonBox = createActionButtons(article);

        card.getChildren().addAll(imageContainer, nameLabel, priceLabel, buttonBox);
        return card;
    }

    private ImageView createProductImageView(Article article) {
        ImageView imageView = new ImageView();
        try {
            if (article.getImage() != null && !article.getImage().trim().isEmpty()) {
                try {
                    Image image = new Image(article.getImage());
                    if (!image.isError()) {
                        imageView.setImage(image);
                    } else {
                        loadDefaultImage(imageView);
                    }
                } catch (IllegalArgumentException e) {
                    loadDefaultImage(imageView);
                }
            } else {
                loadDefaultImage(imageView);
            }
        } catch (Exception e) {
            System.err.println("Error loading product image: " + e.getMessage());
            loadDefaultImage(imageView);
        }

        imageView.setFitWidth(200);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        return imageView;
    }

    private void loadDefaultImage(ImageView imageView) {
        try (InputStream defaultImageStream = getClass().getResourceAsStream("/images/logo.jpg")) {
            if (defaultImageStream != null) {
                imageView.setImage(new Image(defaultImageStream));
            } else {
                System.err.println("Default product image not found in resources");
                // Set a placeholder or leave empty
            }
        } catch (IOException e) {
            System.err.println("Error loading default image: " + e.getMessage());
        }
    }

    private HBox createActionButtons(Article article) {
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button detailsBtn = new Button("Details");
        detailsBtn.getStyleClass().addAll("action-button", "details-button");
        detailsBtn.setOnAction(e -> showDetails(article));

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().addAll("action-button", "edit-button");
        editBtn.setOnAction(e -> showEditForm(article));

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().addAll("action-button", "delete-button");
        deleteBtn.setOnAction(e -> handleDeleteProduct(article));

        buttonBox.getChildren().addAll(detailsBtn, editBtn, deleteBtn);
        return buttonBox;
    }
    private void showDetails(Article article) {
        try {
            // Try with leading slash
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ArticleDetails.fxml"));
            Parent root = loader.load();

            ArticleDetailsController controller = loader.getController();
            controller.setArticle(article);

            Stage stage = new Stage();
            stage.setTitle("Product Details");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Error loading FXML file:");
            e.printStackTrace();

            // Show more detailed error message
            showAlert("Error",
                    "Could not load details view.\n" +
                            "File path: " + getClass().getResource("/ArticleDetails.fxml") + "\n" +
                            "Error: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    private void showEditForm(Article article) {
        showProductForm(article);
    }

    @FXML
    private void handleAddProduct() {
        showProductForm(null);
    }

    private void showProductForm(Article article) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ProductForm.fxml"));
            Parent root = loader.load();

            ProductForm formController = loader.getController();
            formController.setArticle(article);
            formController.setParentController(this);

            Stage stage = new Stage();
            stage.setTitle(article == null ? "Add New Product" : "Edit Product");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert("Error", "Could not load product form: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }
/*
    private void handleDeleteProduct(Article article) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Are you sure you want to delete this product and all its favorites?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Démarrer une transaction
                Connection conn = MyDatabase.getInstance().getConnection();
                conn.setAutoCommit(false);

                try {
                    // 1. Supprimer d'abord les favoris associés
                    String deleteFavoritesSQL = "DELETE FROM favorie WHERE article_id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(deleteFavoritesSQL)) {
                        stmt.setInt(1, article.getId());
                        stmt.executeUpdate();
                    }

                    // 2. Puis supprimer l'article
                    articleService.delete(article.getId());

                    // Valider la transaction
                    conn.commit();

                    showSuccessMessage("Product and associated favorites deleted successfully!");
                    refreshCards();
                } catch (SQLException e) {
                    // Annuler en cas d'erreur
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                showAlert("Database Error",
                        "Could not delete product: " + e.getMessage(),
                        Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        }
    }
*/

    private void handleDeleteProduct(Article article) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Are you sure you want to delete this product and all its favorites?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = MyDatabase.getInstance().getConnection()) {
                conn.setAutoCommit(false); // Start transaction

                try {
                    // 1. Delete favorites first
                    String deleteFavoritesSQL = "DELETE FROM favorie WHERE article_id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(deleteFavoritesSQL)) {
                        stmt.setInt(1, article.getId());
                        stmt.executeUpdate();
                    }

                    // 2. Delete article
                    String deleteArticleSQL = "DELETE FROM article WHERE id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(deleteArticleSQL)) {
                        stmt.setInt(1, article.getId());
                        stmt.executeUpdate();
                    }

                    conn.commit(); // Commit transaction
                    showSuccessMessage("Product and associated favorites deleted successfully!");
                    refreshCards();

                } catch (SQLException e) {
                    conn.rollback(); // Rollback on error
                    throw e;
                }
            } catch (SQLException e) {
                showAlert("Database Error",
                        "Could not delete product: " + e.getMessage(),
                        Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        }
    }

    private void setupPagination() {
        paginationContainer.getChildren().clear();
        pageIndicators.clear();

        int totalPages = getTotalPages();
        if (totalPages <= 1) return;

        // Previous button
        Button prevBtn = new Button("<");
        prevBtn.getStyleClass().add("pagination-button");
        prevBtn.setDisable(currentPage == 1);
        prevBtn.setOnAction(e -> navigateToPage(currentPage - 1));

        // Page indicators
        HBox pageNumbersBox = new HBox(5);
        pageNumbersBox.setAlignment(Pos.CENTER);

        // Determine range of pages to show (e.g., 5 pages around current)
        int startPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(totalPages, currentPage + 2);

        // Always show first page
        if (startPage > 1) {
            StackPane firstPage = createPageIndicator(1);
            firstPage.setOnMouseClicked(e -> navigateToPage(1));
            pageNumbersBox.getChildren().add(firstPage);
            if (startPage > 2) {
                pageNumbersBox.getChildren().add(new Label("..."));
            }
        }

        // Show pages in range
        for (int i = startPage; i <= endPage; i++) {
            StackPane pageIndicator = createPageIndicator(i);
            final int pageNumber = i;
            pageIndicator.setOnMouseClicked(e -> navigateToPage(pageNumber));
            pageNumbersBox.getChildren().add(pageIndicator);
            pageIndicators.add(pageIndicator);
        }

        // Always show last page
        if (endPage < totalPages) {
            if (endPage < totalPages - 1) {
                pageNumbersBox.getChildren().add(new Label("..."));
            }
            StackPane lastPage = createPageIndicator(totalPages);
            lastPage.setOnMouseClicked(e -> navigateToPage(totalPages));
            pageNumbersBox.getChildren().add(lastPage);
        }

        // Next button
        Button nextBtn = new Button(">");
        nextBtn.getStyleClass().add("pagination-button");
        nextBtn.setDisable(currentPage == totalPages);
        nextBtn.setOnAction(e -> navigateToPage(currentPage + 1));

        paginationContainer.getChildren().addAll(prevBtn, pageNumbersBox, nextBtn);
        updatePageIndicators(currentPage);
    }

    private StackPane createPageIndicator(int pageNumber) {
        Circle circle = new Circle(10);
        circle.setFill(pageNumber == currentPage ? Color.web("#4a90e2") : Color.web("#e0e0e0"));

        Text text = new Text(String.valueOf(pageNumber));
        text.setFill(pageNumber == currentPage ? Color.WHITE : Color.BLACK);

        StackPane stack = new StackPane(circle, text);
        stack.getStyleClass().add("page-indicator");
        return stack;
    }

    private void updatePageIndicators(int activePage) {
        for (StackPane stack : pageIndicators) {
            Circle circle = (Circle) stack.getChildren().get(0);
            Text text = (Text) stack.getChildren().get(1);

            int pageNumber = Integer.parseInt(text.getText());
            boolean isActive = pageNumber == activePage;
            circle.setFill(isActive ? Color.web("#4a90e2") : Color.web("#e0e0e0"));
            text.setFill(isActive ? Color.WHITE : Color.BLACK);
        }
    }

    private void navigateToPage(int page) {
        if (page < 1 || page > getTotalPages()) return;
        currentPage = page;
        displayPage(page);
        setupPagination(); // Rebuild pagination controls
    }

    private int getTotalPages() {
        return (int) Math.ceil((double) allArticles.size() / itemsPerPage);
    }

    private void showEmptyState() {
        cardsContainer.getChildren().clear();
        Label emptyLabel = new Label("No products available");
        emptyLabel.getStyleClass().add("empty-label");
        cardsContainer.add(emptyLabel, 0, 0, 3, 1);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void showSuccessMessage(String message) {
        showAlert("Success", message, Alert.AlertType.INFORMATION);
        refreshCards();
    }



}