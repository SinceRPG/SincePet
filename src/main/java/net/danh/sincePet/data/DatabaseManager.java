package net.danh.sincePet.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.danh.sincePet.SincePet;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private final SincePet plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(SincePet plugin) {
        this.plugin = plugin;
        setupDataSource();
        // CHỈNH SỬA: Chạy tạo bảng bất đồng bộ để tránh lag server khi khởi động
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::createTables);
    }

    private void setupDataSource() {
        String type = plugin.getConfigFile().getString("database.type", "SQLITE");
        HikariConfig config = new HikariConfig();

        if (type.equalsIgnoreCase("MYSQL")) {
            config.setJdbcUrl("jdbc:mysql://" + plugin.getConfigFile().getString("database.host") + ":" +
                    plugin.getConfigFile().getString("database.port") + "/" +
                    plugin.getConfigFile().getString("database.database") + "?useSSL=false&autoReconnect=true&characterEncoding=UTF-8");
            config.setUsername(plugin.getConfigFile().getString("database.username"));
            config.setPassword(plugin.getConfigFile().getString("database.password"));
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        } else {
            File file = new File(plugin.getDataFolder(), "database.db");
            config.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
        }

        config.setPoolName("SincePet-Pool");
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(5000); // 5s timeout
        this.dataSource = new HikariDataSource(config);
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) throw new SQLException("DataSource is null");
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }

    private void createTables() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            String usersTable = getUsersTable();
// Trong hàm createTables(), sửa câu lệnh SQL:
            String sqlUsers = "CREATE TABLE IF NOT EXISTS " + usersTable + " (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "active_pet VARCHAR(64), " +
                    "pet_levels TEXT, " +
                    "pet_xp TEXT" +  // <-- Thêm cột này
                    ");";
            stmt.execute(sqlUsers);
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create database tables!");
            e.printStackTrace();
        }
    }

    public boolean isMySQL() {
        return plugin.getConfigFile().getString("database.type", "SQLITE").equalsIgnoreCase("MYSQL");
    }

    public String getUsersTable() {
        return plugin.getConfigFile().getString("database.table_users", "sincepet_users");
    }
}