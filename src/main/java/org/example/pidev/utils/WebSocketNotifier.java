package org.example.pidev.utils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class WebSocketNotifier {

    public interface ConnectionStatusListener {
        void onConnectionFailed();
    }

    private static ConnectionStatusListener listener;

    public static void setConnectionStatusListener(ConnectionStatusListener listener) {
        WebSocketNotifier.listener = listener;
    }

    public static void sendNotification(String message) {
        try {
            URI uri = new URI("ws://localhost:9092");

            WebSocketClient tempClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("✅ Connexion WebSocket établie");
                    send(message);
                    System.out.println("📤 Notification envoyée : " + message);
                    close(); // fermer la connexion après envoi
                }

                @Override public void onMessage(String message) {}

                @Override public void onClose(int code, String reason, boolean remote) {
                    System.out.println("🔌 Connexion WebSocket fermée");
                }

                @Override public void onError(Exception ex) {
                    System.err.println("❌ Erreur WebSocket : " + ex.getMessage());
                    ex.printStackTrace();
                    if (listener != null) {
                        listener.onConnectionFailed();
                    }
                }
            };

            // Connexion bloquante (attend que la connexion soit établie ou échoue)
            if (tempClient.connectBlocking()) {
                System.out.println("🔄 Connexion réussie, envoi de la notification...");
            } else {
                System.out.println("❌ Échec de connexion WebSocket (serveur injoignable)");
                if (listener != null) {
                    listener.onConnectionFailed();
                }
            }

        } catch (URISyntaxException e) {
            System.err.println("❌ URI invalide : " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("❌ Connexion interrompue : " + e.getMessage());
        }
    }
}
