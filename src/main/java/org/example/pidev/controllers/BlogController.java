package org.example.pidev.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.pidev.entities.Blog;
import org.example.pidev.entities.Comment;
import org.example.pidev.services.ServiceBlog;
import org.example.pidev.services.ServiceComment;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

public class BlogController {

    @FXML private FlowPane cardContainer;
    @FXML private Button addBlogButton;

    private final ServiceBlog serviceBlog = new ServiceBlog();

    @FXML
    public void initialize() {
        loadData();
    }

    private void loadData() {
        try {
            ObservableList<Blog> blogs = FXCollections.observableArrayList(serviceBlog.afficher());
            displayBlogsAsCards(blogs);
        } catch (SQLException e) {
            showAlert("Erreur", "Échec du chargement des blogs : " + e.getMessage());
        }
    }

    private void displayBlogsAsCards(ObservableList<Blog> blogs) {
        cardContainer.getChildren().clear();
        cardContainer.setHgap(20);
        cardContainer.setVgap(20);
        cardContainer.setPadding(new Insets(15));

        for (Blog blog : blogs) {
            VBox card = createBlogCard(blog);
            cardContainer.getChildren().add(card);
        }
    }

    private VBox createBlogCard(Blog blog) {
        VBox box = new VBox();
        box.setSpacing(10);
        box.setPadding(new Insets(10));
        box.setStyle("""
            -fx-background-color: white;
            -fx-border-radius: 8;
            -fx-background-radius: 8;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 4, 0.2, 0, 2);
        """);

        // Image
        // Partie Image
        ImageView imageView = new ImageView();
        try {
            Image image;
            if (blog.getImage() != null && !blog.getImage().isEmpty()) {
                // Handle file:/ protocol paths correctly - don't add another file:///
                if (blog.getImage().startsWith("file:/")) {
                    image = new Image(blog.getImage());
                } else if (blog.getImage().matches("^[a-zA-Z]:\\\\.*")) {
                    // For Windows paths without file:/ prefix
                    image = new Image("file:///" + blog.getImage().replace("\\", "/"));
                } else {
                    // For resource paths
                    InputStream resourceStream = getClass().getResourceAsStream("/" + blog.getImage());
                    image = (resourceStream != null) ? new Image(resourceStream)
                            : new Image(getClass().getResourceAsStream("/images/logo.jpg"));
                }
            } else {
                // Default image
                image = new Image(getClass().getResourceAsStream("/images/logo.jpg"));
            }
            imageView.setImage(image);

        } catch (Exception ex) {
            try {
                imageView.setImage(new Image(getClass().getResourceAsStream("/images/logo.jpg")));
            } catch (Exception e) {
            }
        }

        imageView.setFitWidth(250);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(true);



// Conteneur
        VBox imageContainer = new VBox(imageView);
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.setStyle("-fx-padding: 10; -fx-background-color: #f9f9f9;");

        // Titre + bouton édition + suppression
        HBox titleRow = new HBox(10);
        titleRow.setStyle("-fx-alignment: center-left;");

        Label title = new Label(blog.getTitle());
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Button editButton = new Button("🖉");
        editButton.setOnAction(e -> openModifyBlogWindow(blog));
        editButton.setStyle("-fx-font-size: 14px; -fx-background-color: transparent; -fx-cursor: hand;");

        Button test = new Button("Comments");
        test.setOnAction(e -> openCommentWindow(blog));
        test.setStyle("-fx-font-size: 14px; -fx-background-color: transparent; -fx-cursor: hand;");

        Button deleteButton = new Button("🗑️");
        deleteButton.setOnAction(e -> {
            if (confirmDelete()) deleteBlog(blog);
        });
        deleteButton.setStyle("-fx-font-size: 14px; -fx-background-color: transparent; -fx-cursor: hand;");

        titleRow.getChildren().addAll(title, editButton, deleteButton, test);

        // Date
        Label date = new Label(blog.getPublishedDate().toString());
        date.setStyle("-fx-text-fill: #6c757d;");

        // Stats
        HBox stats = new HBox(10);
        stats.getChildren().addAll(
                new Label("♥ " + blog.getLikes()),
                new Label("👎 " + blog.getDislikes()),
                new Label("📝 " + truncateContent(blog.getContent(), 50))
        );
        stats.setStyle("-fx-font-size: 13px;");

        box.getChildren().addAll(imageView, titleRow, date, stats);
        return box;
    }

    private boolean confirmDelete() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer ce blog ?");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer ce blog ?");
        return alert.showAndWait().filter(response -> response == ButtonType.OK).isPresent();
    }

    private void deleteBlog(Blog blog) {
        try {
            serviceBlog.supprimer(blog.getId());
            loadData();
            showAlert("Succès", "Blog supprimé avec succès !");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Échec de la suppression : " + e.getMessage());
        }
    }

    private Image loadSafeImage(String path) {
        try {
            java.net.URL imageUrl = getClass().getResource(path);
            if (imageUrl != null) {
                return new Image(imageUrl.toExternalForm());
            } else {
                System.err.println("⚠ Image introuvable : " + path);
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement image : " + e.getMessage());
        }
        return new Image("https://via.placeholder.com/280x160?text=No+Image");
    }

    private String truncateContent(String content, int maxLength) {
        return (content == null || content.trim().isEmpty()) ? "" :
                (content.length() > maxLength) ? content.substring(0, maxLength) + "..." : content;
    }

    private void openAddCommentWindow(Blog blog) {
        loadFXMLWindow("/AddComment.fxml", "Ajouter un commentaire", loader -> {
            AddCommentController controller = loader.getController();
            controller.setBlog(blog);
            controller.setOnCommentAdded(() -> showAlert("Succès", "Commentaire ajouté avec succès !"));
        });
    }

    private void openModifyBlogWindow(Blog blog) {
        loadFXMLWindow("/edit_blog.fxml", "Modifier Blog", loader -> {
            EditBlogController controller = loader.getController();
            controller.setBlog(blog);
            controller.setOnUpdateCallback(this::loadData);
        });
    }

    private void openBlogDetailsView(Blog blog) {
        loadFXMLWindow("/BlogDetailsView.fxml", "Détails du Blog", loader -> {
            BlogDetailsController controller = loader.getController();
            controller.setBlog(blog);
        });
    }

    @FXML
    private void handleAddBlogButton() {
        loadFXMLWindow("/AddBlog.fxml", "Ajouter Blog", loader -> {
            AddBlogController controller = loader.getController();
            controller.setOnAddCallback(this::loadData);
        });
    }

    private void loadFXMLWindow(String fxmlPath, String title,
                                Consumer<FXMLLoader> controllerSetup) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            controllerSetup.accept(loader);

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible de charger " + fxmlPath);
            e.printStackTrace();
        }
    }

    private void openCommentWindow(Blog blog) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdminCommentView.fxml"));
            Parent commentView = loader.load();

            AdminCommentController controller = loader.getController();
            controller.setBlog(blog);

            Stage stage = new Stage();
            stage.setTitle("Comments: " + blog.getTitle());
            stage.setScene(new Scene(commentView));
            stage.show();
        } catch (Exception e) {
            showAlert("Error", "Failed to open comment window");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
