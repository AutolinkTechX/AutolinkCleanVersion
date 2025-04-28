package org.example.pidev.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.example.pidev.entities.Blog;
import org.example.pidev.entities.Comment;
import org.example.pidev.entities.User;
import org.example.pidev.services.ServiceComment;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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

    private static final String API_KEY = "c7syCiA2zV9WumEMzUWIgBZUcGbT6Mrq";

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
            if (containsBadWords(content)) {
                showAlert("Validation Error", "Votre commentaire contient des mots inappropriés");
                return;
            }

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

    private boolean containsBadWords(String text) {
        try {
            URL url = new URL("https://api.apilayer.com/bad_words");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String payload = "{\"text\":\"" + text.replace("\"", "\\\"") + "\"}";
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int status = conn.getResponseCode();
            if (status != 200) {
                logger.warning("API Bad Word call failed with status: " + status);
                return false; // safer to allow than block on API fail
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            conn.disconnect();

            // Simple parsing without org.json
            String responseString = response.toString();
            // Find "bad_words_total": N
            int index = responseString.indexOf("\"bad_words_total\":");
            if (index != -1) {
                int start = index + 18;
                int end = responseString.indexOf(",", start);
                String numberString = responseString.substring(start, end).trim();
                int badWordsTotal = Integer.parseInt(numberString);
                return badWordsTotal > 0;
            } else {
                logger.warning("Unexpected API response: " + responseString);
                return false;
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error checking bad words", e);
            return false; // safer to allow comment if API check fails
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
