package websocket;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LocalWebSocketServer extends WebSocketServer {

    private static final Logger logger = LogManager.getLogger(LocalWebSocketServer.class);

    private Set<String> activeLobbies = new HashSet<>();

    public LocalWebSocketServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        logger.info("New connection: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        if (reason.isEmpty()) logger.info("Connection closed: " + conn.getRemoteSocketAddress() + " (Exit Code " + code + ")");
        else logger.info("Connection closed: " + conn.getRemoteSocketAddress() + " (Exit Code " + code + ") Reason: " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        logger.info("Message received -> " + message);

        if (message.startsWith("Marco")) {
            logger.info("Sending message -> Polo");

            conn.send("Polo");
        }

        if (message.startsWith("CREATE")) {
            String[] parts = message.split(" ");
            String lobbyName = parts[1];

            if (lobbyExists(lobbyName)) {
                logger.warn("Attempt at lobby creation failed. " + lobbyName + " already exists");

                conn.send("ERROR " + lobbyName + " already exists");
            } else {
                createLobby(lobbyName);

                logger.info("Lobby created (" + lobbyName + ")");

                conn.send(lobbyName + " created successfully");
            }
        }

        if (message.startsWith("LIST")) {
            if (!activeLobbies.isEmpty()) {
                String channels = String.join("\n", activeLobbies) + "\n";

                logger.info("Current channels: " + channels);

                conn.send("CHANNELS " + channels);
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception e) {
        logger.error("Error: " + e.getMessage());

        if (conn != null) {
            logger.warn("Connection: " + conn.getRemoteSocketAddress());
        }
    }

    @Override
    public void onStart() {
        logger.info("WebSocket server started successfully");
    }

    private boolean lobbyExists(String lobbyName) {
        return activeLobbies.contains(lobbyName);
    }

    private void createLobby(String lobbyName) {
        activeLobbies.add(lobbyName);
    }
}
