package com.everyday.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages SQLite database connection and initialization.
 * Stores database in local AppData directory or user home for production portability.
 */
public class DatabaseManager {

    private static String dbPath;
    private static Connection connection;

    public static synchronized void initialize(String customDbPath) throws SQLException {
        if (customDbPath != null && !customDbPath.isBlank()) {
            dbPath = customDbPath;
        } else {
            String appData = System.getenv("APPDATA");
            File baseDir;
            if (appData != null && !appData.isBlank()) {
                baseDir = new File(appData, "EveryDay");
            } else {
                baseDir = new File(System.getProperty("user.home"), ".everyday");
            }
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }
            dbPath = new File(baseDir, "everyday.db").getAbsolutePath();
        }

        // Initialize schema
        try (Connection conn = getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
                stmt.execute("PRAGMA journal_mode = WAL;");
            }
            SchemaMigration.applyMigrations(conn);
        }
    }

    public static synchronized Connection getConnection() throws SQLException {
        if (dbPath == null) {
            initialize(null);
        }
        if (connection == null || connection.isClosed()) {
            String url = "jdbc:sqlite:" + dbPath;
            connection = DriverManager.getConnection(url);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
        }
        return connection;
    }

    public static synchronized void close() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException ignored) {
            }
            connection = null;
        }
    }

    public static String getDbPath() {
        return dbPath;
    }
}
