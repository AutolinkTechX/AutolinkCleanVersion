package org.example.pidev.controllers;

import javafx.animation.AnimationTimer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.embed.swing.SwingFXUtils;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;

import java.awt.image.BufferedImage;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.File;
import javax.imageio.ImageIO;

import org.example.pidev.entities.User;
import org.example.pidev.services.UserService;

public class CameraViewController {
    private final UserService userService = new UserService();
    private static final String PROFILE_PHOTOS_DIR = "profilePhotos";

    @FXML private ImageView backButton;
    @FXML private ImageView cameraImageView;
    @FXML private Button captureButton;
    @FXML private Button confirmPhotoButton;
    @FXML private AnchorPane cameraPane;

    private Webcam webcam;
    private AnimationTimer timer;
    private BufferedImage capturedImage;
    private boolean isImageCaptured = false;
    private User userData;

    @FXML
    public void initialize() {
        // Initialize webcam
        webcam = Webcam.getDefault();
        if (webcam == null) {
            System.err.println("No webcam detected!");
            return;
        }

        webcam.setViewSize(WebcamResolution.VGA.getSize());
        webcam.open();

        // Start the camera feed
        startCameraPreview();

        // Initially disable confirm and retake buttons
        confirmPhotoButton.setDisable(true);
    }

    private void startCameraPreview() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!isImageCaptured && webcam.isOpen()) {
                    BufferedImage image = webcam.getImage();
                    if (image != null) {
                        WritableImage fxImage = SwingFXUtils.toFXImage(image, null);
                        cameraImageView.setImage(fxImage);
                    }
                }
            }
        };
        timer.start();
    }

    @FXML
    void capturePhoto(ActionEvent event) {
        if (webcam != null && webcam.isOpen()) {
            capturedImage = webcam.getImage();
            isImageCaptured = true;
            
            // Display the captured image
            cameraImageView.setImage(SwingFXUtils.toFXImage(capturedImage, null));
            
            confirmPhotoButton.setDisable(false);
            captureButton.setDisable(true);
        }
    }

    @FXML
    void confirmPhoto(ActionEvent event) {
        if (capturedImage == null) {
            System.err.println("No image captured to save!");
            return;
        }

        try {
            // Create directory if it doesn't exist
            Files.createDirectories(Paths.get(PROFILE_PHOTOS_DIR));
            
            // Generate filename with timestamp
            String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            
            String filename = PROFILE_PHOTOS_DIR + File.separator;
            
            // If we have user data, include username in filename
            if (userData != null && userData.getName() != null && userData.getLastName() != null) {
                filename += userData.getName() + "_" + userData.getLastName();
            }
            
            filename += timestamp + ".png";
            
            // Save the image
            File outputFile = new File(filename);
            ImageIO.write(capturedImage, "PNG", outputFile);
            
            System.out.println("Photo saved successfully to: " + outputFile.getAbsolutePath());
            
            userData.setImage_path(outputFile.getAbsolutePath());

            boolean success = userService.createAccount(userData);
            if(success){
                stopCamera();
                handleBackButton(null);
                try {
                    // Close current login window
                    Stage currentStage = (Stage) confirmPhotoButton.getScene().getWindow();
                    currentStage.close();
        
                    // Load SignUp view
                    Parent root = FXMLLoader.load(getClass().getResource("/DashboardLogin.fxml"));
                    Stage stage = new Stage();
                    stage.setScene(new Scene(root));
                    stage.setTitle("Autolink Login");  
                    stage.show();
                } catch (IOException e) {
                    showAlert(Alert.AlertType.ERROR,"Error", "Load Dashboard login failed");
                    e.printStackTrace();
                }
            }
            else{
                System.out.println("error");
            }
        
        } catch (IOException e) {
            System.err.println("Error saving profile photo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleBackButton(MouseEvent event) {
        stopCamera();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/RightPane.fxml"));
            AnchorPane signUpView = loader.load();
            AnchorPane rightPane = (AnchorPane) cameraPane.getParent();
            rightPane.getChildren().setAll(signUpView);
        } catch (IOException e) {
            e.printStackTrace();
            // Handle error appropriately
        }
    }

    public void stopCamera() {
        if (timer != null) {
            timer.stop();
        }
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
    }

    public void initData(User userData) {
        this.userData = userData;
        
        // You can now use the user data in your controller
        if (userData != null) {
            System.out.println("Received user data - Username: " + userData.getName() + " " + userData.getLastName());
            System.out.println("Email: " + userData.getEmail());
            System.out.println("Passwor: " + userData.getPassword());
            System.out.println("Phone: " + userData.getPhone());
            
            // Example: Use the data in your photo confirmation
            confirmPhotoButton.setOnAction(e -> {
                System.out.println("Confirming photo for user: " + userData.getName());
                confirmPhoto(e);
            });
        }
    }
}