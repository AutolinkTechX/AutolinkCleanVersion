package org.example.pidev.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.example.pidev.entities.Entreprise;
import org.example.pidev.entities.User;
import org.example.pidev.services.EntrepriseService;
import org.example.pidev.services.UserService;
import org.example.pidev.utils.SessionManager;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javafx.application.Platform;

import java.io.IOException;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class LoginDashboardController {

    @FXML private TextField emailField;
    @FXML private Label errorLabel;
    @FXML private Label emailErrorLabel;
    @FXML private Label passwordErrorLabel;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Button loginButton;
    @FXML private AnchorPane loginPane;
    @FXML private PasswordField passwordField;
    @FXML private Button showPasswordButton;
    @FXML private TextField visiblePasswordField;
    @FXML private CheckBox rememberMeCheckBox;
    @FXML private Button signUpButton;
    

    private final UserService userService = new UserService();
    private final EntrepriseService entrepriseService = new EntrepriseService();
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final String PREFS_NAME = "pidev_app_prefs";
    private static final String REMEMBER_ME_KEY = "remember_me";
    private static final String EMAIL_KEY = "saved_email";
    private static final String PASSWORD_KEY = "saved_password";
    private static final String USER_TYPE_KEY = "user_type";

    @FXML
    public void initialize() {
        loadSavedCredentials();

        loginButton.setOnAction(event -> {
            try {
                handleLogin();
            } catch (BackingStoreException e) {
                e.printStackTrace();
            }
        });
        forgotPasswordLink.setOnAction(event -> handleForgotPassword());

        // Initialize the visible password field
        visiblePasswordField = new TextField();
        visiblePasswordField.setManaged(false);
        visiblePasswordField.setVisible(false);
        visiblePasswordField.setPromptText("Password");
        visiblePasswordField.getStyleClass().add("login-field");
        
        // Add the visible password field to the same HBox as the password field
        ((HBox) passwordField.getParent()).getChildren().add(visiblePasswordField);

        // Bind the visible password field to the password field
        passwordField.textProperty().bindBidirectional(visiblePasswordField.textProperty());

        // Enable login button only when fields are valid
        emailField.textProperty().addListener((obs, oldVal, newVal) -> validateInputs());
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> validateInputs());

    }

    private void loadSavedCredentials() {
        Preferences prefs = Preferences.userRoot().node(PREFS_NAME);
        boolean rememberMe = prefs.getBoolean(REMEMBER_ME_KEY, false);
        
        if (rememberMe) {
            String savedEmail = prefs.get(EMAIL_KEY, "");
            String savedPassword = prefs.get(PASSWORD_KEY, "");
            String userType = prefs.get(USER_TYPE_KEY, "");
            
            // Set the fields for visual feedback
            emailField.setText(savedEmail);
            passwordField.setText(savedPassword);
            rememberMeCheckBox.setSelected(true);
            
            // Schedule auto-login to run after the scene is fully initialized
            Platform.runLater(() -> {
                if (!savedEmail.isEmpty() && !savedPassword.isEmpty()) {
                    try {
                        if ("USER".equals(userType)) {
                            User user = userService.authenticate(savedEmail, savedPassword);
                            if (user != null) {
                                SessionManager.setCurrentUser(user);
                                redirectBasedOnRole(user.getRoleId(), user);
                            } else {
                                clearSavedCredentials();
                                System.out.println("Auto-login failed: Invalid credentials");
                            }
                        } else if ("ENTREPRISE".equals(userType)) {
                            Entreprise entreprise = entrepriseService.authenticate(savedEmail, savedPassword);
                            if (entreprise != null) {
                                SessionManager.setCurrentEntreprise(entreprise);
                                redirectBasedOnRole(entreprise.getRoleId(), null);
                            } else {
                                clearSavedCredentials();
                                System.out.println("Auto-login failed: Invalid credentials");
                            }
                        }
                    } catch (Exception e) {
                        clearSavedCredentials();
                        System.out.println("Auto-login failed: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void saveCredentials(String email, String password, String userType) {
        Preferences prefs = Preferences.userRoot().node(PREFS_NAME);
        prefs.putBoolean(REMEMBER_ME_KEY, true);
        prefs.put(EMAIL_KEY, email);
        prefs.put(PASSWORD_KEY, password);
        prefs.put(USER_TYPE_KEY, userType);
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    private void clearSavedCredentials() {
        Preferences prefs = Preferences.userRoot().node(PREFS_NAME);
        prefs.remove(REMEMBER_ME_KEY);
        prefs.remove(EMAIL_KEY);
        prefs.remove(PASSWORD_KEY);
        prefs.remove(USER_TYPE_KEY);
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    private void validateInputs() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        
        boolean isEmailValid = !email.isEmpty() && EMAIL_PATTERN.matcher(email).matches();
        boolean isPasswordValid = !password.isEmpty();
        
        // Update error labels
        if (email.isEmpty()) {
            emailErrorLabel.setText("Email is required");
            emailErrorLabel.setVisible(true);
        } else if (!isEmailValid) {
            emailErrorLabel.setText("Please enter a valid email address");
            emailErrorLabel.setVisible(true);
        } else {
            emailErrorLabel.setVisible(false);
        }
        
        if (password.isEmpty()) {
            passwordErrorLabel.setText("Password is required");
            passwordErrorLabel.setVisible(true);
        } else {
            passwordErrorLabel.setVisible(false);
        }
        
        // Enable/disable login button
        loginButton.setDisable(!isEmailValid || !isPasswordValid);
    }

    @FXML
    private void togglePasswordVisibility() {
        boolean isPasswordVisible = visiblePasswordField.isVisible();
        
        if (isPasswordVisible) {
            // Switch to password field
            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            // Change to regular eye icon
            ImageView eyeIcon = (ImageView) showPasswordButton.getGraphic();
            eyeIcon.setImage(new Image(getClass().getResourceAsStream("/icons/eye.png")));
        } else {
            // Switch to visible text field
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            visiblePasswordField.setVisible(true);
            visiblePasswordField.setManaged(true);
            // Change to crossed eye icon
            ImageView eyeIcon = (ImageView) showPasswordButton.getGraphic();
            eyeIcon.setImage(new Image(getClass().getResourceAsStream("/icons/eye-crossed.png")));
        }
    }

    private void handleLogin() throws BackingStoreException {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        try {
            try {
                User authenticatedUser = userService.authenticate(email, password);
                SessionManager.setCurrentUser(authenticatedUser);

                // Save credentials if "Remember Me" is checked
                if (rememberMeCheckBox.isSelected()) {
                    saveCredentials(email, password, "USER");
                } else {
                    clearSavedCredentials();
                }

                try {
                    redirectBasedOnRole(authenticatedUser.getRoleId(), authenticatedUser);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            } catch (IllegalArgumentException e1) {
                // If not User, try Entreprise
                try {
                    Entreprise authenticatedEntreprise = entrepriseService.authenticate(email, password);
                    SessionManager.setCurrentEntreprise(authenticatedEntreprise);
                    
                    // Save credentials if "Remember Me" is checked
                    if (rememberMeCheckBox.isSelected()) {
                        saveCredentials(email, password, "ENTREPRISE");
                    } else {
                        clearSavedCredentials();
                    }

                    try {
                        redirectBasedOnRole(authenticatedEntreprise.getRoleId(), null);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                } catch (IllegalArgumentException e2) {
                    if(userService.emailExists(email) && !userService.isUserVerified(email)){
                        emailErrorLabel.setVisible(false);
                        passwordErrorLabel.setText("Account not verified. Please check your email for verification link.");
                        passwordErrorLabel.setVisible(true);
                    }
                    else{
                        emailErrorLabel.setVisible(false);
                        passwordErrorLabel.setText("Incorrect email or password");
                        passwordErrorLabel.setVisible(true);
                    }
                }
            }
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleSignUp() {
        try {
            // Close current login window
            Stage currentStage = (Stage) loginButton.getScene().getWindow();
            currentStage.close();

            // Load SignUp view
            Parent root = FXMLLoader.load(getClass().getResource("/MainContainer.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Sign Up");  
            stage.show();
        } catch (IOException e) {
            showError("Failed to load sign up view");
            e.printStackTrace();
        }
    }


    private void redirectBasedOnRole(int roleId, User user) throws SQLException, IOException {
        String roleName;
        if (user != null) {
            roleName = userService.getRoleName(roleId);
        } else {
            roleName = entrepriseService.getRoleName(roleId);
        }
        
        if ("ROLE_ADMIN".equals(roleName)) {
            loadAdminDashboard();
        } else if ("ROLE_CLIENT".equals(roleName)) {
            loadClientDashboard(user);
        } else if ("ROLE_ENTREPRISE".equals(roleName)) {
            loadEntrepriseDashboard();
        } else {
            showError("Invalid user role");
        }
    }

    private void loadAdminDashboard() {
        try {
            // Close current login window
            Stage currentStage = (Stage) loginButton.getScene().getWindow();
            currentStage.close();

            // Load Admin Dashboard
            Parent root = FXMLLoader.load(getClass().getResource("/AdminDashboard.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Admin Dashboard");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showError("Failed to load admin dashboard");
            e.printStackTrace();
        }
    }

    private void loadEntrepriseDashboard() {
        try {
            // Close current login window
            Stage currentStage = (Stage) loginButton.getScene().getWindow();
            currentStage.close();

            // Load Enterprise Dashboard
            Parent root = FXMLLoader.load(getClass().getResource("/DashboardEntreprise.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Enterprise Dashboard");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showError("Failed to load enterprise dashboard");
            e.printStackTrace();
        }
    }

    private void loadClientDashboard(User user) {
        try {
            // Close current login window
            Stage currentStage = (Stage) loginButton.getScene().getWindow();
            currentStage.close();

            // Load Client Dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ClientDashboard.fxml"));
            Parent root = loader.load();
            
            // Set the current user in the dashboard controller
            ClientDashboardController controller = loader.getController();
            controller.setCurrentUser(user);
            
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Client Dashboard");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showError("Failed to load client dashboard");
            e.printStackTrace();
        }
    }

    private void handleForgotPassword() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Password Recovery");
        dialog.setHeaderText("Enter your registered email");
        dialog.setContentText("Email:");

        dialog.showAndWait().ifPresent(email -> {
            try {
                if (userService.emailExists(email)) {
                    userService.sendPasswordReset(email);
                    showAlert(Alert.AlertType.INFORMATION,
                            "Password Reset",
                            "A password reset link has been sent to your email.");
                } else {
                    showAlert(Alert.AlertType.ERROR,
                            "Error",
                            "Email doesn't exist in our system.");
                }
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR,
                        "Error",
                        "Password reset failed: " + e.getMessage());
            }
        });
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setStyle("-fx-text-fill: #d9534f;"); // Red color for errors
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}