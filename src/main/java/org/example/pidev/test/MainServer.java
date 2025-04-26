package org.example.pidev.test;

import org.example.pidev.websocket.NotificationServer;

import java.net.InetSocketAddress;

public class MainServer {

    public static void main(String[] args) {
        try {
            NotificationServer server = new NotificationServer(new InetSocketAddress("localhost", 9092));
            server.start();
            System.out.println("✅ Serveur WebSocket démarré sur ws://localhost:9092");
        } catch (Exception e) {
            System.err.println("❌ Erreur de démarrage du serveur WebSocket : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
