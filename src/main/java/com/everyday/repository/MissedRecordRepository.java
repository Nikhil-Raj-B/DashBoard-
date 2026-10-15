package com.everyday.repository;

import com.everyday.database.DatabaseManager;
import com.everyday.model.MissedRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MissedRecordRepository {

    public List<MissedRecord> getByDate(LocalDate date) throws SQLException {
        List<MissedRecord> list = new ArrayList<>();
        String sql = "SELECT id, logical_date, task_id, task_name, category_name, project_name, created_at FROM missed_records WHERE logical_date = ? ORDER BY id ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public List<MissedRecord> getRecent(int limit) throws SQLException {
        List<MissedRecord> list = new ArrayList<>();
        String sql = "SELECT id, logical_date, task_id, task_name, category_name, project_name, created_at FROM missed_records ORDER BY logical_date DESC, id DESC LIMIT ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public void insert(MissedRecord record) throws SQLException {
        String sql = """
            INSERT OR IGNORE INTO missed_records (logical_date, task_id, task_name, category_name, project_name, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, record.getLogicalDate().toString());
            ps.setInt(2, record.getTaskId());
            ps.setString(3, record.getTaskName());
            ps.setString(4, record.getCategoryName());
            ps.setString(5, record.getProjectName());
            ps.setString(6, LocalDateTime.now().toString());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    record.setId(rs.getInt(1));
                }
            }
        }
    }

    public void delete(LocalDate date, int taskId) throws SQLException {
        String sql = "DELETE FROM missed_records WHERE logical_date = ? AND task_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            ps.setInt(2, taskId);
            ps.executeUpdate();
        }
    }

    private MissedRecord mapRow(ResultSet rs) throws SQLException {
        return new MissedRecord(
                rs.getInt("id"),
                LocalDate.parse(rs.getString("logical_date")),
                rs.getInt("task_id"),
                rs.getString("task_name"),
                rs.getString("category_name"),
                rs.getString("project_name"),
                LocalDateTime.parse(rs.getString("created_at"))
        );
    }
}
