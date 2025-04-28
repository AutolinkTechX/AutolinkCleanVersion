package org.example.pidev.controllers;

import com.itextpdf.text.pdf.PdfPCell;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import org.example.pidev.entities.Blog;
import org.example.pidev.entities.User;
import org.example.pidev.services.ServiceBlog;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserBlogController {

    @FXML private FlowPane blogContainer;
    @FXML private TextField searchField;
    @FXML private HBox searchBox;
    @FXML private Button downloadPdfButton;

    private final ServiceBlog serviceBlog = new ServiceBlog();
    private User currentUser;
    private static final Logger logger = Logger.getLogger(UserBlogController.class.getName());
    private List<Blog> allBlogs = new ArrayList<>();

    public void setCurrentUser(User user) {
        if (user == null || user.getId() <= 0) {
            throw new IllegalArgumentException("User must be valid and authenticated");
        }
        this.currentUser = user;
    }

    @FXML
    public void initialize() {
        try {
            if (searchBox == null) {
                searchBox = new HBox(10);
                searchBox.setPadding(new Insets(10));
                searchField = new TextField();
                searchField.setPromptText("Search blogs...");
                HBox.setHgrow(searchField, Priority.ALWAYS);
                searchBox.getChildren().add(searchField);

                if (blogContainer.getParent() instanceof VBox) {
                    VBox parent = (VBox) blogContainer.getParent();
                    parent.getChildren().add(0, searchBox);
                }
            }

            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                try {
                    filterBlogs(newValue);
                } catch (SQLException e) {
                    logger.log(Level.SEVERE, "Error filtering blogs", e);
                }
            });

            loadBlogs();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize blog controller", e);
            showAlert("Initialization Error", "Failed to load blog content", e.getMessage());
        }
    }

    private void loadBlogs() throws SQLException {
        blogContainer.getChildren().clear();
        allBlogs = serviceBlog.afficher();
        displayBlogs(allBlogs);
    }

    private void filterBlogs(String searchText) throws SQLException {
        if (searchText == null || searchText.isEmpty()) {
            displayBlogs(allBlogs);
            return;
        }

        String lowerCaseSearch = searchText.toLowerCase();
        List<Blog> filteredBlogs = new ArrayList<>();

        for (Blog blog : allBlogs) {
            if (blog.getTitle().toLowerCase().contains(lowerCaseSearch) ||
                    blog.getContent().toLowerCase().contains(lowerCaseSearch)) {
                filteredBlogs.add(blog);
            }
        }

        displayBlogs(filteredBlogs);
    }

    private void displayBlogs(List<Blog> blogsToDisplay) {
        Platform.runLater(() -> {
            blogContainer.getChildren().clear();
            for (Blog blog : blogsToDisplay) {
                VBox blogCard = createBlogCard(blog);
                blogContainer.getChildren().add(blogCard);
            }
        });
    }

    private VBox createBlogCard(Blog blog) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-border-radius: 8; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1);");

        ImageView imageView = new ImageView();
        try {
            Image image = loadBlogImage(blog);
            imageView.setImage(image);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Could not load blog image", ex);
            try {
                imageView.setImage(new Image(getClass().getResourceAsStream("/images/logo.jpg")));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Couldn't load default image", e);
            }
        }

        imageView.setFitWidth(250);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(true);

        Label title = new Label(blog.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 18px;");

        String contentPreview = blog.getContent().length() > 100 ?
                blog.getContent().substring(0, 100) + "..." : blog.getContent();
        Label content = new Label(contentPreview);
        content.setWrapText(true);
        content.setMaxWidth(250);

        Label likeLabel = new Label("♥ " + blog.getLikes());
        Label dislikeLabel = new Label("👎 " + blog.getDislikes());

        Button likeButton = new Button("♥");
        likeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c;");
        likeButton.setOnAction(e -> handleVote(blog, true, likeLabel, dislikeLabel));

        Button dislikeButton = new Button("👎");
        dislikeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #3498db;");
        dislikeButton.setOnAction(e -> handleVote(blog, false, likeLabel, dislikeLabel));

        HBox voteBox = new HBox(10, likeButton, likeLabel, dislikeButton, dislikeLabel);

        Button commentButton = new Button("📝 Commenter");
        commentButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");
        commentButton.setOnAction(e -> openCommentWindow(blog));

        card.getChildren().addAll(imageView, title, content, voteBox, commentButton);
        return card;
    }

    private Image loadBlogImage(Blog blog) throws Exception {
        if (blog.getImage() == null || blog.getImage().isEmpty()) {
            return new Image(getClass().getResourceAsStream("/images/logo.jpg"));
        }

        if (blog.getImage().startsWith("file:/")) {
            return new Image(blog.getImage());
        } else if (blog.getImage().matches("^[a-zA-Z]:\\\\.*")) {
            return new Image("file:///" + blog.getImage().replace("\\", "/"));
        } else {
            InputStream resourceStream = getClass().getResourceAsStream("/" + blog.getImage());
            return (resourceStream != null) ? new Image(resourceStream)
                    : new Image(getClass().getResourceAsStream("/images/logo.jpg"));
        }
    }

    private void handleVote(Blog blog, boolean isLike, Label likeLabel, Label dislikeLabel) {
        try {
            if (currentUser == null) {
                showAlert("Login Required", "You must be logged in to vote.", "");
                return;
            }

            boolean success = serviceBlog.voteBlog(blog.getId(), currentUser.getId(), isLike);
            if (!success) {
                showAlert("Vote Denied", "You've already voted for this blog.", "");
                return;
            }

            Blog updatedBlog = serviceBlog.getBlogById(blog.getId());
            Platform.runLater(() -> {
                likeLabel.setText("♥ " + updatedBlog.getLikes());
                dislikeLabel.setText("👎 " + updatedBlog.getDislikes());
            });
        } catch (SQLException e) {
            showAlert("Error", "Failed to vote", e.getMessage());
        }
    }

    private void openCommentWindow(Blog blog) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UserCommentView.fxml"));
            Parent commentView = loader.load();

            UserCommentController controller = loader.getController();
            controller.setBlogAndUser(blog, currentUser);

            Stage stage = new Stage();
            stage.setTitle("Comments: " + blog.getTitle());
            stage.setScene(new Scene(commentView));
            stage.show();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to open comment window", e);
            showAlert("Error", "Failed to open comment window", e.getMessage());
        }
    }

    @FXML
    private void handleDownloadPdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer les blogs en PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        fileChooser.setInitialFileName("export_blogs.pdf");

        Stage stage = (Stage) blogContainer.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file == null) {
            return; // L'utilisateur a annulé
        }

        Document document = null;
        FileOutputStream fos = null;

        try {
            // Créer un nouveau document avec des marges définies
            document = new Document(com.itextpdf.text.PageSize.A4, 50, 50, 50, 50);
            fos = new FileOutputStream(file);
            PdfWriter writer = PdfWriter.getInstance(document, fos);

            document.open();

            // Ajouter un en-tête
            com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD);
            Paragraph title = new Paragraph("Rapport d'exportation des blogs", titleFont);
            title.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            document.add(title);

            // Ajouter une ligne vide
            document.add(new Paragraph(" "));

            // Créer le tableau avec des largeurs de colonnes spécifiques
            PdfPTable table = new PdfPTable(4);
            float[] columnWidths = {0.5f, 2f, 4f, 1.5f};
            table.setWidths(columnWidths);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);

            // Définir un style pour les en-têtes
            com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12, com.itextpdf.text.Font.BOLD);

            // Ajouter les en-têtes (cette approche fonctionne avec toutes les versions d'iText)
            // En-tête "ID"
            PdfPCell cell1 = new PdfPCell(new Paragraph("ID", headerFont));
            cell1.setBackgroundColor(new com.itextpdf.text.BaseColor(220, 220, 220));
            cell1.setPadding(5);
            table.addCell(cell1);

            // En-tête "Titre"
            PdfPCell cell2 = new PdfPCell(new Paragraph("Titre", headerFont));
            cell2.setBackgroundColor(new com.itextpdf.text.BaseColor(220, 220, 220));
            cell2.setPadding(5);
            table.addCell(cell2);

            // En-tête "Contenu"
            PdfPCell cell3 = new PdfPCell(new Paragraph("Contenu", headerFont));
            cell3.setBackgroundColor(new com.itextpdf.text.BaseColor(220, 220, 220));
            cell3.setPadding(5);
            table.addCell(cell3);

            // En-tête "Votes"
            PdfPCell cell4 = new PdfPCell(new Paragraph("Votes", headerFont));
            cell4.setBackgroundColor(new com.itextpdf.text.BaseColor(220, 220, 220));
            cell4.setPadding(5);
            table.addCell(cell4);

            // Ajouter les données
            List<Blog> blogsToExport = getFilteredBlogsForExport();
            if (blogsToExport.isEmpty()) {
                document.add(new Paragraph("Aucun blog ne correspond à vos critères."));
            } else {
                for (Blog blog : blogsToExport) {
                    // Ajouter chaque cellule individuellement
                    table.addCell(String.valueOf(blog.getId()));
                    table.addCell(blog.getTitle());

                    String content = blog.getContent();
                    if (content.length() > 200) {
                        content = content.substring(0, 200) + "...";
                    }
                    table.addCell(content);

                    table.addCell("Likes: " + blog.getLikes() + ", Dislikes: " + blog.getDislikes());
                }
                document.add(table);
            }

            // Ajouter un pied de page
            document.add(new Paragraph(" "));
            Paragraph footer = new Paragraph("Document généré le " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            footer.setAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
            document.add(footer);

            // Assurez-vous de fermer le document avant de fermer le flux
            document.close();

            // Vérifier que le fichier existe et n'est pas vide
            if (file.exists() && file.length() > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "PDF enregistré avec succès à:\n" + file.getAbsolutePath());
            } else {
                throw new IOException("Le fichier PDF n'a pas été créé correctement");
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur de génération PDF", e);
            e.printStackTrace();
            showAlert("Erreur", "Échec de la génération du PDF", e.getMessage());
        } finally {
            // Fermer proprement le flux s'il est encore ouvert
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Erreur lors de la fermeture du flux", e);
            }
        }
    }
    private List<Blog> getFilteredBlogsForExport() {
        String searchText = searchField.getText();
        if (searchText == null || searchText.isEmpty()) {
            return allBlogs;
        }

        String lowerCaseSearch = searchText.toLowerCase();
        List<Blog> filteredBlogs = new ArrayList<>();

        for (Blog blog : allBlogs) {
            if (blog.getTitle().toLowerCase().contains(lowerCaseSearch) ||
                    blog.getContent().toLowerCase().contains(lowerCaseSearch)) {
                filteredBlogs.add(blog);
            }
        }
        return filteredBlogs;
    }

    private void showAlert(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void refreshBlogs() {
        try {
            loadBlogs();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to refresh blogs", e);
        }
    }
}