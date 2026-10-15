package com.everyday.service;

import com.everyday.database.DatabaseManager;
import com.everyday.model.*;
import com.everyday.repository.*;
import com.google.gson.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class BackupService {

    public static class BackupData {
        public int version = 1;
        public String app = "EveryDay";
        public String exportedAt;
        public UserProfile profile;
        public AppSettings settings;
        public List<Category> categories;
        public List<Project> projects;
        public List<Task> tasks;
        public List<DailyRecord> dailyRecords;
        public List<MissedRecord> missedRecords;
        public List<SkippedDay> skippedDays;
        public List<StreakRecord> streaks;
    }

    private final ProfileRepository profileRepository;
    private final SettingsRepository settingsRepository;
    private final CategoryRepository categoryRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final DailyRecordRepository dailyRecordRepository;
    private final MissedRecordRepository missedRecordRepository;
    private final SkippedDayRepository skippedDayRepository;
    private final StreakRepository streakRepository;
    private final Gson gson;

    public BackupService(ProfileRepository profileRepository,
                         SettingsRepository settingsRepository,
                         CategoryRepository categoryRepository,
                         ProjectRepository projectRepository,
                         TaskRepository taskRepository,
                         DailyRecordRepository dailyRecordRepository,
                         MissedRecordRepository missedRecordRepository,
                         SkippedDayRepository skippedDayRepository,
                         StreakRepository streakRepository) {
        this.profileRepository = profileRepository;
        this.settingsRepository = settingsRepository;
        this.categoryRepository = categoryRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.dailyRecordRepository = dailyRecordRepository;
        this.missedRecordRepository = missedRecordRepository;
        this.skippedDayRepository = skippedDayRepository;
        this.streakRepository = streakRepository;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, type, context) -> new JsonPrimitive(src.toString()))
                .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, type, context) -> LocalDate.parse(json.getAsString()))
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, type, context) -> new JsonPrimitive(src.toString()))
                .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, type, context) -> LocalDateTime.parse(json.getAsString()))
                .setPrettyPrinting()
                .create();
    }

    public void exportBackupToJson(File targetFile) throws Exception {
        BackupData data = new BackupData();
        data.exportedAt = LocalDateTime.now().toString();
        data.profile = profileRepository.getProfile();
        data.settings = settingsRepository.getSettings();
        data.categories = categoryRepository.getAll();
        data.projects = projectRepository.getAll();
        data.tasks = taskRepository.getAll();
        data.dailyRecords = dailyRecordRepository.getAll();
        data.missedRecords = missedRecordRepository.getRecent(100000);
        data.skippedDays = skippedDayRepository.getAll();
        data.streaks = streakRepository.getAll();

        try (FileWriter writer = new FileWriter(targetFile)) {
            gson.toJson(data, writer);
        }
    }

    public void validateBackup(File sourceFile) throws Exception {
        if (!sourceFile.exists() || sourceFile.length() == 0) {
            throw new IllegalArgumentException("The backup file is empty or does not exist.");
        }
        try (FileReader reader = new FileReader(sourceFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (!json.has("app") || !"EveryDay".equals(json.get("app").getAsString())) {
                throw new IllegalArgumentException("Invalid backup file: Not a valid EveryDay backup package.");
            }
            if (!json.has("version")) {
                throw new IllegalArgumentException("Invalid backup file: Missing version metadata.");
            }
        }
    }

    public void restoreBackupFromJson(File sourceFile) throws Exception {
        validateBackup(sourceFile);

        BackupData data;
        try (FileReader reader = new FileReader(sourceFile)) {
            data = gson.fromJson(reader, BackupData.class);
        }

        if (data == null) {
            throw new IllegalArgumentException("Could not parse backup content.");
        }

        if (data.profile != null) {
            profileRepository.updateProfile(data.profile);
        }
        if (data.settings != null) {
            settingsRepository.updateSettings(data.settings);
        }
        if (data.categories != null) {
            for (Category c : data.categories) {
                if (categoryRepository.getByName(c.getName()) == null) {
                    categoryRepository.insert(c);
                }
            }
        }
        if (data.projects != null) {
            for (Project p : data.projects) {
                if (projectRepository.getById(p.getId()) == null) {
                    projectRepository.insert(p);
                }
            }
        }
        if (data.tasks != null) {
            for (Task t : data.tasks) {
                if (taskRepository.getById(t.getId()) == null) {
                    taskRepository.insert(t, t.getSchedule());
                }
            }
        }
        if (data.dailyRecords != null) {
            for (DailyRecord dr : data.dailyRecords) {
                dailyRecordRepository.insertOrUpdate(dr);
            }
        }
        if (data.missedRecords != null) {
            for (MissedRecord mr : data.missedRecords) {
                missedRecordRepository.insert(mr);
            }
        }
        if (data.skippedDays != null) {
            for (SkippedDay sd : data.skippedDays) {
                skippedDayRepository.insert(sd);
            }
        }
        if (data.streaks != null) {
            for (StreakRecord sr : data.streaks) {
                streakRepository.insertOrUpdate(sr);
            }
        }
    }

    public void exportDatabaseFile(File targetFile) throws Exception {
        String dbPath = DatabaseManager.getDbPath();
        if (dbPath != null) {
            File currentDb = new File(dbPath);
            Files.copy(currentDb.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
