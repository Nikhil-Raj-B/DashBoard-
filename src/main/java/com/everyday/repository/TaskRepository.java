package com.everyday.repository;

import com.everyday.database.DatabaseManager;
import com.everyday.model.Category;
import com.everyday.model.Project;
import com.everyday.model.Task;
import com.everyday.model.TaskSchedule;
import com.everyday.model.TaskSchedule.ScheduleType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TaskRepository {

    public List<Task> getAll() throws SQLException {
        List<Task> list = new ArrayList<>();
        String sql = """
            SELECT t.id, t.name, t.category_id, t.project_id, t.scheduled_time, t.duration_minutes,
                   t.notes, t.start_date, t.is_active, t.display_order, t.multi_day_end_date,
                   t.task_progress, t.reminder_time, t.reminder_enabled, t.created_logical_date,
                   c.name AS cat_name, c.color AS cat_color, c.is_default AS cat_is_default,
                   p.name AS proj_name, p.status AS proj_status,
                   s.id AS sched_id, s.schedule_type, s.weekdays, s.interval_days, s.custom_dates,
                   s.start_date AS sched_start, s.end_date AS sched_end
            FROM tasks t
            JOIN categories c ON t.category_id = c.id
            LEFT JOIN projects p ON t.project_id = p.id
            LEFT JOIN task_schedules s ON t.id = s.task_id
            ORDER BY t.display_order ASC, t.id ASC
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapTaskWithJoins(rs));
            }
        }
        return list;
    }

    public Task getById(int id) throws SQLException {
        String sql = """
            SELECT t.id, t.name, t.category_id, t.project_id, t.scheduled_time, t.duration_minutes,
                   t.notes, t.start_date, t.is_active, t.display_order, t.multi_day_end_date,
                   t.task_progress, t.reminder_time, t.reminder_enabled, t.created_logical_date,
                   c.name AS cat_name, c.color AS cat_color, c.is_default AS cat_is_default,
                   p.name AS proj_name, p.status AS proj_status,
                   s.id AS sched_id, s.schedule_type, s.weekdays, s.interval_days, s.custom_dates,
                   s.start_date AS sched_start, s.end_date AS sched_end
            FROM tasks t
            JOIN categories c ON t.category_id = c.id
            LEFT JOIN projects p ON t.project_id = p.id
            LEFT JOIN task_schedules s ON t.id = s.task_id
            WHERE t.id = ?
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTaskWithJoins(rs);
                }
            }
        }
        return null;
    }

    public Task insert(Task task, TaskSchedule schedule) throws SQLException {
        String insertTaskSql = """
            INSERT INTO tasks (name, category_id, project_id, scheduled_time, duration_minutes,
                               notes, start_date, is_active, display_order, multi_day_end_date,
                               task_progress, reminder_time, reminder_enabled, created_logical_date,
                               created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        String insertScheduleSql = """
            INSERT INTO task_schedules (task_id, schedule_type, weekdays, interval_days, custom_dates, start_date, end_date)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        Connection conn = DatabaseManager.getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            String now = LocalDateTime.now().toString();

            try (PreparedStatement ps = conn.prepareStatement(insertTaskSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, task.getName());
                ps.setInt(2, task.getCategoryId());
                if (task.getProjectId() != null) ps.setInt(3, task.getProjectId());
                else ps.setNull(3, java.sql.Types.INTEGER);
                ps.setString(4, task.getScheduledTime());
                ps.setInt(5, task.getDurationMinutes());
                ps.setString(6, task.getNotes());
                ps.setString(7, task.getStartDate().toString());
                ps.setInt(8, task.isActive() ? 1 : 0);
                ps.setInt(9, task.getDisplayOrder());
                ps.setString(10, task.getMultiDayEndDate() != null ? task.getMultiDayEndDate().toString() : null);
                ps.setDouble(11, task.getTaskProgress());
                ps.setString(12, task.getReminderTime());
                ps.setInt(13, task.isReminderEnabled() ? 1 : 0);
                ps.setString(14, task.getCreatedLogicalDate() != null ? task.getCreatedLogicalDate().toString() : LocalDate.now().toString());
                ps.setString(15, now);
                ps.setString(16, now);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        task.setId(rs.getInt(1));
                    }
                }
            }

            if (schedule == null) {
                schedule = new TaskSchedule(ScheduleType.EVERY_DAY);
            }
            schedule.setTaskId(task.getId());

            try (PreparedStatement psSched = conn.prepareStatement(insertScheduleSql, Statement.RETURN_GENERATED_KEYS)) {
                psSched.setInt(1, task.getId());
                psSched.setString(2, schedule.getScheduleType().name());
                psSched.setString(3, schedule.getWeekdays());
                psSched.setInt(4, schedule.getIntervalDays());
                psSched.setString(5, schedule.getCustomDates());
                psSched.setString(6, schedule.getStartDate() != null ? schedule.getStartDate().toString() : null);
                psSched.setString(7, schedule.getEndDate() != null ? schedule.getEndDate().toString() : null);
                psSched.executeUpdate();

                try (ResultSet rs = psSched.getGeneratedKeys()) {
                    if (rs.next()) {
                        schedule.setId(rs.getInt(1));
                    }
                }
            }

            conn.commit();
            task.setSchedule(schedule);
            return task;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    public void update(Task task, TaskSchedule schedule) throws SQLException {
        String updateTaskSql = """
            UPDATE tasks SET name = ?, category_id = ?, project_id = ?, scheduled_time = ?,
                             duration_minutes = ?, notes = ?, start_date = ?, is_active = ?,
                             multi_day_end_date = ?, task_progress = ?, reminder_time = ?,
                             reminder_enabled = ?, updated_at = ?
            WHERE id = ?
        """;

        String updateScheduleSql = """
            UPDATE task_schedules SET schedule_type = ?, weekdays = ?, interval_days = ?,
                                      custom_dates = ?, start_date = ?, end_date = ?
            WHERE task_id = ?
        """;

        Connection conn = DatabaseManager.getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(updateTaskSql)) {
                ps.setString(1, task.getName());
                ps.setInt(2, task.getCategoryId());
                if (task.getProjectId() != null) ps.setInt(3, task.getProjectId());
                else ps.setNull(3, java.sql.Types.INTEGER);
                ps.setString(4, task.getScheduledTime());
                ps.setInt(5, task.getDurationMinutes());
                ps.setString(6, task.getNotes());
                ps.setString(7, task.getStartDate().toString());
                ps.setInt(8, task.isActive() ? 1 : 0);
                ps.setString(9, task.getMultiDayEndDate() != null ? task.getMultiDayEndDate().toString() : null);
                ps.setDouble(10, task.getTaskProgress());
                ps.setString(11, task.getReminderTime());
                ps.setInt(12, task.isReminderEnabled() ? 1 : 0);
                ps.setString(13, LocalDateTime.now().toString());
                ps.setInt(14, task.getId());
                ps.executeUpdate();
            }

            if (schedule != null) {
                try (PreparedStatement psSched = conn.prepareStatement(updateScheduleSql)) {
                    psSched.setString(1, schedule.getScheduleType().name());
                    psSched.setString(2, schedule.getWeekdays());
                    psSched.setInt(3, schedule.getIntervalDays());
                    psSched.setString(4, schedule.getCustomDates());
                    psSched.setString(5, schedule.getStartDate() != null ? schedule.getStartDate().toString() : null);
                    psSched.setString(6, schedule.getEndDate() != null ? schedule.getEndDate().toString() : null);
                    psSched.setInt(7, task.getId());
                    psSched.executeUpdate();
                }
            }

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    public void updateDisplayOrder(int taskId, int order) throws SQLException {
        String sql = "UPDATE tasks SET display_order = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, order);
            ps.setInt(2, taskId);
            ps.executeUpdate();
        }
    }

    public void updateActive(int taskId, boolean active) throws SQLException {
        String sql = "UPDATE tasks SET is_active = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setString(2, LocalDateTime.now().toString());
            ps.setInt(3, taskId);
            ps.executeUpdate();
        }
    }

    public void updateTaskProgress(int taskId, double progress) throws SQLException {
        String sql = "UPDATE tasks SET task_progress = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, Math.max(0.0, Math.min(100.0, progress)));
            ps.setString(2, LocalDateTime.now().toString());
            ps.setInt(3, taskId);
            ps.executeUpdate();
        }
    }

    public void delete(int taskId) throws SQLException {
        String sql = "DELETE FROM tasks WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.executeUpdate();
        }
    }

    private Task mapTaskWithJoins(ResultSet rs) throws SQLException {
        String multiDayEndStr = rs.getString("multi_day_end_date");
        LocalDate multiDayEnd = multiDayEndStr != null && !multiDayEndStr.isBlank() ? LocalDate.parse(multiDayEndStr) : null;
        String createdLogDateStr = rs.getString("created_logical_date");
        LocalDate createdLogicalDate = createdLogDateStr != null ? LocalDate.parse(createdLogDateStr) : LocalDate.now();

        Integer projId = rs.getInt("project_id");
        if (rs.wasNull()) projId = null;

        Task task = new Task(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("category_id"),
                projId,
                rs.getString("scheduled_time"),
                rs.getInt("duration_minutes"),
                rs.getString("notes"),
                LocalDate.parse(rs.getString("start_date")),
                rs.getInt("is_active") == 1,
                rs.getInt("display_order"),
                multiDayEnd,
                rs.getDouble("task_progress"),
                rs.getString("reminder_time"),
                rs.getInt("reminder_enabled") == 1,
                createdLogicalDate
        );

        Category category = new Category(
                rs.getInt("category_id"),
                rs.getString("cat_name"),
                rs.getString("cat_color"),
                rs.getInt("cat_is_default") == 1
        );
        task.setCategory(category);

        if (projId != null && rs.getString("proj_name") != null) {
            Project project = new Project();
            project.setId(projId);
            project.setName(rs.getString("proj_name"));
            task.setProject(project);
        }

        if (rs.getString("schedule_type") != null) {
            TaskSchedule sched = new TaskSchedule();
            sched.setId(rs.getInt("sched_id"));
            sched.setTaskId(task.getId());
            sched.setScheduleType(ScheduleType.valueOf(rs.getString("schedule_type")));
            sched.setWeekdays(rs.getString("weekdays"));
            sched.setIntervalDays(rs.getInt("interval_days"));
            sched.setCustomDates(rs.getString("custom_dates"));
            String sStart = rs.getString("sched_start");
            if (sStart != null && !sStart.isBlank()) sched.setStartDate(LocalDate.parse(sStart));
            String sEnd = rs.getString("sched_end");
            if (sEnd != null && !sEnd.isBlank()) sched.setEndDate(LocalDate.parse(sEnd));
            task.setSchedule(sched);
        }

        return task;
    }
}
