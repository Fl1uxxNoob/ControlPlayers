package net.fliuxx.controlPlayers.manager;

import net.fliuxx.controlPlayers.ControlPlayers;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private final ControlPlayers plugin;
    private Connection connection;
    private final String dbPath;

    public DatabaseManager(ControlPlayers plugin) {
        this.plugin = plugin;
        this.dbPath = plugin.getDataFolder().getAbsolutePath() + File.separator + "staff_commands.db";
    }

    public void initialize() {
        try {
            // Create the plugin folder if it doesn't exist
            plugin.getDataFolder().mkdirs();

            // Load the SQLite driver
            Class.forName("org.sqlite.JDBC");

            // Establish the SQLite database connection
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

            // Create the table if it doesn't exist
            createTable();

            plugin.getLogger().info("Database initialized successfully!");

        } catch (SQLException | ClassNotFoundException e) {
            plugin.getLogger().severe("Error initializing database: " + e.getMessage());
        }
    }

    private void createTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS staff_commands (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                discord_user_id TEXT NOT NULL,
                discord_username TEXT NOT NULL,
                command_type TEXT NOT NULL,
                target_player TEXT,
                duration TEXT,
                reason TEXT,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                server_response TEXT
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    public void logCommand(String discordUserId, String discordUsername, String commandType,
                           String targetPlayer, String duration, String reason, String serverResponse) {
        String sql = """
            INSERT INTO staff_commands
            (discord_user_id, discord_username, command_type, target_player, duration, reason, server_response)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, discordUserId);
            pstmt.setString(2, discordUsername);
            pstmt.setString(3, commandType);
            pstmt.setString(4, targetPlayer);
            pstmt.setString(5, duration);
            pstmt.setString(6, reason);
            pstmt.setString(7, serverResponse);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().warning("Error while logging the command: " + e.getMessage());
        }
    }

    public List<StaffCommandRecord> getCommandHistory(String discordUserId, int limit) {
        List<StaffCommandRecord> records = new ArrayList<>();
        String sql = """
            SELECT * FROM staff_commands
            WHERE discord_user_id = ?
            ORDER BY timestamp DESC
            LIMIT ?
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, discordUserId);
            pstmt.setInt(2, limit);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                StaffCommandRecord record = new StaffCommandRecord(
                        rs.getInt("id"),
                        rs.getString("discord_user_id"),
                        rs.getString("discord_username"),
                        rs.getString("command_type"),
                        rs.getString("target_player"),
                        rs.getString("duration"),
                        rs.getString("reason"),
                        rs.getString("timestamp"),
                        rs.getString("server_response")
                );
                records.add(record);
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("Error retrieving command history: " + e.getMessage());
        }

        return records;
    }

    public List<StaffCommandRecord> getAllRecentCommands(int limit) {
        List<StaffCommandRecord> records = new ArrayList<>();
        String sql = """
            SELECT * FROM staff_commands
            ORDER BY timestamp DESC
            LIMIT ?
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                StaffCommandRecord record = new StaffCommandRecord(
                        rs.getInt("id"),
                        rs.getString("discord_user_id"),
                        rs.getString("discord_username"),
                        rs.getString("command_type"),
                        rs.getString("target_player"),
                        rs.getString("duration"),
                        rs.getString("reason"),
                        rs.getString("timestamp"),
                        rs.getString("server_response")
                );
                records.add(record);
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("Error retrieving recent commands: " + e.getMessage());
        }

        return records;
    }

    public int getTotalCommandsCount(String discordUserId) {
        String sql = "SELECT COUNT(*) FROM staff_commands WHERE discord_user_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, discordUserId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("Error counting commands: " + e.getMessage());
        }

        return 0;
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed.");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error closing database: " + e.getMessage());
        }
    }

    // Class representing a command record
    public static class StaffCommandRecord {
        private final int id;
        private final String discordUserId;
        private final String discordUsername;
        private final String commandType;
        private final String targetPlayer;
        private final String duration;
        private final String reason;
        private final String timestamp;
        private final String serverResponse;

        public StaffCommandRecord(int id, String discordUserId, String discordUsername,
                                  String commandType, String targetPlayer, String duration,
                                  String reason, String timestamp, String serverResponse) {
            this.id = id;
            this.discordUserId = discordUserId;
            this.discordUsername = discordUsername;
            this.commandType = commandType;
            this.targetPlayer = targetPlayer;
            this.duration = duration;
            this.reason = reason;
            this.timestamp = timestamp;
            this.serverResponse = serverResponse;
        }

        // Getters
        public int getId() { return id; }
        public String getDiscordUserId() { return discordUserId; }
        public String getDiscordUsername() { return discordUsername; }
        public String getCommandType() { return commandType; }
        public String getTargetPlayer() { return targetPlayer; }
        public String getDuration() { return duration; }
        public String getReason() { return reason; }
        public String getTimestamp() { return timestamp; }
        public String getServerResponse() { return serverResponse; }

        public String getFormattedTimestamp() {
            try {
                // Converts the database timestamp into a readable format
                return timestamp.replace("T", " ").substring(0, 19);
            } catch (Exception e) {
                return timestamp;
            }
        }
    }
}