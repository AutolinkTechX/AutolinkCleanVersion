package org.example.pidev.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.example.pidev.entities.Blog;
import org.example.pidev.entities.Comment;
import org.example.pidev.entities.User;
import org.example.pidev.services.ServiceComment;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserCommentController {

    private static final Logger logger = Logger.getLogger(UserCommentController.class.getName());

    @FXML
    private Label blogTitle;
    @FXML
    private TextArea commentTextArea;
    @FXML
    private VBox commentList;

    private Blog blog;
    private User currentUser;
    private final ServiceComment serviceComment = new ServiceComment();

    public void setBlogAndUser(Blog blog, User user) {
        if (blog == null || user == null) {
            logger.log(Level.SEVERE, "Blog or User is null in setBlogAndUser");
            throw new IllegalArgumentException("Blog and User cannot be null");
        }
        this.blog = blog;
        this.currentUser = user;
        initializeUI();
    }

    private void initializeUI() {
        try {
            blogTitle.setText("Commentaires pour : " + blog.getTitle());
            loadComments();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error initializing UI", e);
            showAlert("Initialization Error", "Failed to initialize comment interface");
        }
    }

    private void loadComments() {
        commentList.getChildren().clear();

        try {
            List<Comment> comments = serviceComment.getCommentsByBlog(blog.getId());

            if (comments.isEmpty()) {
                Label noComments = new Label("No comments yet. Be the first to comment!");
                noComments.setStyle("-fx-font-style: italic; -fx-text-fill: gray;");
                commentList.getChildren().add(noComments);
                return;
            }

            for (Comment comment : comments) {
                VBox commentBox = createCommentBox(comment);
                commentList.getChildren().add(commentBox);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading comments", e);
            showAlert("Database Error", "Failed to load comments");
        }
    }

    private VBox createCommentBox(Comment comment) {
        VBox commentBox = new VBox(5);
        commentBox.setStyle("-fx-padding: 10; -fx-background-color: #f5f5f5; -fx-border-color: #ccc; -fx-border-radius: 5; -fx-background-radius: 5;");

        Label content = new Label(formatCommentHeader(comment) + comment.getContent());
        content.setWrapText(true);
        content.setStyle("-fx-font-size: 14px;");

        commentBox.getChildren().add(content);

        return commentBox;
    }

    private String formatCommentHeader(Comment comment) {
        return comment.getUser().getName() + " - " + comment.getCreated_at() + ":\n";
    }

    @FXML
    private void handleAddComment() {
        String content = commentTextArea.getText().trim();

        if (content.isEmpty()) {
            showAlert("Validation Error", "Le commentaire ne peut pas être vide");
            return;
        }

        if (content.length() > 500) {
            showAlert("Validation Error", "Le commentaire ne peut pas dépasser 500 caractères");
            return;
        }

        try {
            Comment newComment = new Comment();
            newComment.setBlog(blog);
            newComment.setContent(content);
            newComment.setCreated_at(Date.valueOf(LocalDate.now()));
            newComment.setUser(currentUser);

            serviceComment.ajouter(newComment);
            commentTextArea.clear();
            loadComments();

            showInformationAlert("Succès", "Votre commentaire a été ajouté avec succès");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Database error adding comment", e);
            showAlert("Database Error", "Échec de l'ajout du commentaire");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInformationAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
