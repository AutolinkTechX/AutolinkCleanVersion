package org.example.pidev.utils;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class WebSocketNotifier {
    public static void sendNotification(String message) {
        try {
            URI uri = new URI("ws://localhost:9092"); // Endpoint spécifique
          //  URI uri = new URI("ws://localhost:9092"); // même port que ton serveur
            WebSocketClient tempClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("📤 Envoi de la notification WebSocket...");
                    send(message);
                    close();
                }


                @Override public void onMessage(String message) {}
                @Override public void onClose(int code, String reason, boolean remote) {}
                @Override public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };
            tempClient.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}

