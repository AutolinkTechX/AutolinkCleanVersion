package org.example.pidev.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class MainFX extends Application {

    private static String[] savedArgs;
    private static Map<String, String> queryParameters = new HashMap<>();

    @Override
    public void start(Stage primaryStage) {
        try {
            // Check if we have a deep link to handle
            boolean isResetPasswordLink = false;
            String token = null;
            
            // Handle parameters passed from main (for standard launch)
            if (savedArgs != null && savedArgs.length > 0) {
                String uriString = savedArgs[0];
                if (uriString.startsWith("autolink://")) {
                    try {
                        URI uri = new URI(uriString);
                        if ("reset-password".equals(uri.getHost())) {
                            isResetPasswordLink = true;
                            // Parse query parameters
                            String query = uri.getQuery();
                            if (query != null) {
                                String[] pairs = query.split("&");
                                for (String pair : pairs) {
                                    int idx = pair.indexOf("=");
                                    queryParameters.put(pair.substring(0, idx), pair.substring(idx + 1));
                                }
                            }
                            token = queryParameters.get("token");
                        }
                    } catch (URISyntaxException e) {
                        System.err.println("Error parsing URI: " + e.getMessage());
                    }
                }
            }

            FXMLLoader loader;
            if (isResetPasswordLink) {
                // Load the ResetPassword.fxml if we have a reset-password link
                loader = new FXMLLoader(getClass().getResource("/ResetPassword.fxml"));
                Parent root = loader.load();
                
                // Pass the token to the controller if needed
                Object controller = loader.getController();
                if (controller instanceof TokenReceiver) {
                    ((TokenReceiver) controller).setToken(token);
                }
                
                primaryStage.setTitle("Reset Password");
                primaryStage.setScene(new Scene(root));
            } else {
                // Default to login page
                loader = new FXMLLoader(getClass().getResource("/DashboardLogin.fxml"));
                Parent root = loader.load();
                primaryStage.setTitle("Login");
                primaryStage.setScene(new Scene(root));
            }
            
            primaryStage.setResizable(false);
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Error loading FXML file: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("Application Error", "Failed to load the application interface. Please check the logs for details.");
        }
    }

    @Override
    public void init() throws Exception {
        super.init();
        // Get parameters passed to the application
        Parameters parameters = getParameters();
        savedArgs = parameters.getRaw().toArray(new String[0]);
    }

    public static void main(String[] args) {
        savedArgs = args;
        try {
            launch(args);
        } catch (Exception e) {
            System.err.println("Error launching application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Interface for controllers that can receive tokens
    public interface TokenReceiver {
        void setToken(String token);
    }
}