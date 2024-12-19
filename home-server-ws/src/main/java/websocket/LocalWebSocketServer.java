package websocket;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LocalWebSocketServer extends WebSocketServer {

    private static final Logger logger = LogManager.getLogger(LocalWebSocketServer.class);

    // A map to store the channels and their associated WebSocket connections
    private Map<String, Set<WebSocket>> channels = new HashMap<>();

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

        // Handle CREATE command (create a new lobby/channel)
        if (message.startsWith("CREATE")) {
            String[] parts = message.split(" ");
            String lobbyName = parts[1];

            if (lobbyExists(lobbyName)) {
                logger.warn("Attempt at lobby creation failed. " + lobbyName + " already exists");

                conn.send("ERROR " + lobbyName + " already exists");
            } else {
                createLobby(lobbyName);
                joinChannel(lobbyName, conn); // Join the channel as soon as it's created

                logger.info("Lobby created successfully (" + lobbyName + ")");
            }
        }

        // Handle LIST command (list all available channels)
        if (message.startsWith("LIST")) {
            if (!channels.isEmpty()) {
                // Concatenate all the channel names with newlines
                String channelsList = String.join("\n", channels.keySet()) + "\n";

                logger.info("Current channels: " + channelsList);

                conn.send("CHANNELS " + channelsList);
            } else {
                conn.send("CHANNELS No channels available.");
            }
        }

        // Handle JOIN command (join an existing lobby/channel)
        if (message.startsWith("JOIN")) {
            // Extract the channel name
            String[] parts = message.split(" ", 2);
            String channelName = parts[1];

            if (lobbyExists(channelName)) {
                joinChannel(channelName, conn);

                logger.info(conn + " joined channel " + channelName);

                conn.send("JOIN");
            } else {
                logger.warn(channelName + " does not exist");

                conn.send("ERROR Channel " + channelName + " does not exist.");
            }
        }

        // TODO: NO CHANNEL NAME HERE
        if (message.startsWith("USERS")) {
            String channelName = message.split(" ")[1];

            logger.info("Retrieving user list for " + channelName);

            sendUsersInChannel(conn, channelName);
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

    // Check if the lobby exists
    private boolean lobbyExists(String lobbyName) {
        return channels.containsKey(lobbyName); // Check if the lobby name exists in the map
    }

    // Create a new lobby (initialize a new set for that channel)
    private void createLobby(String lobbyName) {
        channels.putIfAbsent(lobbyName, new HashSet<>());
    }

    // Add the client to the specified channel
    private void joinChannel(String channelName, WebSocket conn) {
        channels.putIfAbsent(channelName, new HashSet<>());
        channels.get(channelName).add(conn);
    }

    private void sendUsersInChannel(WebSocket conn, String channelName) {
        Set<WebSocket> usersInChannel = channels.get(channelName);

        if (usersInChannel != null) {
            StringBuilder usersList = new StringBuilder();

            for (WebSocket user : usersInChannel) {
                usersList.append(user.getRemoteSocketAddress().toString()).append("\n");
            }

            conn.send("USERS " + usersList);
        }
    }
}