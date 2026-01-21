package net.danh.sincePet.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.danh.sincePet.SincePet;

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
        createTables();
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
        } else {
            File file = new File(plugin.getDataFolder(), "database.db");
            config.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
        }

        config.setPoolName("SincePet-Pool");
        config.setMaximumPoolSize(10);
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
            String sqlUsers = "CREATE TABLE IF NOT EXISTS " + usersTable + " (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "active_pet VARCHAR(64), " +
                    "pet_levels TEXT" +
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