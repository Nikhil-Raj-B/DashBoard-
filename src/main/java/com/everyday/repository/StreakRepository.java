package com.everyday.repository;

import com.everyday.database.DatabaseManager;
import com.everyday.model.StreakRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StreakRepository {

    public StreakRecord getByType(String type) throws SQLException {
        String sql = "SELECT id, streak_type, current_streak, longest_streak, last_evaluated_date, updated_at FROM streaks WHERE UPPER(streak_type) = UPPER(?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<StreakRecord> getAll() throws SQLException {
        List<StreakRecord> list = new ArrayList<>();
        String sql = "SELECT id, streak_type, current_streak, longest_streak, last_evaluated_date, updated_at FROM streaks ORDER BY id ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public StreakRecord insertOrUpdate(StreakRecord record) throws SQLException {
        String sql = """
            INSERT INTO streaks (streak_type, current_streak, longest_streak, last_evaluated_date, updated_at)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(streak_type) DO UPDATE SET
                current_streak = excluded.current_streak,
                longest_streak = excluded.longest_streak,
                last_evaluated_date = excluded.last_evaluated_date,
                updated_at = excluded.updated_at
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, record.getStreakType().toUpperCase());
            ps.setInt(2, record.getCurrentStreak());
            ps.setInt(3, record.getLongestStreak());
            ps.setString(4, record.getLastEvaluatedDate() != null ? record.getLastEvaluatedDate().toString() : null);
            ps.setString(5, LocalDateTime.now().toString());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    record.setId(rs.getInt(1));
                }
            }
        }
        return record;
    }

    private StreakRecord mapRow(ResultSet rs) throws SQLException {
        String dateStr = rs.getString("last_evaluated_date");
        LocalDate evalDate = dateStr != null && !dateStr.isBlank() ? LocalDate.parse(dateStr) : null;
        return new StreakRecord(
                rs.getInt("id"),
                rs.getString("streak_type"),
                rs.getInt("current_streak"),
                rs.getInt("longest_streak"),
                evalDate,
                LocalDateTime.parse(rs.getString("updated_at"))
        );
    }
}
