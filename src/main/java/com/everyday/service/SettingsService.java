package com.everyday.service;

import com.everyday.model.AppSettings;
import com.everyday.repository.SettingsRepository;

import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class SettingsService {

    private final SettingsRepository settingsRepository;

    public SettingsService(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    public AppSettings getSettings() throws SQLException {
        return settingsRepository.getSettings();
    }

    /**
     * Updates settings. CRITICAL: Changing logical day start MUST NOT rewrite historical data.
     */
    public void updateSettings(String dayStartTime, boolean darkTheme, boolean notificationsEnabled) throws SQLException {
        // Validate dayStartTime
        try {
            LocalTime.parse(dayStartTime, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            dayStartTime = "00:00";
        }
        AppSettings settings = new AppSettings(1, dayStartTime, darkTheme, notificationsEnabled);
        settingsRepository.updateSettings(settings);
    }
}
