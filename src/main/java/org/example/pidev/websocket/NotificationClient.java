package org.example.pidev.websocket;

import javafx.application.Platform;
import org.example.pidev.controllers.ShowAccords;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;


public class NotificationClient extends WebSocketClient {

    private ShowAccords controller; // Référence au contrôleur pour mettre à jour l'UI

    // Constructeur qui accepte la référence du contrôleur
    public NotificationClient(URI serverUri, ShowAccords controller) {
        super(serverUri);
        this.controller = controller;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Connected to WebSocket server");
        controller.onConnectionSuccess();

        // Log avant l'envoi du message
        String message = "Hello from client";  // Exemple de message
        System.out.println("📤 Envoi du message au serveur : " + message);

        // Envoi du message
        send(message);

    }

    @Override
    public void onMessage(String message) {
        System.out.println("Message reçu : " + message);
        controller.updateNotification(); // Appelle bien la méthode du contrôleur
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Disconnected from WebSocket server");
        controller.onConnectionFailed();
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
        controller.onConnectionFailed();
    }

    public boolean isConnected() {
        return this.isOpen();
    }
   /* public static void main(String[] args) throws URISyntaxException {
        WebSocketClient client = new NotificationClient(new URI("ws://localhost:9090"));
        client.connect();
    }*/
}
