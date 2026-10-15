package com.everyday.repository;

import com.everyday.database.DatabaseManager;
import com.everyday.model.UserProfile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class ProfileRepository {

    public UserProfile getProfile() throws SQLException {
        String sql = "SELECT id, name, tagline, avatar_path FROM user_profile WHERE id = 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new UserProfile(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("tagline"),
                        rs.getString("avatar_path")
                );
            }
        }
        return new UserProfile();
    }

    public void updateProfile(UserProfile profile) throws SQLException {
        String sql = "UPDATE user_profile SET name = ?, tagline = ?, avatar_path = ?, updated_at = ? WHERE id = 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, profile.getName());
            ps.setString(2, profile.getTagline());
            ps.setString(3, profile.getAvatarPath());
            ps.setString(4, LocalDateTime.now().toString());
            ps.executeUpdate();
        }
    }
}
