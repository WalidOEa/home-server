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

    private Map<String, Set<Player>> channels = new HashMap<>();

    private Map<WebSocket, String> clientChannel = new HashMap<>();

    public LocalWebSocketServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        logger.info("New connection: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String channelName = clientChannel.remove(conn);

        if (channelName != null) {
            Set<Player> players = channels.get(channelName);

            if (players != null) {
                players.removeIf(player -> player.getConn().equals(conn));

                boolean wasHost = players.stream().noneMatch(Player::isHost);

                if (wasHost && !players.isEmpty()) {
                    Player newHost = players.iterator().next();
                    newHost.setHost(true);
                    newHost.getConn().send("HOST");

                    logger.info(newHost.getUsername() + " is now the host of " + channelName);
                }
            }
        }

        logger.info("Connection closed: " + conn.getRemoteSocketAddress() + " (Exit Code " + code + ")");
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
                createLobby(lobbyName, conn);

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

        if (message.startsWith("USERS")) {
            String channelName = clientChannel.get(conn);

            if (channelName != null) {
                logger.info("Retrieving user list for channel " + channelName);

                sendUsersInChannel(conn, channelName);
            } else {
                logger.warn("Client " + conn.getRemoteSocketAddress() + " requested USERS but is not in any channel");
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

    // Check if the lobby exists
    private boolean lobbyExists(String lobbyName) {
        return channels.containsKey(lobbyName); // Check if the lobby name exists in the map
    }

    // Create a new lobby (initialize a new set for that channel)
    private void createLobby(String lobbyName, WebSocket conn) {
        channels.putIfAbsent(lobbyName, new HashSet<>());

        logger.info("Lobby created successfully (" + lobbyName + ")");
    }

    // Add the client to the specified channel
    private void joinChannel(String channelName, WebSocket conn) {
        channels.putIfAbsent(channelName, new HashSet<>());

        // Check if conn is already in channelName
        if (clientChannel.containsKey(conn) && clientChannel.get(conn).equals(channelName)) {

            conn.send("ERROR already in channel " + channelName);

            logger.warn(conn.getRemoteSocketAddress() + " attempted to join channel " + channelName + " but is already in it");
            return;
        }

        String username = "Player" + (channels.get(channelName).size() + 1);
        Player newPlayer = new Player(username, conn, false);

        Set<Player> channelPlayers = channels.get(channelName);
        if (channelPlayers.size() == 0) {
            newPlayer.setHost(true);

            conn.send("HOST");

            logger.info(newPlayer.getUsername() + " is now the host of " + channelName);
        }

        channels.get(channelName).add(newPlayer);

        // Track the connections current channel
        clientChannel.put(conn, channelName);

        logger.info(conn.getRemoteSocketAddress() + " joined channel " + channelName + " as " + username);
    }

    private void sendUsersInChannel(WebSocket conn, String channelName) {
        Set<Player> usersInChannel = channels.get(channelName);

        if (usersInChannel != null) {
            StringBuilder usersList = new StringBuilder();

            for (Player player : usersInChannel) {
                usersList.append(player.getUsername());

                if (player.isHost()) usersList.append(" (Host)");
                usersList.append("\n");
            }

            conn.send("USERS " + usersList);
        } else {
            conn.send("ERROR Channel " + channelName + " does not exist.");
        }
    }
}