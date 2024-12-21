package websocket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;

public class ScoreDatabase {

    private static final Logger logger = LogManager.getLogger(ScoreDatabase.class);

    private static final String DATABASE_URL = "jdbc:sqlite:websocket_tetrecs_scores.db";

    public ScoreDatabase() {
        createTable();
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS scores (\n"
                + "    name TEXT NOT NULL,\n"
                + "    score INTEGER\n"
                + ");";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();

            logger.info("Database scores.db created successfully");
        } catch (SQLException e) {
            logger.error("Failed to initialise database scores.db, ", e);
        }
    }

    private Connection connect() {
        try {
            return DriverManager.getConnection(DATABASE_URL);
        } catch (SQLException e) {
            logger.error("Failed to connect to database scores.db, ", e);
            return null;
        }
    }

    public void populateDatabase() {
        logger.info("Populating newly created database");

        String sql = "INSERT INTO scores(name, score) VALUES(?, ?)";

        try (Connection conn = connect();
             PreparedStatement preparedStmt = conn.prepareStatement(sql)) {

            for (int i = 10; i > 0; i--) {
                String playerName = "Player" + (11 - i);
                int score = i * 1000;

                preparedStmt.setString(1, playerName);
                preparedStmt.setInt(2, score);

                preparedStmt.executeUpdate();
            }

            logger.info("Successfully populated database");
        } catch (SQLException e) {
            logger.error("Failed to populate scores into database, ", e);
        }
    }

    public void upsertScore(String name, int score) {
        String selectSql = "SELECT score FROM scores WHERE name = ?";
        String updateSql = "UPDATE scores SET score = ? WHERE name = ?";
        String insertSql = "INSERT INTO scores (name, score) VALUES (?, ?)";

        try (Connection conn = connect();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql);
             PreparedStatement updateStmt = conn.prepareStatement(updateSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

            selectStmt.setString(1, name);
            ResultSet resultSet = selectStmt.executeQuery();

            if (resultSet.next()) {
                int currentScore = resultSet.getInt("score");

                if (score > currentScore) {
                    updateStmt.setInt(1, score);
                    updateStmt.setString(2, name);
                    updateStmt.executeUpdate();

                    logger.info("Updated score for " + name + " to " + score);
                }
            } else {
                insertStmt.setString(1, name);
                insertStmt.setInt(2, score);
                insertStmt.executeUpdate();

                logger.info("Inserted new score for " + name + ": " + score);
            }
        } catch (SQLException e) {
            logger.error("Failed to upsert score for " + name, e);
        }
    }

    public String getScores() {
        String sql = "SELECT name, score FROM scores ORDER BY score DESC";
        StringBuilder scores = new StringBuilder();

        try (Connection conn = connect();
             PreparedStatement preparedStmt = conn.prepareStatement(sql);
             ResultSet resultSet = preparedStmt.executeQuery()) {

            while (resultSet.next()) {
                scores.append(resultSet.getString("name")).append(":").append(resultSet.getInt("score")).append("\n");
            }
        } catch (SQLException e) {
            logger.error("Failed to retrieve scores from database, " + e);
        }

        return scores.toString();
    }
}
