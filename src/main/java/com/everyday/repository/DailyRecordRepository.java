package com.everyday.repository;

import com.everyday.database.DatabaseManager;
import com.everyday.model.DailyRecord;
import com.everyday.model.DailyRecord.DayStatus;
import com.everyday.model.DailyRecord.StreakState;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DailyRecordRepository {

    public DailyRecord getByDate(LocalDate date) throws SQLException {
        String sql = """
            SELECT id, logical_date, total_active_tasks, completed_active_tasks,
                   completion_percentage, day_status, streak_state, evaluated_at
            FROM daily_records
            WHERE logical_date = ?
        """;
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

    public List<DailyRecord> getAll() throws SQLException {
        List<DailyRecord> list = new ArrayList<>();
        String sql = """
            SELECT id, logical_date, total_active_tasks, completed_active_tasks,
                   completion_percentage, day_status, streak_state, evaluated_at
            FROM daily_records
            ORDER BY logical_date ASC
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<DailyRecord> getBetween(LocalDate start, LocalDate end) throws SQLException {
        List<DailyRecord> list = new ArrayList<>();
        String sql = """
            SELECT id, logical_date, total_active_tasks, completed_active_tasks,
                   completion_percentage, day_status, streak_state, evaluated_at
            FROM daily_records
            WHERE logical_date >= ? AND logical_date <= ?
            ORDER BY logical_date ASC
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, start.toString());
            ps.setString(2, end.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public DailyRecord insertOrUpdate(DailyRecord record) throws SQLException {
        String sql = """
            INSERT INTO daily_records (
                logical_date, total_active_tasks, completed_active_tasks,
                completion_percentage, day_status, streak_state, evaluated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(logical_date) DO UPDATE SET
                total_active_tasks = excluded.total_active_tasks,
                completed_active_tasks = excluded.completed_active_tasks,
                completion_percentage = excluded.completion_percentage,
                day_status = excluded.day_status,
                streak_state = excluded.streak_state,
                evaluated_at = excluded.evaluated_at
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, record.getLogicalDate().toString());
            ps.setInt(2, record.getTotalActiveTasks());
            ps.setInt(3, record.getCompletedActiveTasks());
            ps.setDouble(4, record.getCompletionPercentage());
            ps.setString(5, record.getDayStatus().name());
            ps.setString(6, record.getStreakState().name());
            ps.setString(7, LocalDateTime.now().toString());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    record.setId(rs.getInt(1));
                }
            }
        }
        return record;
    }

    private DailyRecord mapRow(ResultSet rs) throws SQLException {
        DailyRecord record = new DailyRecord();
        record.setId(rs.getInt("id"));
        record.setLogicalDate(LocalDate.parse(rs.getString("logical_date")));
        record.setTotalActiveTasks(rs.getInt("total_active_tasks"));
        record.setCompletedActiveTasks(rs.getInt("completed_active_tasks"));
        record.setCompletionPercentage(rs.getDouble("completion_percentage"));
        record.setDayStatus(DayStatus.valueOf(rs.getString("day_status")));
        record.setStreakState(StreakState.valueOf(rs.getString("streak_state")));
        String evalStr = rs.getString("evaluated_at");
        if (evalStr != null) {
            record.setEvaluatedAt(LocalDateTime.parse(evalStr));
        }
        return record;
    }
}
