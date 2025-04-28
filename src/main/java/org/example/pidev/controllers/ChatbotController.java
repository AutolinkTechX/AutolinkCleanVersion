package org.example.pidev.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.example.pidev.entities.User;
import org.example.pidev.utils.DeepSeekAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class ChatbotController {
    @FXML private VBox messageContainer;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField userInput;
    private final DeepSeekAPI deepSeekAPI = new DeepSeekAPI();
    private final List<String> conversationHistory = new ArrayList<>();

    @FXML
    private void initialize() {
        // Configuration du défilement automatique
        messageContainer.heightProperty().addListener((obs, old, newVal) ->
                scrollPane.setVvalue(1.0)
        );

        appendInitialMessage();
    }


    private void appendInitialMessage() {
        String welcomeMessage = "Bonjour ! Je suis l'assistant Autlink. Voici comment je peux vous aider :\n"
                + "- Évaluer vos pièces automobiles usagées\n"
                + "- Vous guider dans le processus de recyclage ou de reconditionnement\n"
                + "- Vous aider à déposer vos matériaux à recycler\n"
                + "- Vous accompagner pour l'achat de pièces recyclées de qualité\n"
                + "- Répondre à vos questions sur nos engagements environnementaux";

        appendMessage("Assistant Autlink", welcomeMessage);
    }


    @FXML
    private void handleSend() {
        String message = userInput.getText().trim();
        if (!message.isEmpty()) {
            addUserMessage(message);
            processAIResponse(message);
        }
    }

    private void addUserMessage(String message) {
        appendMessage("Vous", message);
        conversationHistory.add("User: " + message);
        userInput.clear();
        showTypingIndicator();
    }

    private void processAIResponse(String message) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            String apiResponse = deepSeekAPI.getResponseFromDeepSeek(message);
            Platform.runLater(() -> {
                removeTypingIndicator();
                appendMessage("Assistant", apiResponse);
                conversationHistory.add("Assistant: " + apiResponse);
            });
        });
        executor.shutdown();
    }

    private void appendMessage(String sender, String message) {
        Platform.runLater(() -> {
            HBox container = createMessageContainer(sender);
            Label messageLabel = createMessageLabel(sender, message);

            container.getChildren().add(messageLabel);
            messageContainer.getChildren().add(container);
        });
    }

    private HBox createMessageContainer(String sender) {
        HBox container = new HBox();
        container.setAlignment(sender.equals("Vous") ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        container.setPadding(new Insets(5));
        return container;
    }

    private Label createMessageLabel(String sender, String message) {
        Label label = new Label(message);
        label.setWrapText(true);
        label.setMaxWidth(300);
        label.setPadding(new Insets(10));
        label.getStyleClass().addAll(
                "message-bubble",
                sender.equals("Vous") ? "user-message" : "assistant-message"
        );
        return label;
    }

    private void showTypingIndicator() {
        Platform.runLater(() -> {
            HBox typingContainer = new HBox(5);
            typingContainer.setAlignment(Pos.CENTER_LEFT);
            typingContainer.getStyleClass().add("typing-indicator");

            IntStream.range(0, 3).forEach(i -> {
                Circle dot = new Circle(3);
                dot.getStyleClass().add("typing-dot");
                typingContainer.getChildren().add(dot);
            });

            messageContainer.getChildren().add(typingContainer);
        });
    }

    private void removeTypingIndicator() {
        Platform.runLater(() -> {
            messageContainer.getChildren().removeIf(node ->
                    node.getStyleClass().contains("typing-indicator"));
        });
    }

}