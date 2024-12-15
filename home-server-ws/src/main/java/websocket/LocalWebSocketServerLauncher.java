package websocket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;

public class LocalWebSocketServerLauncher {

    private static final Logger logger = LogManager.getLogger(LocalWebSocketServer.class);

    public static void main(String[] args) {
        int port = 9070;
        LocalWebSocketServer localWebSocketServer = new LocalWebSocketServer(new InetSocketAddress("localhost", port));

        logger.info("Starting WebSocket server...");

        localWebSocketServer.start();

        logger.info("WebSocket server is listening on ws://localhost:" + port);
    }
}
