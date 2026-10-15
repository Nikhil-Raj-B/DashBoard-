package com.everyday.repository;

import com.everyday.database.DatabaseManager;
import com.everyday.model.AppSettings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class SettingsRepository {

    public AppSettings getSettings() throws SQLException {
        String sql = "SELECT id, day_start_time, dark_theme, notifications_enabled FROM app_settings WHERE id = 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new AppSettings(
                        rs.getInt("id"),
                        rs.getString("day_start_time"),
                        rs.getInt("dark_theme") == 1,
                        rs.getInt("notifications_enabled") == 1
                );
            }
        }
        return new AppSettings();
    }

    public void updateSettings(AppSettings settings) throws SQLException {
        String sql = "UPDATE app_settings SET day_start_time = ?, dark_theme = ?, notifications_enabled = ?, updated_at = ? WHERE id = 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, settings.getDayStartTime());
            ps.setInt(2, settings.isDarkTheme() ? 1 : 0);
            ps.setInt(3, settings.isNotificationsEnabled() ? 1 : 0);
            ps.setString(4, LocalDateTime.now().toString());
            ps.executeUpdate();
        }
    }
}
