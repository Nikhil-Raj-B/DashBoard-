package com.everyday.service;

import com.everyday.model.AppSettings;
import com.everyday.repository.*;
import com.everyday.util.DateUtils;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Central service registry providing access to services across controllers.
 */
public class ServiceRegistry {

    private static ServiceRegistry instance;

    private final ProfileRepository profileRepository;
    private final SettingsRepository settingsRepository;
    private final CategoryRepository categoryRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final DailyRecordRepository dailyRecordRepository;
    private final TaskDailyInstanceRepository taskDailyInstanceRepository;
    private final MissedRecordRepository missedRecordRepository;
    private final SkippedDayRepository skippedDayRepository;
    private final StreakRepository streakRepository;

    private final ProfileService profileService;
    private final SettingsService settingsService;
    private final DailyEngineService dailyEngineService;
    private final TaskService taskService;
    private final ProjectService projectService;
    private final StreakService streakService;
    private final CalendarService calendarService;
    private final AnalyticsService analyticsService;
    private final BackupService backupService;
    private final NotificationService notificationService;

    public ServiceRegistry() {
        this.profileRepository = new ProfileRepository();
        this.settingsRepository = new SettingsRepository();
        this.categoryRepository = new CategoryRepository();
        this.projectRepository = new ProjectRepository();
        this.taskRepository = new TaskRepository();
        this.dailyRecordRepository = new DailyRecordRepository();
        this.taskDailyInstanceRepository = new TaskDailyInstanceRepository();
        this.missedRecordRepository = new MissedRecordRepository();
        this.skippedDayRepository = new SkippedDayRepository();
        this.streakRepository = new StreakRepository();

        this.profileService = new ProfileService(profileRepository);
        this.settingsService = new SettingsService(settingsRepository);
        this.dailyEngineService = new DailyEngineService(
                taskRepository, taskDailyInstanceRepository, dailyRecordRepository,
                missedRecordRepository, skippedDayRepository
        );
        this.taskService = new TaskService(taskRepository, taskDailyInstanceRepository, dailyEngineService);
        this.projectService = new ProjectService(projectRepository, taskRepository);
        this.streakService = new StreakService(
                streakRepository, dailyRecordRepository, skippedDayRepository, taskDailyInstanceRepository
        );
        this.calendarService = new CalendarService(
                dailyRecordRepository, taskDailyInstanceRepository, missedRecordRepository, skippedDayRepository
        );
        this.analyticsService = new AnalyticsService(
                dailyRecordRepository, taskDailyInstanceRepository, missedRecordRepository
        );
        this.backupService = new BackupService(
                profileRepository, settingsRepository, categoryRepository, projectRepository,
                taskRepository, dailyRecordRepository, missedRecordRepository, skippedDayRepository, streakRepository
        );
        this.notificationService = new NotificationService();
    }

    public static synchronized ServiceRegistry getInstance() {
        if (instance == null) {
            instance = new ServiceRegistry();
        }
        return instance;
    }

    /**
     * Resolves the current logical date based on the user's configured day start time.
     */
    public LocalDate getCurrentLogicalDate() {
        try {
            AppSettings settings = settingsRepository.getSettings();
            return DateUtils.getLogicalDate(LocalDateTime.now(), settings.getDayStartLocalTime());
        } catch (SQLException e) {
            return LocalDate.now();
        }
    }

    public ProfileRepository getProfileRepository() { return profileRepository; }
    public SettingsRepository getSettingsRepository() { return settingsRepository; }
    public CategoryRepository getCategoryRepository() { return categoryRepository; }
    public ProjectRepository getProjectRepository() { return projectRepository; }
    public TaskRepository getTaskRepository() { return taskRepository; }
    public DailyRecordRepository getDailyRecordRepository() { return dailyRecordRepository; }
    public TaskDailyInstanceRepository getTaskDailyInstanceRepository() { return taskDailyInstanceRepository; }
    public MissedRecordRepository getMissedRecordRepository() { return missedRecordRepository; }
    public SkippedDayRepository getSkippedDayRepository() { return skippedDayRepository; }
    public StreakRepository getStreakRepository() { return streakRepository; }

    public ProfileService getProfileService() { return profileService; }
    public SettingsService getSettingsService() { return settingsService; }
    public DailyEngineService getDailyEngineService() { return dailyEngineService; }
    public TaskService getTaskService() { return taskService; }
    public ProjectService getProjectService() { return projectService; }
    public StreakService getStreakService() { return streakService; }
    public CalendarService getCalendarService() { return calendarService; }
    public AnalyticsService getAnalyticsService() { return analyticsService; }
    public BackupService getBackupService() { return backupService; }
    public NotificationService getNotificationService() { return notificationService; }
}
