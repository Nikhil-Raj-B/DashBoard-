package com.everyday;

import com.everyday.database.DatabaseManager;
import com.everyday.model.Category;
import com.everyday.model.DailyRecord;
import com.everyday.model.Project;
import com.everyday.model.Task;
import com.everyday.model.TaskSchedule;
import com.everyday.repository.*;
import com.everyday.service.BackupService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class BackupRestoreTest {

    private File tempDb;
    private File backupJsonFile;
    private BackupService backupService;
    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    private CategoryRepository categoryRepository;

    @BeforeEach
    public void setup() throws SQLException {
        tempDb = new File(System.getProperty("java.io.tmpdir"), "everyday_backup_test_" + System.nanoTime() + ".db");
        backupJsonFile = new File(System.getProperty("java.io.tmpdir"), "everyday_backup_" + System.nanoTime() + ".json");
        DatabaseManager.initialize(tempDb.getAbsolutePath());

        ProfileRepository profileRepo = new ProfileRepository();
        SettingsRepository settingsRepo = new SettingsRepository();
        categoryRepository = new CategoryRepository();
        projectRepository = new ProjectRepository();
        taskRepository = new TaskRepository();
        DailyRecordRepository dailyRecordRepo = new DailyRecordRepository();
        MissedRecordRepository missedRecordRepo = new MissedRecordRepository();
        SkippedDayRepository skippedDayRepo = new SkippedDayRepository();
        StreakRepository streakRepo = new StreakRepository();

        backupService = new BackupService(
                profileRepo, settingsRepo, categoryRepository, projectRepository,
                taskRepository, dailyRecordRepo, missedRecordRepo, skippedDayRepo, streakRepo
        );
    }

    @AfterEach
    public void cleanup() {
        DatabaseManager.close();
        if (tempDb != null && tempDb.exists()) tempDb.delete();
        if (backupJsonFile != null && backupJsonFile.exists()) backupJsonFile.delete();
    }

    @Test
    public void testBackupExportAndRestore() throws Exception {
        LocalDate today = LocalDate.of(2026, 10, 8);

        // Seed sample data
        Project p = new Project(0, "Backup Project", "Testing backup", today, null, Project.ProjectStatus.ACTIVE, null, 50.0);
        projectRepository.insert(p);

        Task t = new Task(0, "Backup Task", 1, p.getId(), "09:00", 30, null, today, true, 1, null, 0, null, false, today);
        taskRepository.insert(t, new TaskSchedule(TaskSchedule.ScheduleType.EVERY_DAY));

        // Export
        backupService.exportBackupToJson(backupJsonFile);
        assertTrue(backupJsonFile.exists());
        assertTrue(backupJsonFile.length() > 0);

        // Validate
        assertDoesNotThrow(() -> backupService.validateBackup(backupJsonFile));

        // Restore into new database
        File restoreDb = new File(System.getProperty("java.io.tmpdir"), "everyday_restore_target_" + System.nanoTime() + ".db");
        DatabaseManager.close();
        DatabaseManager.initialize(restoreDb.getAbsolutePath());

        assertDoesNotThrow(() -> backupService.restoreBackupFromJson(backupJsonFile));

        // Verify data exists in restored database
        assertEquals("Backup Project", projectRepository.getById(p.getId()).getName());
        assertEquals("Backup Task", taskRepository.getById(t.getId()).getName());

        DatabaseManager.close();
        if (restoreDb.exists()) restoreDb.delete();
    }

    @Test
    public void testInvalidBackupValidationFails() throws Exception {
        File badFile = new File(System.getProperty("java.io.tmpdir"), "bad_backup_" + System.nanoTime() + ".json");
        try (FileWriter fw = new FileWriter(badFile)) {
            fw.write("{\"randomKey\": \"some value\"}");
        }

        assertThrows(IllegalArgumentException.class, () -> {
            backupService.validateBackup(badFile);
        });

        if (badFile.exists()) badFile.delete();
    }
}
