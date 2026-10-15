package com.everyday.repository;

import com.everyday.database.DatabaseManager;
import com.everyday.model.SkippedDay;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SkippedDayRepository {

    public SkippedDay getByDate(LocalDate date) throws SQLException {
        String sql = "SELECT id, logical_date, reason, created_at FROM skipped_days WHERE logical_date = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<SkippedDay> getAll() throws SQLException {
        List<SkippedDay> list = new ArrayList<>();
        String sql = "SELECT id, logical_date, reason, created_at FROM skipped_days ORDER BY logical_date DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public SkippedDay insert(SkippedDay skippedDay) throws SQLException {
        if (skippedDay.getReason() == null || skippedDay.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("A skip reason is required.");
        }
        String sql = """
            INSERT INTO skipped_days (logical_date, reason, created_at)
            VALUES (?, ?, ?)
            ON CONFLICT(logical_date) DO UPDATE SET
                reason = excluded.reason
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, skippedDay.getLogicalDate().toString());
            ps.setString(2, skippedDay.getReason().trim());
            ps.setString(3, LocalDateTime.now().toString());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    skippedDay.setId(rs.getInt(1));
                }
            }
        }
        return skippedDay;
    }

    public void delete(LocalDate date) throws SQLException {
        String sql = "DELETE FROM skipped_days WHERE logical_date = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            ps.executeUpdate();
        }
    }

    private SkippedDay mapRow(ResultSet rs) throws SQLException {
        return new SkippedDay(
                rs.getInt("id"),
                LocalDate.parse(rs.getString("logical_date")),
                rs.getString("reason"),
                LocalDateTime.parse(rs.getString("created_at"))
        );
    }
}
