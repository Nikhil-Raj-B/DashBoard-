package com.everyday.repository;

import com.everyday.database.DatabaseManager;
import com.everyday.model.TaskDailyInstance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TaskDailyInstanceRepository {

    public List<TaskDailyInstance> getInstancesForDate(LocalDate date) throws SQLException {
        List<TaskDailyInstance> list = new ArrayList<>();
        String sql = """
            SELECT id, logical_date, task_id, task_name_snapshot, category_name_snapshot,
                   project_name_snapshot, is_late_addition, is_completed, completed_at,
                   display_order, is_deleted_occurrence
            FROM task_daily_instances
            WHERE logical_date = ? AND is_deleted_occurrence = 0
            ORDER BY display_order ASC, id ASC
        """;
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

    public TaskDailyInstance getInstance(LocalDate date, int taskId) throws SQLException {
        String sql = """
            SELECT id, logical_date, task_id, task_name_snapshot, category_name_snapshot,
                   project_name_snapshot, is_late_addition, is_completed, completed_at,
                   display_order, is_deleted_occurrence
            FROM task_daily_instances
            WHERE logical_date = ? AND task_id = ?
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            ps.setInt(2, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public TaskDailyInstance insertOrUpdate(TaskDailyInstance instance) throws SQLException {
        String sql = """
            INSERT INTO task_daily_instances (
                logical_date, task_id, task_name_snapshot, category_name_snapshot,
                project_name_snapshot, is_late_addition, is_completed, completed_at,
                display_order, is_deleted_occurrence, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(logical_date, task_id) DO UPDATE SET
                display_order = excluded.display_order,
                is_deleted_occurrence = excluded.is_deleted_occurrence
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, instance.getLogicalDate().toString());
            ps.setInt(2, instance.getTaskId());
            ps.setString(3, instance.getTaskNameSnapshot());
            ps.setString(4, instance.getCategoryNameSnapshot());
            ps.setString(5, instance.getProjectNameSnapshot());
            ps.setInt(6, instance.isLateAddition() ? 1 : 0);
            ps.setInt(7, instance.isCompleted() ? 1 : 0);
            ps.setString(8, instance.getCompletedAt() != null ? instance.getCompletedAt().toString() : null);
            ps.setInt(9, instance.getDisplayOrder());
            ps.setInt(10, instance.isDeletedOccurrence() ? 1 : 0);
            ps.setString(11, LocalDateTime.now().toString());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    instance.setId(rs.getInt(1));
                }
            }
        }
        return instance;
    }

    public void markCompleted(LocalDate date, int taskId, boolean completed, LocalDateTime completedAt) throws SQLException {
        String sql = """
            UPDATE task_daily_instances
            SET is_completed = ?, completed_at = ?
            WHERE logical_date = ? AND task_id = ?
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, completed ? 1 : 0);
            ps.setString(2, completed && completedAt != null ? completedAt.toString() : null);
            ps.setString(3, date.toString());
            ps.setInt(4, taskId);
            ps.executeUpdate();
        }
    }

    public void markDeletedOccurrence(LocalDate date, int taskId) throws SQLException {
        String sql = """
            UPDATE task_daily_instances
            SET is_deleted_occurrence = 1
            WHERE logical_date = ? AND task_id = ?
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            ps.setInt(2, taskId);
            ps.executeUpdate();
        }
    }

    public void deleteInstance(LocalDate date, int taskId) throws SQLException {
        String sql = "DELETE FROM task_daily_instances WHERE logical_date = ? AND task_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            ps.setInt(2, taskId);
            ps.executeUpdate();
        }
    }

    public void updateOrder(LocalDate date, int taskId, int order) throws SQLException {
        String sql = "UPDATE task_daily_instances SET display_order = ? WHERE logical_date = ? AND task_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, order);
            ps.setString(2, date.toString());
            ps.setInt(3, taskId);
            ps.executeUpdate();
        }
    }

    private TaskDailyInstance mapRow(ResultSet rs) throws SQLException {
        String compAtStr = rs.getString("completed_at");
        LocalDateTime compAt = compAtStr != null && !compAtStr.isBlank() ? LocalDateTime.parse(compAtStr) : null;
        return new TaskDailyInstance(
                rs.getInt("id"),
                LocalDate.parse(rs.getString("logical_date")),
                rs.getInt("task_id"),
                rs.getString("task_name_snapshot"),
                rs.getString("category_name_snapshot"),
                rs.getString("project_name_snapshot"),
                rs.getInt("is_late_addition") == 1,
                rs.getInt("is_completed") == 1,
                compAt,
                rs.getInt("display_order"),
                rs.getInt("is_deleted_occurrence") == 1
        );
    }
}
