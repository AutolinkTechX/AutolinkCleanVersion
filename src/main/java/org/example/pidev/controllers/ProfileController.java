package org.example.pidev.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.example.pidev.entities.User;
import org.example.pidev.utils.SessionManager;
import org.example.pidev.services.UserService;

import java.io.File;
import java.sql.SQLException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.prefs.BackingStoreException;
import javafx.application.Platform;

public class ProfileController {
    @FXML private TextField nameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private ImageView profileImageView;
    @FXML private Label imageNameLabel;
    @FXML private Label nameErrorLabel;
    @FXML private Label lastNameErrorLabel;
    @FXML private Label emailErrorLabel;
    @FXML private Label phoneErrorLabel;
    @FXML private Label imageErrorLabel;
    @FXML private ImageView submitLoadingIcon;
    @FXML private Button submitButton;
    
    private File selectedImageFile;
    private UserService userService = new UserService();

    @FXML
    public void initialize() {
        loadUserProfile();
    }

    private void loadUserProfile() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            Platform.runLater(() -> {
                nameField.setText(currentUser.getName());
                lastNameField.setText(currentUser.getLastName());
                emailField.setText(currentUser.getEmail());
                phoneField.setText(String.valueOf(currentUser.getPhone()));
                
                if (currentUser.getImage_path() != null && !currentUser.getImage_path().isEmpty()) {
                    try {
                        File imageFile = new File(currentUser.getImage_path());
                        Image image = new Image(imageFile.toURI().toString());
                        profileImageView.setImage(image);
                        imageNameLabel.setText(imageFile.getName());
                    } catch (Exception e) {
                        System.err.println("Error loading profile image: " + e.getMessage());
                        loadDefaultAvatar();
                    }
                } else {
                    loadDefaultAvatar();
                }
            });
        }
    }

    private void loadDefaultAvatar() {
        try {
            Image defaultImage = new Image(getClass().getResourceAsStream("/images/default-avatar.png"));
            profileImageView.setImage(defaultImage);
            imageNameLabel.setText("Default Avatar");
        } catch (Exception e) {
            System.err.println("Error loading default avatar: " + e.getMessage());
        }
    }

    @FXML
    private void handleBrowseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        
        selectedImageFile = fileChooser.showOpenDialog(profileImageView.getScene().getWindow());
        if (selectedImageFile != null) {
            try {
                Image image = new Image(selectedImageFile.toURI().toString());
                profileImageView.setImage(image);
                imageNameLabel.setText(selectedImageFile.getName());
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Image Error", "Could not load selected image");
            }
        }
    }

    @FXML
    private void handleChangePassword() {
        // Implementation for password change would go here
        showAlert(Alert.AlertType.INFORMATION, "Coming Soon", "Password change functionality will be implemented soon");
    }

    @FXML
    private void handleSubmit() {
        if (!validateInputs()) {
            return;
        }

        setLoading(true);
        
        new Thread(() -> {
            try {
                User currentUser = SessionManager.getCurrentUser();
                if (currentUser == null) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Error", "No user session found");
                        setLoading(false);
                    });
                    return;
                }

                // Update user object
                currentUser.setName(nameField.getText().trim());
                currentUser.setLastName(lastNameField.getText().trim());
                currentUser.setEmail(emailField.getText().trim());
                currentUser.setPhone(Integer.parseInt(phoneField.getText().trim()));

                // Handle image upload
                if (selectedImageFile != null) {
                    String imagePath = saveProfileImage(selectedImageFile);
                    currentUser.setImage_path(imagePath);
                }

                // Update database
                boolean updateSuccess = userService.updateUserProfile(currentUser);
                
                Platform.runLater(() -> {
                    if (updateSuccess) {
                        SessionManager.setCurrentUser(currentUser);
                        try {
                            SessionManager.saveSession(true);
                        } catch (BackingStoreException e) {
                            e.printStackTrace();
                        }
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Profile updated successfully");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to update profile");
                    }
                    setLoading(false);
                });
            } catch (NumberFormatException e) {
                Platform.runLater(() -> {
                    phoneErrorLabel.setText("Phone must be a number");
                    setLoading(false);
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to update profile: " + e.getMessage());
                    setLoading(false);
                });
                e.printStackTrace();
            } catch (IOException e) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "File Error", "Failed to save profile image: " + e.getMessage());
                    setLoading(false);
                });
                e.printStackTrace();
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Error", "An unexpected error occurred");
                    setLoading(false);
                });
                e.printStackTrace();
            }
        }).start();
    }

    private String saveProfileImage(File imageFile) throws IOException {
        File uploadDir = new File("uploads");
        if (!uploadDir.exists()) {
            uploadDir.mkdir();
        }

        String extension = imageFile.getName().substring(imageFile.getName().lastIndexOf("."));
        String newFilename = "profile_" + SessionManager.getCurrentUser().getId() + "_" + 
                          System.currentTimeMillis() + extension;

        File destFile = new File(uploadDir, newFilename);
        Files.copy(imageFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        return destFile.getAbsolutePath();
    }

    private boolean validateInputs() {
        boolean isValid = true;
        
        // Clear previous errors
        nameErrorLabel.setText("");
        lastNameErrorLabel.setText("");
        emailErrorLabel.setText("");
        phoneErrorLabel.setText("");
        imageErrorLabel.setText("");
        
        // Validate name
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            nameErrorLabel.setText("Name is required");
            isValid = false;
        } else if (name.length() > 50) {
            nameErrorLabel.setText("Name must be ≤ 50 characters");
            isValid = false;
        }
        
        // Validate last name
        String lastName = lastNameField.getText().trim();
        if (lastName.isEmpty()) {
            lastNameErrorLabel.setText("Last name is required");
            isValid = false;
        } else if (lastName.length() > 50) {
            lastNameErrorLabel.setText("Last name must be ≤ 50 characters");
            isValid = false;
        }
        
        // Validate email
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            emailErrorLabel.setText("Email is required");
            isValid = false;
        } else if (!email.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            emailErrorLabel.setText("Invalid email format");
            isValid = false;
        }
        
        // Validate phone
        String phone = phoneField.getText().trim();
        if (phone.isEmpty()) {
            phoneErrorLabel.setText("Phone is required");
            isValid = false;
        } else if (!phone.matches("\\d+")) {
            phoneErrorLabel.setText("Phone must contain only numbers");
            isValid = false;
        } else if (phone.length() > 15) {
            phoneErrorLabel.setText("Phone too long");
            isValid = false;
        }
        
        return isValid;
    }

    private void setLoading(boolean isLoading) {
        submitButton.setDisable(isLoading);
        submitLoadingIcon.setVisible(isLoading);
        submitButton.setText(isLoading ? "Saving..." : "Save Changes");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}