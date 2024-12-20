package websocket;

import org.java_websocket.WebSocket;

public class Player {
    private String username;
    private WebSocket conn;
    private boolean host;

    public Player(String username, WebSocket conn, boolean host) {
        this.username = username;
        this.conn = conn;
        this.host = host;
    }

    public String getUsername() {
        return username;
    }

    public WebSocket getConn() {
        return conn;
    }

    public boolean isHost() {
        return host;
    }

    public void setUsername(String newUsername) {
        this.username = newUsername;
    }

    public void setConn(WebSocket newConn) {
        this.conn = newConn;
    }

    public void setHost(boolean host) {
        this.host = host;
    }
}
