package com.everyday.service;

import com.everyday.model.DailyRecord;
import com.everyday.model.DailyRecord.DayStatus;
import com.everyday.model.DailyRecord.StreakState;
import com.everyday.model.MissedRecord;
import com.everyday.model.SkippedDay;
import com.everyday.model.Task;
import com.everyday.model.TaskDailyInstance;
import com.everyday.repository.DailyRecordRepository;
import com.everyday.repository.MissedRecordRepository;
import com.everyday.repository.SkippedDayRepository;
import com.everyday.repository.TaskDailyInstanceRepository;
import com.everyday.repository.TaskRepository;
import com.everyday.util.DateUtils;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Core engine managing logical days, daily task instances,
 * percentage calculation, missed task archival, and late-added task handling.
 */
public class DailyEngineService {

    private final TaskRepository taskRepository;
    private final TaskDailyInstanceRepository instanceRepository;
    private final DailyRecordRepository dailyRecordRepository;
    private final MissedRecordRepository missedRecordRepository;
    private final SkippedDayRepository skippedDayRepository;

    public DailyEngineService(TaskRepository taskRepository,
                              TaskDailyInstanceRepository instanceRepository,
                              DailyRecordRepository dailyRecordRepository,
                              MissedRecordRepository missedRecordRepository,
                              SkippedDayRepository skippedDayRepository) {
        this.taskRepository = taskRepository;
        this.instanceRepository = instanceRepository;
        this.dailyRecordRepository = dailyRecordRepository;
        this.missedRecordRepository = missedRecordRepository;
        this.skippedDayRepository = skippedDayRepository;
    }

    /**
     * Prepares and returns the daily instances for the given logical date.
     * Ensures all active scheduled tasks have a daily instance.
     */
    public synchronized List<TaskDailyInstance> prepareDay(LocalDate logicalDate) throws SQLException {
        List<Task> allTasks = taskRepository.getAll();
        List<TaskDailyInstance> existingInstances = instanceRepository.getInstancesForDate(logicalDate);
        DailyRecord existingRecord = dailyRecordRepository.getByDate(logicalDate);
        boolean dayAlreadyStarted = (existingRecord != null && existingRecord.getTotalActiveTasks() > 0) || !existingInstances.isEmpty();

        // Clean up uncompleted instances of inactive tasks
        for (TaskDailyInstance inst : existingInstances) {
            allTasks.stream()
                    .filter(t -> t.getId() == inst.getTaskId())
                    .findFirst()
                    .ifPresent(t -> {
                        if (!t.isActive() && !inst.isCompleted()) {
                            try {
                                instanceRepository.deleteInstance(logicalDate, inst.getTaskId());
                            } catch (SQLException ignored) {}
                        }
                    });
        }

        for (Task task : allTasks) {
            // Only consider tasks marked active (ON/OFF toggle)
            if (!task.isActive()) {
                continue;
            }

            // Check if scheduled for this date
            if (task.getSchedule() != null && !task.getSchedule().isScheduledFor(logicalDate, task.getStartDate())) {
                continue;
            }

            // Check if instance already exists
            boolean exists = existingInstances.stream().anyMatch(i -> i.getTaskId() == task.getId());
            if (!exists) {
                // If created after day already started on this logical date, it was added late
                boolean isLateAddition = dayAlreadyStarted && task.getCreatedLogicalDate() != null && task.getCreatedLogicalDate().equals(logicalDate);

                TaskDailyInstance newInstance = new TaskDailyInstance();
                newInstance.setLogicalDate(logicalDate);
                newInstance.setTaskId(task.getId());
                newInstance.setTaskNameSnapshot(task.getName());
                newInstance.setCategoryNameSnapshot(task.getCategory() != null ? task.getCategory().getName() : "General");
                newInstance.setProjectNameSnapshot(task.getProject() != null ? task.getProject().getName() : null);
                newInstance.setLateAddition(isLateAddition);
                newInstance.setCompleted(false);
                newInstance.setDisplayOrder(task.getDisplayOrder());
                newInstance.setDeletedOccurrence(false);

                instanceRepository.insertOrUpdate(newInstance);
            }
        }

        // Re-fetch with attached runtime task objects
        List<TaskDailyInstance> instances = instanceRepository.getInstancesForDate(logicalDate);
        for (TaskDailyInstance inst : instances) {
            allTasks.stream().filter(t -> t.getId() == inst.getTaskId()).findFirst().ifPresent(inst::setTask);
        }

        // Recalculate daily aggregate
        calculateAndSaveDailyRecord(logicalDate, instances);

        return instances;
    }

