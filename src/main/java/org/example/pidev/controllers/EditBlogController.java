package org.example.pidev.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.example.pidev.entities.Blog;
import org.example.pidev.services.ServiceBlog;

import java.sql.SQLException;

public class EditBlogController {

    @FXML
    private TextField titleField;
    @FXML
    private TextArea contentField;  // Changed from TextField to TextArea
    @FXML
    private TextField imageField;
    @FXML
    private TextField likesField;
    @FXML
    private TextField dislikesField;

    private Blog blog;  // Store the current blog object

    // This method will be used to set the Blog object
    public void setBlog(Blog blog) {
        this.blog = blog;
        // Initialize the fields with the blog's data
        titleField.setText(blog.getTitle());
        contentField.setText(blog.getContent());
        imageField.setText(blog.getImage());
        likesField.setText(String.valueOf(blog.getLikes()));
        dislikesField.setText(String.valueOf(blog.getDislikes()));
    }

    // This method will be used to save the changes made to the blog
    @FXML
    private void handleSave() {  // Updated to match the FXML onAction attribute
        // Update the blog object with the new values
        blog.setTitle(titleField.getText());
        blog.setContent(contentField.getText());
        blog.setImage(imageField.getText());
        blog.setLikes(Integer.parseInt(likesField.getText()));
        blog.setDislikes(Integer.parseInt(dislikesField.getText()));

        // Save the updated blog using the service
        try {
            ServiceBlog serviceBlog = new ServiceBlog();
            serviceBlog.modifier(blog);  // Call the modify method of the service
            System.out.println("Blog updated!");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // After saving, you can update the list of blogs in the main view
        if (onUpdateCallback != null) {
            onUpdateCallback.run();  // Trigger the callback to reload the data
        }
    }

    // The callback function to reload data after updating the blog
    private Runnable onUpdateCallback;

    // Set the callback
    public void setOnUpdateCallback(Runnable callback) {
        this.onUpdateCallback = callback;
    }
}
