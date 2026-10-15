package com.everyday;

import com.everyday.database.DatabaseManager;
import com.everyday.model.*;
import com.everyday.repository.*;
import com.everyday.service.DailyEngineService;
import com.everyday.service.ProjectService;
import com.everyday.service.SettingsService;
import com.everyday.service.TaskService;
import com.everyday.util.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProjectAndHistoryTest {

    private File tempDb;
    private ProjectRepository projectRepository;
    private TaskRepository taskRepository;
    private TaskDailyInstanceRepository instanceRepository;
    private DailyRecordRepository dailyRecordRepository;
    private MissedRecordRepository missedRecordRepository;
    private SkippedDayRepository skippedDayRepository;
    private SettingsRepository settingsRepository;

    private ProjectService projectService;
    private DailyEngineService dailyEngineService;
    private TaskService taskService;
    private SettingsService settingsService;

    @BeforeEach
    public void setup() throws SQLException {
        tempDb = new File(System.getProperty("java.io.tmpdir"), "everyday_proj_test_" + System.nanoTime() + ".db");
        DatabaseManager.initialize(tempDb.getAbsolutePath());

        projectRepository = new ProjectRepository();
        taskRepository = new TaskRepository();
        instanceRepository = new TaskDailyInstanceRepository();
        dailyRecordRepository = new DailyRecordRepository();
        missedRecordRepository = new MissedRecordRepository();
        skippedDayRepository = new SkippedDayRepository();
        settingsRepository = new SettingsRepository();

        projectService = new ProjectService(projectRepository, taskRepository);
        dailyEngineService = new DailyEngineService(
                taskRepository, instanceRepository, dailyRecordRepository,
                missedRecordRepository, skippedDayRepository
        );
        taskService = new TaskService(taskRepository, instanceRepository, dailyEngineService);
        settingsService = new SettingsService(settingsRepository);
    }

    @AfterEach
    public void cleanup() {
        DatabaseManager.close();
        if (tempDb != null && tempDb.exists()) {
            tempDb.delete();
        }
    }

    @Test
    public void testProjectProgressIsCompletelyIndependent() throws SQLException {
        LocalDate today = LocalDate.of(2026, 10, 8);

        // Project 1: 65% progress
        Project p1 = new Project(0, "JavaFX Desktop", "Desktop dashboard", today, today.plusMonths(1), Project.ProjectStatus.ACTIVE, null, 65.0);
        projectService.createProject(p1);

        // Project 2: 32% progress
        Project p2 = new Project(0, "AI Engine", "Neural network integration", today, today.plusMonths(2), Project.ProjectStatus.ACTIVE, null, 32.0);
        projectService.createProject(p2);

        // Daily Tasks: 7 out of 8 completed = 87.5%
        LocalDate yesterday = today.minusDays(1);
        for (int i = 1; i <= 8; i++) {
            Task t = new Task(0, "Daily Task " + i, 1, p1.getId(), "09:00", 30, null, yesterday, true, i, null, 0, null, false, yesterday);
            taskService.createTask(t, new TaskSchedule(TaskSchedule.ScheduleType.EVERY_DAY), yesterday);
        }

        dailyEngineService.prepareDay(today);
        List<TaskDailyInstance> insts = instanceRepository.getInstancesForDate(today);
        for (int i = 0; i < 7; i++) {
            dailyEngineService.toggleTaskCompletion(today, insts.get(i).getTaskId(), true);
        }

        DailyRecord daily = dailyEngineService.getDailyRecord(today);

        // Verify Daily completion is purely based on active daily tasks
        assertEquals(87.5, daily.getCompletionPercentage(), "Daily completion must be 87.5% and NEVER averaged with project progress!");

        // Verify Project progress remains exactly as set
        Project freshP1 = projectService.getProjectById(p1.getId());
        Project freshP2 = projectService.getProjectById(p2.getId());
        assertEquals(65.0, freshP1.getProgress());
        assertEquals(32.0, freshP2.getProgress());
    }

    @Test
    public void testHistoricalRecordsPreservedWhenTaskModifiedLater() throws SQLException {
        LocalDate day0 = LocalDate.of(2026, 9, 30);
        LocalDate day1 = LocalDate.of(2026, 10, 1);
        LocalDate day2 = LocalDate.of(2026, 10, 2);

        // Create task on Day 0
        Task t = new Task(0, "Original Task Name", 1, null, "09:00", 30, "Original notes", day0, true, 1, null, 0, null, false, day0);
        taskService.createTask(t, new TaskSchedule(TaskSchedule.ScheduleType.EVERY_DAY), day0);

        dailyEngineService.prepareDay(day1);
        dailyEngineService.toggleTaskCompletion(day1, t.getId(), true);

        DailyRecord day1Record = dailyEngineService.getDailyRecord(day1);
        assertEquals(100.0, day1Record.getCompletionPercentage());

        // Snapshot on Day 1 has "Original Task Name"
        TaskDailyInstance day1Inst = instanceRepository.getInstance(day1, t.getId());
        assertEquals("Original Task Name", day1Inst.getTaskNameSnapshot());

        // User edits the task on Day 2: changes name, duration, notes
        t.setName("Renamed Task Name");
        t.setDurationMinutes(120);
        t.setNotes("New modified notes");
        taskService.updateTask(t, new TaskSchedule(TaskSchedule.ScheduleType.EVERY_DAY), day2);

        // Day 1 historical record and instance MUST NOT BE DESTROYED OR ALTERED
        TaskDailyInstance day1InstAfterEdit = instanceRepository.getInstance(day1, t.getId());
        assertEquals("Original Task Name", day1InstAfterEdit.getTaskNameSnapshot(), "Day 1 snapshot must preserve original historical task name!");

        DailyRecord day1RecordAfterEdit = dailyEngineService.getDailyRecord(day1);
        assertEquals(100.0, day1RecordAfterEdit.getCompletionPercentage(), "Day 1 historical completion must remain intact!");
    }

    @Test
    public void testDayStartSettingChangeDoesNotRewriteHistory() throws SQLException {
        LocalDate day1 = LocalDate.of(2026, 10, 1);

        DailyRecord past = new DailyRecord(day1, 5, 5); // 100%
        dailyRecordRepository.insertOrUpdate(past);

        // Change logical day start time from 00:00 to 04:00 AM
        settingsService.updateSettings("04:00", true, true);

        // Check past record
        DailyRecord retrievedPast = dailyRecordRepository.getByDate(day1);
        assertNotNull(retrievedPast);
        assertEquals(100.0, retrievedPast.getCompletionPercentage(), "Changing day start time must not alter past records!");

        // Verify logical date calculation logic
        LocalDateTime earlyMorning = LocalDateTime.of(2026, 10, 8, 2, 30); // 2:30 AM
        LocalDate logicalDate = DateUtils.getLogicalDate(earlyMorning, LocalTime.of(4, 0));
        assertEquals(LocalDate.of(2026, 10, 7), logicalDate, "Before 4:00 AM belongs to previous logical day");

        LocalDateTime lateMorning = LocalDateTime.of(2026, 10, 8, 5, 30); // 5:30 AM
        LocalDate logicalDate2 = DateUtils.getLogicalDate(lateMorning, LocalTime.of(4, 0));
        assertEquals(LocalDate.of(2026, 10, 8), logicalDate2, "After 4:00 AM belongs to current day");
    }
}
