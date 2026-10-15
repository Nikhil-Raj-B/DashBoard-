package com.everyday.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

/**
 * Handles database schema creation and versioned migrations.
 */
public class SchemaMigration {

    private static final int CURRENT_VERSION = 1;

    public static void applyMigrations(Connection conn) throws SQLException {
        conn.setAutoCommit(false);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS schema_version (
                    version INTEGER PRIMARY KEY,
                    applied_at TEXT NOT NULL
                );
            """);

            int existingVersion = 0;
            try (ResultSet rs = stmt.executeQuery("SELECT MAX(version) FROM schema_version")) {
                if (rs.next()) {
                    existingVersion = rs.getInt(1);
                }
            }

            if (existingVersion < 1) {
                applyVersion1(stmt, conn);
            }

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static void applyVersion1(Statement stmt, Connection conn) throws SQLException {
        // User Profile
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS user_profile (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                name TEXT NOT NULL DEFAULT 'Nikhil',
                tagline TEXT NOT NULL DEFAULT 'Personal',
                avatar_path TEXT,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            );
        """);

        // App Settings
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS app_settings (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                day_start_time TEXT NOT NULL DEFAULT '00:00',
                dark_theme INTEGER NOT NULL DEFAULT 1,
                notifications_enabled INTEGER NOT NULL DEFAULT 1,
                updated_at TEXT NOT NULL
            );
        """);

        // Categories
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL,
                color TEXT NOT NULL DEFAULT '#3B82F6',
                is_default INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL
            );
        """);

        // Projects
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS projects (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                description TEXT,
                start_date TEXT NOT NULL,
                target_date TEXT,
                status TEXT NOT NULL DEFAULT 'ACTIVE',
                notes TEXT,
                progress REAL NOT NULL DEFAULT 0.0,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            );
        """);

        // Tasks
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                category_id INTEGER NOT NULL REFERENCES categories(id),
                project_id INTEGER REFERENCES projects(id) ON DELETE SET NULL,
                scheduled_time TEXT,
                duration_minutes INTEGER DEFAULT 30,
                notes TEXT,
                start_date TEXT NOT NULL,
                is_active INTEGER NOT NULL DEFAULT 1,
                display_order INTEGER NOT NULL DEFAULT 0,
                multi_day_end_date TEXT,
                task_progress REAL NOT NULL DEFAULT 0.0,
                reminder_time TEXT,
                reminder_enabled INTEGER NOT NULL DEFAULT 0,
                created_logical_date TEXT NOT NULL,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            );
        """);

        // Task Schedules
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS task_schedules (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                task_id INTEGER NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
                schedule_type TEXT NOT NULL,
                weekdays TEXT,
                interval_days INTEGER DEFAULT 1,
                custom_dates TEXT,
                start_date TEXT,
                end_date TEXT
            );
        """);

        // Daily Records (Daily Aggregate)
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS daily_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                logical_date TEXT UNIQUE NOT NULL,
                total_active_tasks INTEGER NOT NULL DEFAULT 0,
                completed_active_tasks INTEGER NOT NULL DEFAULT 0,
                completion_percentage REAL NOT NULL DEFAULT 0.0,
                day_status TEXT NOT NULL DEFAULT 'INCOMPLETE',
                streak_state TEXT NOT NULL DEFAULT 'NO_STREAK',
                evaluated_at TEXT
            );
        """);

        // Task Daily Instances (Per-day task status & snapshot)
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS task_daily_instances (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                logical_date TEXT NOT NULL,
                task_id INTEGER NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
                task_name_snapshot TEXT NOT NULL,
                category_name_snapshot TEXT NOT NULL,
                project_name_snapshot TEXT,
                is_late_addition INTEGER NOT NULL DEFAULT 0,
                is_completed INTEGER NOT NULL DEFAULT 0,
                completed_at TEXT,
                display_order INTEGER DEFAULT 0,
                is_deleted_occurrence INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL,
                UNIQUE(logical_date, task_id)
            );
        """);

        // Missed Records (Historical missed tasks)
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS missed_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                logical_date TEXT NOT NULL,
                task_id INTEGER NOT NULL,
                task_name TEXT NOT NULL,
                category_name TEXT NOT NULL,
                project_name TEXT,
                created_at TEXT NOT NULL,
                UNIQUE(logical_date, task_id)
            );
        """);

        // Skipped Days
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS skipped_days (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                logical_date TEXT UNIQUE NOT NULL,
                reason TEXT NOT NULL,
                created_at TEXT NOT NULL
            );
        """);

        // Streaks (Overall and per category)
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS streaks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                streak_type TEXT UNIQUE NOT NULL,
                current_streak INTEGER NOT NULL DEFAULT 0,
                longest_streak INTEGER NOT NULL DEFAULT 0,
                last_evaluated_date TEXT,
                updated_at TEXT NOT NULL
            );
        """);

        // Indexes for performance
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_tasks_category ON tasks(category_id);");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_tasks_project ON tasks(project_id);");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_task_instances_date ON task_daily_instances(logical_date);");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_missed_records_date ON missed_records(logical_date);");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_daily_records_date ON daily_records(logical_date);");

        String now = LocalDateTime.now().toString();

        // Seed default profile if not exists
        stmt.execute(String.format("""
            INSERT OR IGNORE INTO user_profile (id, name, tagline, avatar_path, created_at, updated_at)
            VALUES (1, 'Nikhil Raj', 'Personal', NULL, '%s', '%s');
        """, now, now));

        // Seed default settings if not exists
        stmt.execute(String.format("""
            INSERT OR IGNORE INTO app_settings (id, day_start_time, dark_theme, notifications_enabled, updated_at)
            VALUES (1, '00:00', 1, 1, '%s');
        """, now));

        // Seed default categories
        String[][] defaultCats = {
            {"Coding", "#3B82F6"},
            {"DSA", "#60A5FA"},
            {"Gym", "#10B981"},
            {"Study", "#F59E0B"},
            {"Project", "#8B5CF6"},
            {"Reading", "#EC4899"},
            {"Personal", "#06B6D4"}
        };
        for (String[] cat : defaultCats) {
            String insertCat = String.format("""
                INSERT OR IGNORE INTO categories (name, color, is_default, created_at)
                VALUES ('%s', '%s', 1, '%s');
            """, cat[0], cat[1], now);
            stmt.execute(insertCat);
        }

        // Seed initial OVERALL streak entry
        stmt.execute(String.format("""
            INSERT OR IGNORE INTO streaks (streak_type, current_streak, longest_streak, last_evaluated_date, updated_at)
            VALUES ('OVERALL', 0, 0, NULL, '%s');
        """, now));

        // Record schema version
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO schema_version (version, applied_at) VALUES (?, ?)")) {
            ps.setInt(1, 1);
            ps.setString(2, now);
            ps.executeUpdate();
        }
    }
}