    /**
     * Calculates the daily completion statistics for the logical date:
     * - Every active daily task has equal weight.
     * - Late-added tasks MUST NOT affect today's completion percentage or denominator.
     * - completion = completed baseline tasks / total baseline tasks * 100.
     */
    public synchronized DailyRecord calculateAndSaveDailyRecord(LocalDate logicalDate, List<TaskDailyInstance> instances) throws SQLException {
        if (instances == null) {
            instances = instanceRepository.getInstancesForDate(logicalDate);
        }

        SkippedDay skipped = skippedDayRepository.getByDate(logicalDate);

        int totalActiveTasks = 0;
        int completedActiveTasks = 0;

        for (TaskDailyInstance inst : instances) {
            if (inst.isDeletedOccurrence()) {
                continue;
            }
            // CRITICAL RULE: Late-added tasks MUST NOT be added to denominator or numerator for today!
            if (!inst.isLateAddition()) {
                totalActiveTasks++;
                if (inst.isCompleted()) {
                    completedActiveTasks++;
                }
            }
        }

        DailyRecord record = dailyRecordRepository.getByDate(logicalDate);
        if (record == null) {
            record = new DailyRecord();
            record.setLogicalDate(logicalDate);
        }

        record.setTotalActiveTasks(totalActiveTasks);
        record.setCompletedActiveTasks(completedActiveTasks);
        record.recalculate();

        if (skipped != null) {
            record.setStreakState(StreakState.SKIPPED);
        }

        record.setEvaluatedAt(LocalDateTime.now());
        dailyRecordRepository.insertOrUpdate(record);

        return record;
    }

    /**
     * Toggles completion for a task on the specified logical date.
     * Allows undo while the day is active.
     */
    public synchronized DailyRecord toggleTaskCompletion(LocalDate logicalDate, int taskId, boolean completed) throws SQLException {
        LocalDateTime completedAt = completed ? LocalDateTime.now() : null;
        instanceRepository.markCompleted(logicalDate, taskId, completed, completedAt);
        return calculateAndSaveDailyRecord(logicalDate, null);
    }

    /**
     * Marks an occurrence as deleted for this specific day.
     * Does not count as completed, does not count as missed.
     */
    public synchronized DailyRecord deleteOccurrence(LocalDate logicalDate, int taskId) throws SQLException {
        instanceRepository.markDeletedOccurrence(logicalDate, taskId);
        return calculateAndSaveDailyRecord(logicalDate, null);
    }

    /**
     * Evaluates past un-finalized days up to currentLogicalDate - 1:
     * - Unfinished tasks that are NOT late additions become Missed Records.
     * - Late additions never become missed merely because they were created late.
     * - Missed tasks do NOT carry over to today's active tasks.
     */
    public synchronized void archiveMissedTasksForPastDays(LocalDate currentLogicalDate) throws SQLException {
        List<DailyRecord> pastRecords = dailyRecordRepository.getAll();
        for (DailyRecord past : pastRecords) {
            LocalDate pastDate = past.getLogicalDate();
            if (pastDate.isBefore(currentLogicalDate)) {
                // Check if skipped
                SkippedDay skipped = skippedDayRepository.getByDate(pastDate);
                if (skipped != null) {
                    continue; // Skipped days don't generate missed tasks
                }

                List<TaskDailyInstance> instances = instanceRepository.getInstancesForDate(pastDate);
                for (TaskDailyInstance inst : instances) {
                    if (!inst.isCompleted() && !inst.isDeletedOccurrence() && !inst.isLateAddition()) {
                        MissedRecord missed = new MissedRecord(
                                0,
                                pastDate,
                                inst.getTaskId(),
                                inst.getTaskNameSnapshot(),
                                inst.getCategoryNameSnapshot(),
                                inst.getProjectNameSnapshot(),
                                LocalDateTime.now()
                        );
                        missedRecordRepository.insert(missed);
                    }
                }
            }
        }
    }

    /**
     * Gets missed tasks for recent days (e.g. yesterday or recent).
     */
    public List<MissedRecord> getRecentMissedTasks(int limit) throws SQLException {
        return missedRecordRepository.getRecent(limit);
    }

    public List<MissedRecord> getMissedTasksForDate(LocalDate date) throws SQLException {
        return missedRecordRepository.getByDate(date);
    }

    public DailyRecord getDailyRecord(LocalDate date) throws SQLException {
        DailyRecord record = dailyRecordRepository.getByDate(date);
        if (record == null) {
            record = calculateAndSaveDailyRecord(date, null);
        }
        return record;
    }
}
