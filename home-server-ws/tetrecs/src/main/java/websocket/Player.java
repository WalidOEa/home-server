package websocket;

import org.java_websocket.WebSocket;

public class Player {
    private String username;
    private WebSocket conn;
    private boolean host;

    private int score;

    private int lives;

    public Player(String username, WebSocket conn, boolean host, int score, int lives) {
        this.username = username;
        this.conn = conn;
        this.host = host;
        this.score = score;
        this.lives = lives;
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

    public int getScore() {
        return score;
    }

    public int getLives() { return lives; }

    public void setUsername(String newUsername) {
        this.username = newUsername;
    }

    public void setConn(WebSocket newConn) {
        this.conn = newConn;
    }

    public void setHost(boolean host) {
        this.host = host;
    }

    public void setScore(int score) { this.score = score; }

    public void setLives(int lives) { this.lives = lives; }
}
