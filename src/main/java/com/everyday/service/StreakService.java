package com.everyday.service;

import com.everyday.model.DailyRecord;
import com.everyday.model.DailyRecord.StreakState;
import com.everyday.model.SkippedDay;
import com.everyday.model.StreakLevel;
import com.everyday.model.StreakRecord;
import com.everyday.model.TaskDailyInstance;
import com.everyday.repository.DailyRecordRepository;
import com.everyday.repository.SkippedDayRepository;
import com.everyday.repository.StreakRepository;
import com.everyday.repository.TaskDailyInstanceRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service managing overall and individual activity streaks.
 * Adheres strictly to locked threshold rules and skip day behavior.
 */
public class StreakService {

    private final StreakRepository streakRepository;
    private final DailyRecordRepository dailyRecordRepository;
    private final SkippedDayRepository skippedDayRepository;
    private final TaskDailyInstanceRepository instanceRepository;

    public StreakService(StreakRepository streakRepository,
                         DailyRecordRepository dailyRecordRepository,
                         SkippedDayRepository skippedDayRepository,
                         TaskDailyInstanceRepository instanceRepository) {
        this.streakRepository = streakRepository;
        this.dailyRecordRepository = dailyRecordRepository;
        this.skippedDayRepository = skippedDayRepository;
        this.instanceRepository = instanceRepository;
    }

    public StreakRecord getOverallStreak() throws SQLException {
        StreakRecord record = streakRepository.getByType("OVERALL");
        if (record == null) {
            record = new StreakRecord(0, "OVERALL", 0, 0, null, LocalDateTime.now());
            streakRepository.insertOrUpdate(record);
        }
        return record;
    }

    public List<StreakRecord> getAllStreaks() throws SQLException {
        return streakRepository.getAll();
    }

    public StreakRecord getActivityStreak(String activityName) throws SQLException {
        if (activityName == null || activityName.isBlank()) return getOverallStreak();
        StreakRecord record = streakRepository.getByType(activityName);
        if (record == null) {
            record = new StreakRecord(0, activityName.toUpperCase(), 0, 0, null, LocalDateTime.now());
            streakRepository.insertOrUpdate(record);
        }
        return record;
    }

    /**
     * Records an intentional skip for a day.
     * Reason is strictly REQUIRED.
     * Skipped day does not affect percentage, does not increase streak, does not break streak.
     */
    public SkippedDay skipDay(LocalDate date, String reason) throws SQLException {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("A skip reason is required.");
        }
        SkippedDay skippedDay = new SkippedDay(0, date, reason.trim(), LocalDateTime.now());
        skippedDayRepository.insert(skippedDay);

        // Update DailyRecord streak state to SKIPPED
        DailyRecord record = dailyRecordRepository.getByDate(date);
        if (record != null) {
            record.setStreakState(StreakState.SKIPPED);
            dailyRecordRepository.insertOrUpdate(record);
        }

