package org.example.pidev.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import java.io.File;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.example.pidev.services.UserService;
import org.example.pidev.utils.SessionManager;
import org.example.pidev.entities.User;


import javafx.scene.control.Alert;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javafx.stage.FileChooser;


public class ProfileController {

    @FXML
    private Label emailError;

    @FXML
    private TextField emailField;

    @FXML
    private Label imageError;

    @FXML
    private Label imageNameLabel;

    @FXML
    private Label lastNameError;

    @FXML
    private TextField lastNameField;

    @FXML
    private Label nameError;

    @FXML
    private TextField nameField;

    @FXML
    private Label phoneError;

    @FXML
    private TextField phoneField;

    @FXML
    private ImageView profileImageView;

    private User currentUser;
    private File selectedImageFile;
    private final UserService userService = new UserService();
    private ClientDashboardController dashboardController;

    public void setDashboardController(ClientDashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    @FXML
    public void initialize() {
        currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            loadUserData();
        }
    }

    private void loadUserData() {
        nameField.setText(currentUser.getName());
        lastNameField.setText(currentUser.getLastName());
        emailField.setText(currentUser.getEmail());
        phoneField.setText(String.valueOf(currentUser.getPhone()));
        
        // Load profile image if exists
        try{
            if (currentUser.getImage_path() != null && !currentUser.getImage_path().isEmpty()) {
                Image image = new Image(new File(currentUser.getImage_path()).toURI().toString());
                profileImageView.setImage(image);
            }else{
                profileImageView.setImage(new Image(getClass().getResourceAsStream("/icons/Users.png")));
            }
        }catch(Exception e){
            System.err.println("Error loading profile image: " + e.getMessage());
        }
    }

    @FXML
    void handleChangePassword(ActionEvent event) {
        try {
            // Load the FXML file for the password change dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/changePassword.fxml"));
            Parent root = loader.load();
            
            // Get the controller and set the current user
            ChangePasswordController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            
            // Create a new stage for the dialog
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.UTILITY);
            dialogStage.setTitle("Change Password");
            dialogStage.setScene(new Scene(root));
            
            // Set the owner of the dialog to the main window
            Stage mainStage = (Stage) nameField.getScene().getWindow();
            dialogStage.initOwner(mainStage);
            
            // Show the dialog and wait for it to be closed
            dialogStage.showAndWait();
            
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load password change dialog");
            e.printStackTrace();
        }
    }

    @FXML
    void handleImageUpload(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        
        selectedImageFile = fileChooser.showOpenDialog(profileImageView.getScene().getWindow());
        if (selectedImageFile != null) {
            imageNameLabel.setText(selectedImageFile.getName());
            profileImageView.setImage(new Image(selectedImageFile.toURI().toString()));
        }
    }

    @FXML
    void handleSubmit(ActionEvent event) {
        // Validate inputs
        boolean isValid = validateInputs();
        
        if (!isValid) return;

        // Update user data
        updateUserFromForm();

        // Handle image upload if a new image was selected
        handleImageUploadIfNeeded();

        // Save to database
        try {
            boolean success = userService.updateUserProfile(currentUser);
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Profile updated successfully");
                SessionManager.setCurrentUser(currentUser); // Update session
                
                // Instead of creating a new instance, use the reference we have
                if (dashboardController != null) {
                    dashboardController.updateUserMenu();
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to update profile");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean validateInputs() {
        boolean isValid = true;
        
        if (nameField.getText().isEmpty()) {
            nameError.setText("Name is required");
            isValid = false;
        } else {
            nameError.setText("");
        }
        
        if (lastNameField.getText().isEmpty()) {
            lastNameError.setText("Last name is required");
            isValid = false;
        } else {
            lastNameError.setText("");
        }
        
        if (emailField.getText().isEmpty()) {
            emailError.setText("Email is required");
            isValid = false;
        } else if (!emailField.getText().matches(".+@.+\\..+")) {
            emailError.setText("Invalid email format");
            isValid = false;
        } else {
            emailError.setText("");
        }
        
        try {
            Integer.parseInt(phoneField.getText());
            phoneError.setText("");
        } catch (NumberFormatException e) {
            phoneError.setText("Phone must be a number");
            isValid = false;
        }
        
        return isValid;
    }

    private void updateUserFromForm() {
        currentUser.setName(nameField.getText());
        currentUser.setLastName(lastNameField.getText());
        currentUser.setEmail(emailField.getText());
        currentUser.setPhone(Integer.parseInt(phoneField.getText()));
    }

    private void handleImageUploadIfNeeded() {
        if (selectedImageFile != null) {
            try {
                // Create uploads directory if it doesn't exist
                File uploadsDir = new File("uploads");
                if (!uploadsDir.exists()) {
                    uploadsDir.mkdir();
                }
                
                // Save the file
                File destFile = new File(uploadsDir, selectedImageFile.getName());
                Files.copy(selectedImageFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                
                currentUser.setImage_path(destFile.getAbsolutePath());
            } catch (IOException e) {
                imageError.setText("Error uploading image");
                e.printStackTrace();
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


}
