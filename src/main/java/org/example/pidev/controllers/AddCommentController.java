package org.example.pidev.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import org.example.pidev.entities.Blog;
import org.example.pidev.entities.Comment;
import org.example.pidev.entities.User;
import org.example.pidev.services.ServiceComment;
import org.example.pidev.services.UserService;

import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;

public class AddCommentController {
    @FXML private TextArea commentTextArea;

    private Blog blog;
    private Runnable onCommentAdded;

    public void setBlog(Blog blog) {
        this.blog = blog;
    }

    public void setOnCommentAdded(Runnable callback) {
        this.onCommentAdded = callback;
    }

    @FXML
    private void handleSubmit() {
        if (commentTextArea.getText().isEmpty()) {
            return;
        }

        try {
            // TODO: Replace with the actual logged-in user
            UserService userService = new UserService();
            User user = userService.getUserById(1); // ⚠️ Temporaire

            Comment comment = new Comment(
                    blog,
                    commentTextArea.getText(),
                    Date.valueOf(LocalDate.now()),
                    user
            );

            ServiceComment service = new ServiceComment();
            service.ajouter(comment);

            if (onCommentAdded != null) {
                onCommentAdded.run();
            }

            commentTextArea.getScene().getWindow().hide();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
