package com.everyday.service;

import com.everyday.model.DailyRecord;
import com.everyday.model.MissedRecord;
import com.everyday.model.SkippedDay;
import com.everyday.model.TaskDailyInstance;
import com.everyday.repository.DailyRecordRepository;
import com.everyday.repository.MissedRecordRepository;
import com.everyday.repository.SkippedDayRepository;
import com.everyday.repository.TaskDailyInstanceRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarService {

    public enum DayVisualState {
        COMPLETED,
        INCOMPLETE_STREAK_CONTINUED,
        SKIPPED,
        STREAK_BROKEN,
        EMPTY_OR_FUTURE
    }

    public static class DayDetail {
        public LocalDate date;
        public DayVisualState visualState;
        public double completionPercentage;
        public int tasksCompleted;
        public int totalTasks;
        public String dayStatus;
        public String streakState;
        public List<TaskDailyInstance> completedTasks = new ArrayList<>();
        public List<TaskDailyInstance> unfinishedTasks = new ArrayList<>();
        public List<MissedRecord> missedTasks = new ArrayList<>();
        public String skippedReason;
    }

    private final DailyRecordRepository dailyRecordRepository;
    private final TaskDailyInstanceRepository instanceRepository;
    private final MissedRecordRepository missedRecordRepository;
    private final SkippedDayRepository skippedDayRepository;

    public CalendarService(DailyRecordRepository dailyRecordRepository,
                           TaskDailyInstanceRepository instanceRepository,
                           MissedRecordRepository missedRecordRepository,
                           SkippedDayRepository skippedDayRepository) {
        this.dailyRecordRepository = dailyRecordRepository;
        this.instanceRepository = instanceRepository;
        this.missedRecordRepository = missedRecordRepository;
        this.skippedDayRepository = skippedDayRepository;
    }

    public Map<LocalDate, DayVisualState> getMonthVisualStates(YearMonth yearMonth) throws SQLException {
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        List<DailyRecord> records = dailyRecordRepository.getBetween(start, end);
        Map<LocalDate, DailyRecord> recMap = new HashMap<>();
        for (DailyRecord r : records) {
            recMap.put(r.getLogicalDate(), r);
        }

        Map<LocalDate, DayVisualState> result = new HashMap<>();
        LocalDate today = LocalDate.now();

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (d.isAfter(today)) {
                result.put(d, DayVisualState.EMPTY_OR_FUTURE);
                continue;
            }

            SkippedDay skipped = skippedDayRepository.getByDate(d);
            if (skipped != null) {
                result.put(d, DayVisualState.SKIPPED);
                continue;
            }

            DailyRecord dr = recMap.get(d);
            if (dr == null || dr.getTotalActiveTasks() == 0) {
                result.put(d, DayVisualState.EMPTY_OR_FUTURE);
            } else if (dr.getCompletedActiveTasks() >= dr.getTotalActiveTasks() && dr.getTotalActiveTasks() > 0) {
                result.put(d, DayVisualState.COMPLETED);
            } else if (dr.getStreakState() == DailyRecord.StreakState.CONTINUED || dr.getStreakState() == DailyRecord.StreakState.INCREASED) {
                result.put(d, DayVisualState.INCOMPLETE_STREAK_CONTINUED);
            } else if (dr.getStreakState() == DailyRecord.StreakState.BROKEN) {
                result.put(d, DayVisualState.STREAK_BROKEN);
            } else {
                result.put(d, dr.getCompletionPercentage() >= 50.0 ? DayVisualState.INCOMPLETE_STREAK_CONTINUED : DayVisualState.STREAK_BROKEN);
            }
        }
        return result;
    }

    public DayDetail getDayDetail(LocalDate date) throws SQLException {
        DayDetail detail = new DayDetail();
        detail.date = date;

        SkippedDay skipped = skippedDayRepository.getByDate(date);
        if (skipped != null) {
            detail.skippedReason = skipped.getReason();
            detail.visualState = DayVisualState.SKIPPED;
            detail.streakState = "SKIPPED";
        }

        DailyRecord dr = dailyRecordRepository.getByDate(date);
        if (dr != null) {
            detail.completionPercentage = dr.getCompletionPercentage();
            detail.tasksCompleted = dr.getCompletedActiveTasks();
            detail.totalTasks = dr.getTotalActiveTasks();
            detail.dayStatus = dr.getDayStatus().name();
            if (detail.streakState == null) {
                detail.streakState = dr.getStreakState().name();
            }
        }

        List<TaskDailyInstance> instances = instanceRepository.getInstancesForDate(date);
        for (TaskDailyInstance inst : instances) {
            if (inst.isDeletedOccurrence()) continue;
            if (inst.isCompleted()) {
                detail.completedTasks.add(inst);
            } else {
                detail.unfinishedTasks.add(inst);
            }
        }

        detail.missedTasks = missedRecordRepository.getByDate(date);

        if (detail.visualState == null) {
            if (detail.totalTasks == 0) {
                detail.visualState = DayVisualState.EMPTY_OR_FUTURE;
            } else if (detail.tasksCompleted >= detail.totalTasks) {
                detail.visualState = DayVisualState.COMPLETED;
            } else if (detail.completionPercentage >= 50.0) {
                detail.visualState = DayVisualState.INCOMPLETE_STREAK_CONTINUED;
            } else {
                detail.visualState = DayVisualState.STREAK_BROKEN;
            }
        }

        return detail;
    }

    public Map<LocalDate, Double> getHeatMapData(LocalDate startDate, LocalDate endDate) throws SQLException {
        List<DailyRecord> records = dailyRecordRepository.getBetween(startDate, endDate);
        Map<LocalDate, Double> map = new HashMap<>();
        for (DailyRecord r : records) {
            map.put(r.getLogicalDate(), r.getCompletionPercentage());
        }
        return map;
    }
}
