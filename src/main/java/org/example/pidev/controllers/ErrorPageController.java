package org.example.pidev.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import java.io.IOException;

public class ErrorPageController {

    @FXML
    private Button backToLoginButton;

    @FXML
    private Label errorMessageLabel;

    @FXML
    void handleGoBack(ActionEvent event) {
        try {
            // Close current login window
            Stage currentStage = (Stage) backToLoginButton.getScene().getWindow();
            currentStage.close();

            // Load SignUp view
            Parent root = FXMLLoader.load(getClass().getResource("/DashboardLogin.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Autolink Login");  
            stage.show();
        } catch (IOException e) {
            setErrorMessage("Failed to load sign up view");
            e.printStackTrace();
        }
    }

    public void setErrorMessage(String message) {
        errorMessageLabel.setText(message);
    }

}
