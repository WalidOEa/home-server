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

//TODO: If Client A created a lobby and Client B joins it as the host then Client A joins it they both see START button
public class LocalWebSocketServer extends WebSocketServer {

    private static final Logger logger = LogManager.getLogger(LocalWebSocketServer.class);

    private Map<String, Set<Player>> channels = new HashMap<>();

    private Map<WebSocket, String> clientChannel = new HashMap<>();

    private final ScoreDatabase scoreDatabase;

    public LocalWebSocketServer(InetSocketAddress address) {
        super(address);

        this.scoreDatabase = new ScoreDatabase();
        this.scoreDatabase.populateDatabase();
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

                if (wasHost && !players.isEmpty()) promoteNewHost(channelName);

                broadcastUsers(channelName);
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

        if (message.startsWith("JOIN")) {
            String[] parts = message.split(" ", 2);
            String channelName = parts[1];

            if (lobbyExists(channelName)) {
                joinChannel(channelName, conn);

                logger.info(conn + " joined channel " + channelName);

                conn.send("JOIN");
                broadcastUsers(channelName);
            } else {
                logger.warn(channelName + " does not exist");

                conn.send("ERROR Channel " + channelName + " does not exist.");
            }
        }

        if (message.startsWith("PART")) {
            String channelName = clientChannel.get(conn);

            if (channelName != null) {
                Set<Player> players = channels.get(channelName);

                if (players != null) {
                    players.removeIf(player -> player.getConn().equals(conn));

                    boolean wasHost = players.stream().noneMatch(Player::isHost);

                    if (wasHost && !players.isEmpty()) {
                        promoteNewHost(channelName);
                    }

                    broadcastUsers(channelName);

                    clientChannel.remove(conn);
                    logger.info(conn.getRemoteSocketAddress() + " left channel " + channelName);
                }
            } else {
                logger.warn("Client " + conn.getRemoteSocketAddress() + " is not in any channel");
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

        if (message.startsWith("MSG")) {
            String channelName = clientChannel.get(conn);

            if (channelName != null) {
                String chatMessage = message.substring(4);

                Player sender = getPlayerByConn(conn);
                String username = sender != null ? sender.getUsername() : "Unknown";

                String formattedMessage = username + ": " + chatMessage;

                logger.info(formattedMessage);

                broadcastMessage(channelName, formattedMessage, conn);
            }
        }

        if (message.startsWith("NICK")) {
            String[] parts = message.split(" ", 2);

            if (parts.length > 1) {
                String newNick = parts[1].trim();
                String channelName = clientChannel.get(conn);

                if (channelName != null) {
                    Player player = getPlayerByConn(conn);

                    if (player != null) {
                        player.setUsername(newNick);

                        logger.info(conn.getRemoteSocketAddress() + " changed nickname to " + newNick);

                        conn.send("NICK " + newNick);

                        broadcastUsers(channelName);
                    }
                }
            }
        }

        if (message.startsWith("START")) {
            String channelName = clientChannel.get(conn);

            if (channelName != null) {
                Set<Player> playersInChannel = channels.get(channelName);

                if (playersInChannel != null) {
                    logger.info("Broadcasting START message to channel " + channelName);

                    for (Player player : playersInChannel) {
                        player.getConn().send("START");
                    }
                } else {
                    logger.warn("Channel " + channelName + " does not exist or has no players");

                    conn.send("ERROR Channel " + channelName + " does not exist or is empty");
                }
            } else {
                logger.warn("Client " + conn.getRemoteSocketAddress() + " attempted START but is not in a channel");
            }
        }

        if (message.startsWith("PIECE")) {
            String channelName = clientChannel.get(conn);

            if (channelName != null) {
                Set<Player> playersInChannel = channels.get(channelName);

                if (playersInChannel != null) {
                    int pieceValue = (int) (Math.random() * 15);

                    String pieceMessage = "PIECE " + pieceValue;

                    for (Player player : playersInChannel) {
                        player.getConn().send(pieceMessage);
                    }

                    logger.info("Broadcasted PIECE " + pieceValue + " to channel " + channelName);
                }
            }
        }

        if (message.startsWith("BOARD")) {
            String channelName = clientChannel.get(conn);

            if (channelName != null) {
                handleBoardUpdate(channelName, conn, message.substring(6));
            }
        }

        if (message.startsWith("LIVES")) {
            String channelName = clientChannel.get(conn);

            if (channelName != null) {
                handleLivesUpdate(channelName, conn, message.substring(6));
            }
        }

        if (message.startsWith("SCORE")) {
            broadcastScore(conn, message.substring(7).trim());
        }

        if (message.startsWith("HISCORE")) {
            String[] parts = message.replace("HISCORE", "").split(":");
            String name = parts[0];
            int score = Integer.parseInt(parts[1]);

            scoreDatabase.upsertScore(name, score);
            conn.send("NEWSCORE " + name + ":" + score);
        }

        if (message.startsWith("HISCORES")) {
            String scores = scoreDatabase.getScores();
            conn.send("HISCORES " + scores.trim());
        }

        if (message.startsWith("DIE")) {
            removeConnectionFromChannel(conn);
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
        return channels.containsKey(lobbyName); // Check if the lobby name exists in the map
    }

    // Create a new lobby (initialize a new set for that channel)
    private void createLobby(String lobbyName, WebSocket conn) {
        channels.putIfAbsent(lobbyName, new HashSet<>());

        logger.info("Lobby created successfully (" + lobbyName + ")");
    }

    private void promoteNewHost(String channelName) {
        Set<Player> players = channels.get(channelName);

        if (players != null && !players.isEmpty()) {
            Player newHost = players.iterator().next();

            for (Player player : players) {
                player.setHost(false);
            }

            newHost.setHost(true);
            newHost.getConn().send("HOST");

            logger.info(newHost.getUsername() + " is now the host of " + channelName);

            broadcastUsers(channelName);
        }
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

    private Player getPlayerByConn(WebSocket conn) {
        for (Set<Player> players : channels.values()) {
            for (Player player : players) {
                if (player.getConn().equals(conn)) return player;
            }
        }

        return null;
    }

    private void broadcastUsers(String channelName) {
        Set<Player> usersInChannel = channels.get(channelName);

        if (usersInChannel != null) {
            StringBuilder usersList = new StringBuilder();

            for (Player player : usersInChannel) {
                usersList.append(player.getUsername());

                if (player.isHost()) usersList.append(" (Host)");
                usersList.append("\n");
            }

            for (Player player : usersInChannel) {
                player.getConn().send("USERS " + usersList);
            }
        } else {
            logger.warn("Channel " + channelName + " does not exist");
        }
    }

    private void broadcastMessage(String channelName, String message, WebSocket sender) {
        Set<Player> usersInChannel = channels.get(channelName);

        if (usersInChannel != null) {
            for (Player player : usersInChannel) {
                player.getConn().send("MSG " + message);
            }
        } else {
            sender.send("ERROR Channel " + channelName + " does not exist");
        }
    }

    private void broadcastScore(WebSocket conn, String scores) {
        String channel = clientChannel.get(conn);
        Set<Player> players = channels.get(channel);

        logger.info("Broadcasting scores to channel " + channel + ": " + scores);

        for (Player player : players) {
            WebSocket playerConn = player.getConn();

            if (playerConn != conn) playerConn.send("SCORES " + scores);
        }
    }

    @Deprecated
    private void handleBoardUpdate(String channelName, WebSocket conn, String boardState) {
        Set<Player> playersInChannel = channels.get(channelName);

        if (playersInChannel != null) {
            for (Player player : playersInChannel) {
                if (!player.getConn().equals(conn)) player.getConn().send("BOARD " + boardState);
            }
        }
        logger.info("Broadcasted BOARD state to channel " + channelName);
    }

    private void handleLivesUpdate(String channelName, WebSocket conn, String lives) {
        Set<Player> usersInChannel = channels.get(channelName);

        if (usersInChannel != null) {
            for (Player player : usersInChannel) {
                if (!player.getConn().equals(conn)) player.getConn().send("LIVES " + lives);
            }
        }
        logger.info("Broadcasted LIVES update to channel " + channelName);
    }

    private void removeConnectionFromChannel(WebSocket conn) {
        String channel = clientChannel.get(conn);

        if (channel != null) {
            Set<Player> players = channels.get(channel);

            if (players != null) {
                players.removeIf(player -> player.getConn().equals(conn));

                logger.info("Removed connection from channel " + channel);

                if (players.isEmpty()) {
                    channels.remove(channel);

                    logger.info("Channel " + channel + " is empty and has been removed");
                }
            }

            clientChannel.remove(conn);
        } else {
            logger.error("Connection not associated with any channel");
        }
    }
}