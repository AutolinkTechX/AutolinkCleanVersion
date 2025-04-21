package org.example.pidev.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.example.pidev.entities.Blog;
import org.example.pidev.entities.Comment;
import org.example.pidev.services.ServiceComment;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

public class BlogDetailsController {

    @FXML private Label titleLabel;
    @FXML private ImageView blogImage;
    @FXML private TextArea contentArea;
    @FXML private ListView<Comment> commentsListView;

    private Blog blog;
    private final ServiceComment serviceComment = new ServiceComment();

    public void setBlog(Blog blog) {
        this.blog = blog;
        loadBlogDetails();
        loadComments();
    }

    private void loadBlogDetails() {
        titleLabel.setText(blog.getTitle());
        contentArea.setText(blog.getContent());

        try {
            InputStream stream = getClass().getResourceAsStream("/images/logo.jpg");
            if (blog.getImage() != null && !blog.getImage().isEmpty()) {
                stream = getClass().getResourceAsStream("/" + blog.getImage());
            }
            if (stream != null) {
                blogImage.setImage(new Image(stream));
            }
        } catch (Exception e) {
            System.err.println("Error loading blog image: " + e.getMessage());
        }
    }

    private void loadComments() {
        try {
            List<Comment> comments = serviceComment.getCommentsByBlog(blog.getId());
            commentsListView.getItems().setAll(comments);
        } catch (SQLException e) {
            System.err.println("Error loading comments: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) titleLabel.getScene().getWindow();
        stage.close();
    }

    @FXML private void handleAddComment() {}
    @FXML private void handleBackToBlogs() {}
}