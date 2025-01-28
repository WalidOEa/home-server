package websocket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

public class LocalWebSocketServerLauncher {

    private static final Logger logger = LogManager.getLogger(LocalWebSocketServerLauncher.class);

    public static void main(String[] args) {
        String host = "0.0.0.0";
        int port = 8887;
        WebSocketServer localWebSocketServer = new LocalWebSocketServer(new InetSocketAddress(host, port));

        logger.info("Starting WebSocket server...");

        localWebSocketServer.run();
    }
}
