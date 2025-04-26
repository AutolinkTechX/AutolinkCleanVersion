package org.example.pidev.websocket;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class NotificationServer extends WebSocketServer {

    private static final Set<WebSocket> connections = Collections.synchronizedSet(new HashSet<>());

    public NotificationServer(InetSocketAddress address) {
        super(address);
    }

  /*  @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        System.out.println("✅ Nouveau client connecté : " + conn.getRemoteSocketAddress());
    }*/

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        System.out.println("✅ Nouveau client connecté : " + conn.getRemoteSocketAddress());

        // Envoi d'un message de test à ce client
        conn.send("🔔 Notification test envoyée !");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        System.out.println("🚪 Client déconnecté : " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("📨 Message reçu : " + message);
        broadcast(message); // ✅ Envoie à tous les clients connectés
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("❌ Erreur WebSocket : " + ex.getMessage());
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("✅ Serveur WebSocket prêt !");
    }
}
