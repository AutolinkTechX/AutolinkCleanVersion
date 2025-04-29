package org.example.pidev.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.example.pidev.entities.Blog;
import org.example.pidev.entities.Comment;
import org.example.pidev.entities.User;
import org.example.pidev.services.ServiceComment;

import java.util.List;
import java.util.logging.Level;

public class AdminCommentController {
    private Blog blog;

    @FXML
    private Label blogTitle;

    @FXML
    private VBox commentList;

    private final ServiceComment serviceComment = new ServiceComment();

    public void setBlog(Blog blog) {
        if (blog == null) {
            throw new IllegalArgumentException("Blog cannot be null");
        }
        this.blog = blog;
        initializeUI();
    }

    private void initializeUI() {
        try {
            blogTitle.setText("Commentaires pour : " + blog.getTitle());
            loadComments();
        } catch (Exception e) {
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
