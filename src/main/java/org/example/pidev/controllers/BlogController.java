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
import org.example.pidev.services.ServiceBlog;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.function.Consumer;

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
            String imagePath = blog.getImage(); // ou blog.getImage() si c'est une instance
            System.out.println("Chemin de l'image: " + imagePath); // Debug

            if (imagePath != null && !imagePath.isEmpty()) {
                if (imagePath.startsWith("http")) {
                    // Image en ligne
                    imageView.setImage(new Image(imagePath, true));
                } else {
                    // Image locale (dans les ressources)
                    InputStream imageStream = getClass().getResourceAsStream(imagePath.startsWith("/") ? imagePath : "/" + imagePath);
                    if (imageStream != null) {
                        imageView.setImage(new Image(imageStream)); // Image depuis les ressources
                    } else {
                        System.out.println("Image introuvable, chargement de l'image par défaut.");
                        imageStream = getClass().getResourceAsStream("/images/logo.jpg");
                        if (imageStream != null) {
                            imageView.setImage(new Image(imageStream));
                        } else {
                            System.out.println("Image par défaut introuvable.");
                        }
                    }
                }
            } else {
                // Si le chemin est null ou vide, charge l'image par défaut
                InputStream imageStream = getClass().getResourceAsStream("/images/logo.jpg");
                if (imageStream != null) {
                    imageView.setImage(new Image(imageStream));
                } else {
                    System.out.println("Image par défaut introuvable.");
                }
            }
        } catch (Exception e) {
            System.out.println("Erreur lors du chargement de l'image : " + e.getMessage());
            InputStream imageStream = getClass().getResourceAsStream("/images/logo.jpg");
            if (imageStream != null) {
                imageView.setImage(new Image(imageStream));
            } else {
                System.out.println("Image par défaut introuvable.");
            }
        }

// Paramètres d'affichage
        imageView.setFitWidth(200);
        imageView.setFitHeight(200);
        imageView.setPreserveRatio(true); // Vous pouvez changer cette ligne en fonction de votre préférence
        imageView.setSmooth(true);
        imageView.setCache(true);


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

        Button test = new Button("test");
        test.setOnAction(e -> openBlogDetailsView(blog));
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
