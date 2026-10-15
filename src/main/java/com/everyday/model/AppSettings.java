package com.everyday.model;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class AppSettings {
    private int id = 1;
    private String dayStartTime = "00:00"; // HH:mm
    private boolean darkTheme = true;
    private boolean notificationsEnabled = true;

    public AppSettings() {}

    public AppSettings(int id, String dayStartTime, boolean darkTheme, boolean notificationsEnabled) {
        this.id = id;
        this.dayStartTime = dayStartTime != null ? dayStartTime : "00:00";
        this.darkTheme = darkTheme;
        this.notificationsEnabled = notificationsEnabled;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDayStartTime() { return dayStartTime; }
    public void setDayStartTime(String dayStartTime) { this.dayStartTime = dayStartTime; }

    public LocalTime getDayStartLocalTime() {
        try {
            return LocalTime.parse(dayStartTime, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            return LocalTime.MIDNIGHT;
        }
    }

    public boolean isDarkTheme() { return darkTheme; }
    public void setDarkTheme(boolean darkTheme) { this.darkTheme = darkTheme; }

    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }
}
