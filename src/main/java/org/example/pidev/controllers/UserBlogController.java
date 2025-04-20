package org.example.pidev.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.example.pidev.entities.Blog;
import org.example.pidev.entities.User;
import org.example.pidev.services.ServiceBlog;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserBlogController {

    @FXML
    private FlowPane blogContainer;

    private final ServiceBlog serviceBlog = new ServiceBlog();
    private User currentUser;
    private static final Logger logger = Logger.getLogger(UserBlogController.class.getName());

    public void setCurrentUser(User user) {
        if (user == null || user.getId() <= 0) {
            throw new IllegalArgumentException("User must be valid and authenticated");
        }
        this.currentUser = user;
    }

    @FXML
    public void initialize() {
        try {
            loadBlogs();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize blog controller", e);
            showAlert("Initialization Error", "Failed to load blog content", e.getMessage());
        }
    }

    private void loadBlogs() throws SQLException {
        blogContainer.getChildren().clear();
        List<Blog> blogs = serviceBlog.afficher();

        for (Blog blog : blogs) {
            VBox blogCard = createBlogCard(blog);
            blogContainer.getChildren().add(blogCard);
        }
    }

    private VBox createBlogCard(Blog blog) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-border-radius: 8; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1);");

        ImageView imageView = new ImageView();
        try {
            Image image;
            if (blog.getImage() != null && !blog.getImage().isEmpty()) {
                if (blog.getImage().startsWith("file:/") || blog.getImage().matches("^[a-zA-Z]:\\\\.*")) {
                    image = new Image("file:///" + blog.getImage().replace("\\", "/"));
                } else {
                    InputStream resourceStream = getClass().getResourceAsStream("/" + blog.getImage());
                    image = (resourceStream != null) ? new Image(resourceStream)
                            : new Image(getClass().getResourceAsStream("/images/logo.jpg"));
                }
            } else {
                image = new Image(getClass().getResourceAsStream("/images/logo.jpg"));
            }
            imageView.setImage(image);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Could not load blog image", ex);
            imageView.setImage(new Image(getClass().getResourceAsStream("/images/logo.jpg")));
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

    private void showAlert(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}