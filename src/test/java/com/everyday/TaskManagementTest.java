package com.everyday;

import com.everyday.database.DatabaseManager;
import com.everyday.model.Category;
import com.everyday.model.DailyRecord;
import com.everyday.model.Task;
import com.everyday.model.TaskDailyInstance;
import com.everyday.model.TaskSchedule;
import com.everyday.model.TaskSchedule.ScheduleType;
import com.everyday.repository.*;
import com.everyday.service.DailyEngineService;
import com.everyday.service.TaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TaskManagementTest {

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
        tempDb = new File(System.getProperty("java.io.tmpdir"), "everyday_task_test_" + System.nanoTime() + ".db");
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
    public void testTaskCreationEditingDeletion() throws SQLException {
        LocalDate today = LocalDate.of(2026, 10, 8);

        // 1. Creation
        Task t = new Task(0, "Learn Rust Basics", 1, null, "10:00", 45, "Memory safety", today, true, 1, null, 0, null, false, today);
        TaskSchedule sched = new TaskSchedule(ScheduleType.EVERY_DAY);
        Task created = taskService.createTask(t, sched, today);

        assertTrue(created.getId() > 0);
        Task retrieved = taskService.getTaskById(created.getId());
        assertNotNull(retrieved);
        assertEquals("Learn Rust Basics", retrieved.getName());
        assertEquals(45, retrieved.getDurationMinutes());

        // 2. Editing
        retrieved.setName("Learn Advanced Rust");
        retrieved.setDurationMinutes(60);
        taskService.updateTask(retrieved, sched, today);

        Task updated = taskService.getTaskById(created.getId());
        assertEquals("Learn Advanced Rust", updated.getName());
        assertEquals(60, updated.getDurationMinutes());

        // 3. Deletion
        taskService.deleteTask(created.getId(), today);
        assertNull(taskService.getTaskById(created.getId()));
    }

    @Test
    public void testTaskCompletionAndUndo() throws SQLException {
        LocalDate today = LocalDate.of(2026, 10, 8);

        Task t = new Task(0, "Read 20 pages", 6, null, "14:00", 30, null, today.minusDays(1), true, 1, null, 0, null, false, today.minusDays(1));
        taskService.createTask(t, new TaskSchedule(ScheduleType.EVERY_DAY), today.minusDays(1));

        dailyEngineService.prepareDay(today);

        // Complete
        DailyRecord recordAfterComp = dailyEngineService.toggleTaskCompletion(today, t.getId(), true);
        assertEquals(1, recordAfterComp.getCompletedActiveTasks());
        assertEquals(100.0, recordAfterComp.getCompletionPercentage());

        // Undo while day is active
        DailyRecord recordAfterUndo = dailyEngineService.toggleTaskCompletion(today, t.getId(), false);
        assertEquals(0, recordAfterUndo.getCompletedActiveTasks());
        assertEquals(0.0, recordAfterUndo.getCompletionPercentage());
    }

    @Test
    public void testOrderingPersistence() throws SQLException {
        LocalDate today = LocalDate.of(2026, 10, 8);

        Task t1 = new Task(0, "Task Alpha", 1, null, "08:00", 30, null, today, true, 1, null, 0, null, false, today);
        Task t2 = new Task(0, "Task Beta", 1, null, "09:00", 30, null, today, true, 2, null, 0, null, false, today);
        taskService.createTask(t1, new TaskSchedule(ScheduleType.EVERY_DAY), today);
        taskService.createTask(t2, new TaskSchedule(ScheduleType.EVERY_DAY), today);

        // Reorder
        taskService.updateDisplayOrder(t1.getId(), 5);
        taskService.updateDisplayOrder(t2.getId(), 2);

        Task fresh1 = taskService.getTaskById(t1.getId());
        Task fresh2 = taskService.getTaskById(t2.getId());

        assertEquals(5, fresh1.getDisplayOrder());
        assertEquals(2, fresh2.getDisplayOrder());
    }

    @Test
    public void testTaskOnOffToggle() throws SQLException {
        LocalDate today = LocalDate.of(2026, 10, 8);

        Task t = new Task(0, "Morning Yoga", 3, null, "07:00", 30, null, today.minusDays(1), true, 1, null, 0, null, false, today.minusDays(1));
        taskService.createTask(t, new TaskSchedule(ScheduleType.EVERY_DAY), today);

        // Turn OFF (paused)
        taskService.toggleActive(t.getId(), false, today);

        Task fresh = taskService.getTaskById(t.getId());
        assertFalse(fresh.isActive(), "Task should be OFF");

        // Prepare day: OFF tasks must NOT be included in active tasks
        List<TaskDailyInstance> instances = dailyEngineService.prepareDay(today);
        assertTrue(instances.isEmpty(), "OFF task must not appear in active day schedule");

        // Turn ON again
        taskService.toggleActive(t.getId(), true, today);
        List<TaskDailyInstance> reInstances = dailyEngineService.prepareDay(today);
        assertEquals(1, reInstances.size(), "ON task should appear in daily schedule");
    }

    @Test
    public void testRecurringAndCustomSchedules() {
        LocalDate monday = LocalDate.of(2026, 10, 5); // Monday
        LocalDate tuesday = LocalDate.of(2026, 10, 6); // Tuesday
        LocalDate wednesday = LocalDate.of(2026, 10, 7); // Wednesday

        // Specific Weekdays: Monday and Wednesday only
        TaskSchedule weekdaySched = new TaskSchedule(ScheduleType.SPECIFIC_WEEKDAYS);
        weekdaySched.setWeekdays("MONDAY,WEDNESDAY");

        assertTrue(weekdaySched.isScheduledFor(monday, monday));
        assertFalse(weekdaySched.isScheduledFor(tuesday, monday));
        assertTrue(weekdaySched.isScheduledFor(wednesday, monday));

        // Custom interval: every 3 days
        TaskSchedule intervalSched = new TaskSchedule(ScheduleType.CUSTOM_INTERVAL);
        intervalSched.setIntervalDays(3);
        intervalSched.setStartDate(monday);

        assertTrue(intervalSched.isScheduledFor(monday, monday)); // Day 0
        assertFalse(intervalSched.isScheduledFor(tuesday, monday)); // Day 1
        assertFalse(intervalSched.isScheduledFor(wednesday, monday)); // Day 2
        assertTrue(intervalSched.isScheduledFor(monday.plusDays(3), monday)); // Day 3 (Thursday)

        // Custom dates
        TaskSchedule customDatesSched = new TaskSchedule(ScheduleType.CUSTOM_DATES);
        customDatesSched.setCustomDates("2026-10-06,2026-10-20");

        assertFalse(customDatesSched.isScheduledFor(monday, monday));
        assertTrue(customDatesSched.isScheduledFor(tuesday, monday));
    }

    @Test
    public void testMultiDayTaskProgress() throws SQLException {
        LocalDate start = LocalDate.of(2026, 10, 10);
        LocalDate end = LocalDate.of(2026, 10, 16); // 7 days

        Task multiDay = new Task(0, "Build Dashboard", 1, null, "10:00", 60, null, start, true, 1, end, 45.0, null, false, start);
        TaskSchedule sched = new TaskSchedule(ScheduleType.MULTI_DAY);
        sched.setStartDate(start);
        sched.setEndDate(end);

        Task created = taskService.createTask(multiDay, sched, start);

        assertTrue(created.isMultiDay());
        assertEquals("Day 1/7", created.getMultiDayDayProgressString(start));
        assertEquals("Day 3/7", created.getMultiDayDayProgressString(start.plusDays(2)));
        assertEquals("Day 7/7", created.getMultiDayDayProgressString(end));

        // Independent task progress (45%)
        assertEquals(45.0, created.getTaskProgress());

        // Update progress
        taskService.updateTaskProgress(created.getId(), 75.0);
        Task fresh = taskService.getTaskById(created.getId());
        assertEquals(75.0, fresh.getTaskProgress());
    }
}
