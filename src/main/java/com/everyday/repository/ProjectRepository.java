package com.everyday.repository;

import com.everyday.database.DatabaseManager;
import com.everyday.model.Project;
import com.everyday.model.Project.ProjectStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProjectRepository {

    public List<Project> getAll() throws SQLException {
        List<Project> list = new ArrayList<>();
        String sql = "SELECT id, name, description, start_date, target_date, status, notes, progress FROM projects ORDER BY id DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public Project getById(int id) throws SQLException {
        String sql = "SELECT id, name, description, start_date, target_date, status, notes, progress FROM projects WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public Project insert(Project project) throws SQLException {
        String sql = """
            INSERT INTO projects (name, description, start_date, target_date, status, notes, progress, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            String now = LocalDateTime.now().toString();
            ps.setString(1, project.getName());
            ps.setString(2, project.getDescription());
            ps.setString(3, project.getStartDate().toString());
            ps.setString(4, project.getTargetDate() != null ? project.getTargetDate().toString() : null);
            ps.setString(5, project.getStatus().name());
            ps.setString(6, project.getNotes());
            ps.setDouble(7, project.getProgress());
            ps.setString(8, now);
            ps.setString(9, now);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    project.setId(rs.getInt(1));
                }
            }
        }
        return project;
    }

    public void update(Project project) throws SQLException {
        String sql = """
            UPDATE projects SET name = ?, description = ?, start_date = ?, target_date = ?,
                                status = ?, notes = ?, progress = ?, updated_at = ?
            WHERE id = ?
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, project.getName());
            ps.setString(2, project.getDescription());
            ps.setString(3, project.getStartDate().toString());
            ps.setString(4, project.getTargetDate() != null ? project.getTargetDate().toString() : null);
            ps.setString(5, project.getStatus().name());
            ps.setString(6, project.getNotes());
            ps.setDouble(7, project.getProgress());
            ps.setString(8, LocalDateTime.now().toString());
            ps.setInt(9, project.getId());
            ps.executeUpdate();
        }
    }

    public void updateProgress(int id, double progress) throws SQLException {
        String sql = "UPDATE projects SET progress = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, Math.max(0.0, Math.min(100.0, progress)));
            ps.setString(2, LocalDateTime.now().toString());
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM projects WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Project mapRow(ResultSet rs) throws SQLException {
        String targetDateStr = rs.getString("target_date");
        LocalDate targetDate = targetDateStr != null && !targetDateStr.isBlank() ? LocalDate.parse(targetDateStr) : null;
        return new Project(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                LocalDate.parse(rs.getString("start_date")),
                targetDate,
                ProjectStatus.valueOf(rs.getString("status")),
                rs.getString("notes"),
                rs.getDouble("progress")
        );
    }
}
