package org.example.pidev.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;


public class MainFX extends Application {

    private static String[] savedArgs;

    @Override
    public void start(Stage primaryStage) throws IOException{
        try {
            if (handleDeepLinks(primaryStage)) {
                return;
            }

            // 3. Default to login page
            System.out.println("No valid session found - loading login page");
            loadLoginPage(primaryStage);

        } catch (Exception e) {
            System.err.println("Application startup error: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("Startup Error", "Failed to initialize application");
            loadLoginPage(primaryStage);
        }
    }

    private boolean handleDeepLinks(Stage stage) {
        if (savedArgs == null || savedArgs.length == 0) {
            return false;
        }

        try {
            String uriString = savedArgs[0];
            if (uriString.startsWith("autolink://reset-password")) {
                URI uri = new URI(uriString);
                String query = uri.getQuery();
                Map<String, String> params = new HashMap<>();
                
                if (query != null) {
                    for (String pair : query.split("&")) {
                        int idx = pair.indexOf("=");
                        params.put(pair.substring(0, idx), pair.substring(idx + 1));
                    }
                }

                String token = params.get("token");
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResetPassword.fxml"));
                Parent root = loader.load();
                
                Object controller = loader.getController();
                if (controller instanceof TokenReceiver) {
                    ((TokenReceiver) controller).setToken(token);
                }
                
                stage.setTitle("Reset Password");
                stage.setScene(new Scene(root));
                stage.setResizable(false);
                stage.show();
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error handling deep link: " + e.getMessage());
        }
        return false;
    }

    private void loadLoginPage(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/DashboardLogin.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("Login");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.show();
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