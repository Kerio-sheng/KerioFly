package com.kerio.keriofly.database;

import com.kerio.keriofly.KerioFly;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.UUID;

public class DatabaseManager {
    
    private final KerioFly plugin;
    private Connection connection;
    private String tablePrefix;
    private boolean enabled;
    
    public DatabaseManager(KerioFly plugin) {
        this.plugin = plugin;
    }
    
    public void connect() {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        enabled = config.getBoolean("database.enabled", true);
        
        if (!enabled) {
            plugin.getLogger().info("資料庫功能已停用，將使用本地儲存");
            return;
        }
        
        String host = config.getString("database.host");
        int port = config.getInt("database.port");
        String database = config.getString("database.database");
        String username = config.getString("database.username");
        String password = config.getString("database.password");
        tablePrefix = config.getString("database.table-prefix", "keriofly_");
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";
            connection = DriverManager.getConnection(url, username, password);
            
            createTables();
            plugin.getLogger().info("資料庫連接成功！");
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().severe("資料庫連接失敗: " + e.getMessage());
            enabled = false;
        }
    }
    
    private void createTables() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "players (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "name VARCHAR(16) NOT NULL," +
                "fly_time BIGINT NOT NULL DEFAULT 0," +
                "fly_enabled BOOLEAN NOT NULL DEFAULT FALSE," +
                "fly_points BIGINT NOT NULL DEFAULT 0," +
                "last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }
    
    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("資料庫連接已關閉");
            } catch (SQLException e) {
                plugin.getLogger().severe("關閉資料庫連接時發生錯誤: " + e.getMessage());
            }
        }
    }
    
    public void savePlayerData(UUID uuid, String name, long flyTime, boolean flyEnabled) {
        if (!enabled || connection == null) return;
        
        String sql = "INSERT INTO " + tablePrefix + "players (uuid, name, fly_time, fly_enabled) " +
                "VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE name=?, fly_time=?, fly_enabled=?";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setLong(3, flyTime);
            ps.setBoolean(4, flyEnabled);
            ps.setString(5, name);
            ps.setLong(6, flyTime);
            ps.setBoolean(7, flyEnabled);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("保存玩家數據時發生錯誤: " + e.getMessage());
        }
    }
    
    public PlayerData loadPlayerData(UUID uuid) {
        if (!enabled || connection == null) return null;
        
        String sql = "SELECT * FROM " + tablePrefix + "players WHERE uuid=?";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return new PlayerData(
                    uuid,
                    rs.getString("name"),
                    rs.getLong("fly_time"),
                    rs.getBoolean("fly_enabled")
                );
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("加載玩家數據時發生錯誤: " + e.getMessage());
        }
        
        return null;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public static class PlayerData {
        private final UUID uuid;
        private final String name;
        private final long flyTime;
        private final boolean flyEnabled;
        
        public PlayerData(UUID uuid, String name, long flyTime, boolean flyEnabled) {
            this.uuid = uuid;
            this.name = name;
            this.flyTime = flyTime;
            this.flyEnabled = flyEnabled;
        }
        
        public UUID getUuid() { return uuid; }
        public String getName() { return name; }
        public long getFlyTime() { return flyTime; }
        public boolean isFlyEnabled() { return flyEnabled; }
    }
    
    public void savePlayerPoints(UUID uuid, long points) {
        if (!enabled || connection == null) return;
        
        String sql = "UPDATE " + tablePrefix + "players SET fly_points=? WHERE uuid=?";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, points);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("保存飛行點數時發生錯誤: " + e.getMessage());
        }
    }
    
    public Long loadPlayerPoints(UUID uuid) {
        if (!enabled || connection == null) return null;
        
        String sql = "SELECT fly_points FROM " + tablePrefix + "players WHERE uuid=?";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getLong("fly_points");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("加載飛行點數時發生錯誤: " + e.getMessage());
        }
        
        return null;
    }
}