        return skippedDay;
    }

    public boolean isDaySkipped(LocalDate date) throws SQLException {
        return skippedDayRepository.getByDate(date) != null;
    }

    public SkippedDay getSkippedDay(LocalDate date) throws SQLException {
        return skippedDayRepository.getByDate(date);
    }

    /**
     * Evaluates streaks chronologically across all recorded history up to the target date.
     * Idempotent and guarantees deterministic streaks.
     */
    public synchronized void recalculateAllStreaks(LocalDate upToDate) throws SQLException {
        List<DailyRecord> allRecords = dailyRecordRepository.getAll();
        if (allRecords.isEmpty()) {
            return;
        }

        // 1. Recalculate OVERALL streak
        int currentOverall = 0;
        int longestOverall = 0;
        LocalDate lastDate = null;

        for (DailyRecord record : allRecords) {
            LocalDate date = record.getLogicalDate();
            if (upToDate != null && date.isAfter(upToDate)) {
                continue;
            }

            // Check if day was intentionally skipped
            SkippedDay skipped = skippedDayRepository.getByDate(date);
            if (skipped != null) {
                record.setStreakState(StreakState.SKIPPED);
                dailyRecordRepository.insertOrUpdate(record);
                // Streak is preserved! Does not increase, does not break.
                continue;
            }

            // Empty days with no tasks scheduled: do not break streak if user didn't have any tasks
            if (record.getTotalActiveTasks() <= 0) {
                continue;
            }

            // Determine threshold for current streak count
            StreakLevel currentLevel = StreakLevel.forDays(currentOverall);
            double pct = record.getCompletionPercentage();

            if (currentLevel.meetsThreshold(pct)) {
                // Streak continues!
                currentOverall++;
                if (pct >= 100.0) {
                    record.setStreakState(StreakState.INCREASED);
                } else {
                    record.setStreakState(StreakState.CONTINUED);
                }
                if (currentOverall > longestOverall) {
                    longestOverall = currentOverall;
                }
            } else {
                // Fails threshold -> streak breaks
                currentOverall = 0;
                record.setStreakState(StreakState.BROKEN);
            }

            lastDate = date;
            dailyRecordRepository.insertOrUpdate(record);
        }

        StreakRecord overall = getOverallStreak();
        overall.setCurrentStreak(currentOverall);
        if (longestOverall > overall.getLongestStreak()) {
            overall.setLongestStreak(longestOverall);
        }
        overall.setLastEvaluatedDate(lastDate);
        overall.setUpdatedAt(LocalDateTime.now());
        streakRepository.insertOrUpdate(overall);

        // 2. Recalculate Activity Streaks
        recalculateActivityStreaks(upToDate);
    }

    private void recalculateActivityStreaks(LocalDate upToDate) throws SQLException {
        // Collect all distinct category names
        List<TaskDailyInstance> allInstances = new ArrayList<>();
        List<DailyRecord> records = dailyRecordRepository.getAll();
        Set<String> categories = new HashSet<>();

        for (DailyRecord rec : records) {
            if (upToDate != null && rec.getLogicalDate().isAfter(upToDate)) continue;
            List<TaskDailyInstance> insts = instanceRepository.getInstancesForDate(rec.getLogicalDate());
            for (TaskDailyInstance inst : insts) {
                if (inst.getCategoryNameSnapshot() != null) {
                    categories.add(inst.getCategoryNameSnapshot().trim());
                }
            }
        }

        for (String cat : categories) {
            if (cat.isEmpty()) continue;
            int catStreak = 0;
            int catLongest = 0;
            LocalDate catLast = null;

            for (DailyRecord rec : records) {
                LocalDate date = rec.getLogicalDate();
                if (upToDate != null && date.isAfter(upToDate)) continue;

                if (skippedDayRepository.getByDate(date) != null) {
                    continue; // Skip preserves activity streak
                }

                List<TaskDailyInstance> insts = instanceRepository.getInstancesForDate(date);
                List<TaskDailyInstance> catInsts = insts.stream()
                        .filter(i -> cat.equalsIgnoreCase(i.getCategoryNameSnapshot()) && !i.isDeletedOccurrence())
                        .toList();

                if (catInsts.isEmpty()) {
                    // No task in this category on this day -> neutral
                    continue;
                }

                long completed = catInsts.stream().filter(TaskDailyInstance::isCompleted).count();
                double catPct = ((double) completed / catInsts.size()) * 100.0;

                StreakLevel level = StreakLevel.forDays(catStreak);
                if (level.meetsThreshold(catPct)) {
                    catStreak++;
                    if (catStreak > catLongest) {
                        catLongest = catStreak;
                    }
                } else {
                    catStreak = 0;
                }
                catLast = date;
            }

            StreakRecord rec = getActivityStreak(cat);
            rec.setCurrentStreak(catStreak);
            if (catLongest > rec.getLongestStreak()) {
                rec.setLongestStreak(catLongest);
            }
            rec.setLastEvaluatedDate(catLast);
            rec.setUpdatedAt(LocalDateTime.now());
            streakRepository.insertOrUpdate(rec);
        }
    }
}
