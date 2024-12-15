package websocket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;

public class LocalWebSocketServerLauncher {

    private static final Logger logger = LogManager.getLogger(LocalWebSocketServerLauncher.class);

    public static void main(String[] args) {
        int port = 9070;
        LocalWebSocketServer localWebSocketServer = new LocalWebSocketServer(new InetSocketAddress("localhost", port));

        logger.info("Starting WebSocket server...");

        try {
            localWebSocketServer.start();
            logger.info("WebSocket server is listening on ws://localhost:" + port);
        } catch (Exception e) {
            logger.error("Error starting WebSocket server: " + e.getMessage());
        }
    }
}
