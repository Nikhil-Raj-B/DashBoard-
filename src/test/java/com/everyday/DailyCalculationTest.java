package com.everyday;

import com.everyday.database.DatabaseManager;
import com.everyday.model.*;
import com.everyday.model.DailyRecord.DayStatus;
import com.everyday.repository.*;
import com.everyday.service.DailyEngineService;
import com.everyday.service.TaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DailyCalculationTest {

    private File tempDb;
    private TaskRepository taskRepository;
    private TaskDailyInstanceRepository instanceRepository;
    private DailyRecordRepository dailyRecordRepository;
    private MissedRecordRepository missedRecordRepository;
    private SkippedDayRepository skippedDayRepository;
    private DailyEngineService dailyEngineService;
    private TaskService taskService;

    @BeforeEach
    public void setup() throws SQLException {
        tempDb = new File(System.getProperty("java.io.tmpdir"), "everyday_test_" + System.nanoTime() + ".db");
        DatabaseManager.initialize(tempDb.getAbsolutePath());

        taskRepository = new TaskRepository();
        instanceRepository = new TaskDailyInstanceRepository();
        dailyRecordRepository = new DailyRecordRepository();
        missedRecordRepository = new MissedRecordRepository();
        skippedDayRepository = new SkippedDayRepository();

        dailyEngineService = new DailyEngineService(
                taskRepository, instanceRepository, dailyRecordRepository,
                missedRecordRepository, skippedDayRepository
        );
        taskService = new TaskService(taskRepository, instanceRepository, dailyEngineService);
    }

    @AfterEach
    public void cleanup() {
        DatabaseManager.close();
        if (tempDb != null && tempDb.exists()) {
            tempDb.delete();
        }
    }

    @Test
    public void testDailyCalculationsExactRatios() {
        // Daily calculations: 0/8, 4/8, 5/8, 7/8, 8/8
        LocalDate date = LocalDate.of(2026, 10, 8);

        // 0/8 = 0.0%
        DailyRecord r0 = new DailyRecord(date, 8, 0);
        assertEquals(0.0, r0.getCompletionPercentage());
        assertEquals(DayStatus.INCOMPLETE, r0.getDayStatus());

        // 4/8 = 50.0%
        DailyRecord r4 = new DailyRecord(date, 8, 4);
        assertEquals(50.0, r4.getCompletionPercentage());
        assertEquals(DayStatus.INCOMPLETE, r4.getDayStatus());

        // 5/8 = 62.5%
        DailyRecord r5 = new DailyRecord(date, 8, 5);
        assertEquals(62.5, r5.getCompletionPercentage());
        assertEquals(DayStatus.INCOMPLETE, r5.getDayStatus());

        // 7/8 = 87.5%
        DailyRecord r7 = new DailyRecord(date, 8, 7);
        assertEquals(87.5, r7.getCompletionPercentage());
        assertEquals(DayStatus.INCOMPLETE, r7.getDayStatus());

        // 8/8 = 100.0%
        DailyRecord r8 = new DailyRecord(date, 8, 8);
        assertEquals(100.0, r8.getCompletionPercentage());
        assertEquals(DayStatus.COMPLETED, r8.getDayStatus());
    }

    @Test
    public void testLateAddedTaskDoesNotAffectDenominatorOrPercentage() throws SQLException {
        LocalDate today = LocalDate.of(2026, 10, 8);
        LocalDate yesterday = today.minusDays(1);

        // Create 8 tasks created yesterday (normal baseline tasks)
        for (int i = 1; i <= 8; i++) {
            Task t = new Task(0, "Task " + i, 1, null, "09:00", 30, null, yesterday, true, i, null, 0, null, false, yesterday);
            taskService.createTask(t, new TaskSchedule(TaskSchedule.ScheduleType.EVERY_DAY), yesterday);
        }

        // Prepare today
        dailyEngineService.prepareDay(today);

        // Complete 7 of the 8 tasks
        List<TaskDailyInstance> instances = instanceRepository.getInstancesForDate(today);
        assertEquals(8, instances.size());
        for (int i = 0; i < 7; i++) {
            dailyEngineService.toggleTaskCompletion(today, instances.get(i).getTaskId(), true);
        }

        DailyRecord beforeLateAdd = dailyEngineService.getDailyRecord(today);
        assertEquals(8, beforeLateAdd.getTotalActiveTasks());
        assertEquals(7, beforeLateAdd.getCompletedActiveTasks());
        assertEquals(87.5, beforeLateAdd.getCompletionPercentage());

        // Add a new task TODAY at 5 PM (created on `today`)
        Task lateTask = new Task(0, "Late Task Added At 5 PM", 1, null, "17:00", 45, null, today, true, 9, null, 0, null, false, today);
        taskService.createTask(lateTask, new TaskSchedule(TaskSchedule.ScheduleType.EVERY_DAY), today);

        // Re-evaluate today
        DailyRecord afterLateAdd = dailyEngineService.getDailyRecord(today);

        // Today's result MUST remain 7/8 = 87.5%
        assertEquals(8, afterLateAdd.getTotalActiveTasks(), "Late added task must not increase today's denominator!");
        assertEquals(7, afterLateAdd.getCompletedActiveTasks());
        assertEquals(87.5, afterLateAdd.getCompletionPercentage(), "Today's completion % must remain unaffected!");

        // The late task is marked as late addition
        TaskDailyInstance lateInst = instanceRepository.getInstance(today, lateTask.getId());
        assertNotNull(lateInst);
        assertTrue(lateInst.isLateAddition(), "Task created today must be flagged as late addition for today");

        // Even if user completes the late task today, today's baseline score is not modified
        dailyEngineService.toggleTaskCompletion(today, lateTask.getId(), true);
        DailyRecord afterLateComplete = dailyEngineService.getDailyRecord(today);
        assertEquals(87.5, afterLateComplete.getCompletionPercentage());

        // NEXT DAY (tomorrow): Late task becomes a normal active task!
        LocalDate tomorrow = today.plusDays(1);
        List<TaskDailyInstance> tomorrowInstances = dailyEngineService.prepareDay(tomorrow);
        assertEquals(9, tomorrowInstances.size());
        TaskDailyInstance tomorrowLateInst = instanceRepository.getInstance(tomorrow, lateTask.getId());
        assertNotNull(tomorrowLateInst);
        assertFalse(tomorrowLateInst.isLateAddition(), "Late task must become normal active task starting tomorrow!");

        DailyRecord tomorrowRecord = dailyEngineService.getDailyRecord(tomorrow);
        assertEquals(9, tomorrowRecord.getTotalActiveTasks(), "Tomorrow's denominator includes the task normally");
    }

    @Test
    public void testMissedTasksArchivalDoesNotAutoCarryOver() throws SQLException {
        LocalDate yesterday = LocalDate.of(2026, 10, 7);
        LocalDate today = LocalDate.of(2026, 10, 8);
        LocalDate dayBefore = yesterday.minusDays(1);
        Task t1 = new Task(0, "Completed Yesterday", 1, null, "09:00", 30, null, dayBefore, true, 1, null, 0, null, false, dayBefore);
        Task t2 = new Task(0, "Unfinished Yesterday", 1, null, "10:00", 30, null, dayBefore, true, 2, null, 0, null, false, dayBefore);
        taskService.createTask(t1, new TaskSchedule(TaskSchedule.ScheduleType.EVERY_DAY), dayBefore);
        taskService.createTask(t2, new TaskSchedule(TaskSchedule.ScheduleType.EVERY_DAY), dayBefore);

        dailyEngineService.prepareDay(yesterday);
        dailyEngineService.toggleTaskCompletion(yesterday, t1.getId(), true);
        // t2 is left incomplete on yesterday

        // Now advance to today and archive past days
        dailyEngineService.archiveMissedTasksForPastDays(today);

        // Missed tasks recorded
        List<MissedRecord> missed = missedRecordRepository.getByDate(yesterday);
        assertEquals(1, missed.size());
        assertEquals("Unfinished Yesterday", missed.get(0).getTaskName());

        // Prepare today's schedule
        List<TaskDailyInstance> todayInstances = dailyEngineService.prepareDay(today);
        DailyRecord todayRecord = dailyEngineService.getDailyRecord(today);

        // Yesterday's missed task does not increase today's denominator or carry over
        // Both tasks are scheduled for every day, so today has its own 2 fresh instances with completed = false
        for (TaskDailyInstance inst : todayInstances) {
            assertFalse(inst.isCompleted(), "Today's tasks start uncompleted");
        }
        assertEquals(2, todayRecord.getTotalActiveTasks());
        assertEquals(0, todayRecord.getCompletedActiveTasks());
        assertEquals(0.0, todayRecord.getCompletionPercentage());
    }

    @Test
    public void testLateTaskNeverBecomesMissedOnCreationDay() throws SQLException {
        LocalDate yesterday = LocalDate.of(2026, 10, 7);
        LocalDate today = LocalDate.of(2026, 10, 8);

        // Baseline task already existing on yesterday
        Task baseline = new Task(0, "Baseline Task", 1, null, "09:00", 30, null, yesterday.minusDays(1), true, 1, null, 0, null, false, yesterday.minusDays(1));
        taskService.createTask(baseline, new TaskSchedule(TaskSchedule.ScheduleType.EVERY_DAY), yesterday.minusDays(1));
        dailyEngineService.prepareDay(yesterday);
        dailyEngineService.toggleTaskCompletion(yesterday, baseline.getId(), true);

        // Task created late on yesterday (after day started)
        Task lateTask = new Task(0, "Late Task Created Yesterday", 1, null, "20:00", 30, null, yesterday, true, 2, null, 0, null, false, yesterday);
        taskService.createTask(lateTask, new TaskSchedule(TaskSchedule.ScheduleType.EVERY_DAY), yesterday);

        // Left incomplete on yesterday

        // Archive missed tasks
        dailyEngineService.archiveMissedTasksForPastDays(today);

        // MUST NOT become a missed task merely because it was created late
        List<MissedRecord> missed = missedRecordRepository.getByDate(yesterday);
        assertTrue(missed.isEmpty(), "Late task created on that day must NOT become a missed record for that day");
    }
}
