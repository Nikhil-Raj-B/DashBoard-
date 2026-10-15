package com.everyday.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Task {
    private int id;
    private String name;
    private int categoryId;
    private Integer projectId;
    private String scheduledTime; // "09:00"
    private int durationMinutes = 30;
    private String notes;
    private LocalDate startDate;
    private boolean active = true; // ON / OFF
    private int displayOrder = 0;
    private LocalDate multiDayEndDate;
    private double taskProgress = 0.0; // Independent task progress % (e.g. 45%)
    private String reminderTime;
    private boolean reminderEnabled = false;
    private LocalDate createdLogicalDate; // Logical date when task was created

    // Joined helper fields
    private Category category;
    private Project project;
    private TaskSchedule schedule;

    public Task() {
        this.startDate = LocalDate.now();
        this.createdLogicalDate = LocalDate.now();
    }

    public Task(int id, String name, int categoryId, Integer projectId, String scheduledTime,
                int durationMinutes, String notes, LocalDate startDate, boolean active,
                int displayOrder, LocalDate multiDayEndDate, double taskProgress,
                String reminderTime, boolean reminderEnabled, LocalDate createdLogicalDate) {
        this.id = id;
        this.name = name;
        this.categoryId = categoryId;
        this.projectId = projectId;
        this.scheduledTime = scheduledTime;
        this.durationMinutes = durationMinutes;
        this.notes = notes;
        this.startDate = startDate != null ? startDate : LocalDate.now();
        this.active = active;
        this.displayOrder = displayOrder;
        this.multiDayEndDate = multiDayEndDate;
        this.taskProgress = taskProgress;
        this.reminderTime = reminderTime;
        this.reminderEnabled = reminderEnabled;
        this.createdLogicalDate = createdLogicalDate != null ? createdLogicalDate : LocalDate.now();
    }

    public boolean isMultiDay() {
        return multiDayEndDate != null && multiDayEndDate.isAfter(startDate);
    }

    /**
     * Calculates the multi-day status string, e.g., "Day 1/7".
     */
    public String getMultiDayDayProgressString(LocalDate currentDate) {
        if (!isMultiDay() || currentDate == null) {
            return null;
        }
        if (currentDate.isBefore(startDate)) {
            return "Upcoming";
        }
        long totalDays = ChronoUnit.DAYS.between(startDate, multiDayEndDate) + 1;
        long currentDayIndex = ChronoUnit.DAYS.between(startDate, currentDate) + 1;
        if (currentDayIndex > totalDays) {
            return "Completed period";
        }
        return "Day " + currentDayIndex + "/" + totalDays;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public Integer getProjectId() { return projectId; }
    public void setProjectId(Integer projectId) { this.projectId = projectId; }

    public String getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(String scheduledTime) { this.scheduledTime = scheduledTime; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public LocalDate getMultiDayEndDate() { return multiDayEndDate; }
    public void setMultiDayEndDate(LocalDate multiDayEndDate) { this.multiDayEndDate = multiDayEndDate; }

    public double getTaskProgress() { return taskProgress; }
    public void setTaskProgress(double taskProgress) {
        this.taskProgress = Math.max(0.0, Math.min(100.0, taskProgress));
    }

    public String getReminderTime() { return reminderTime; }
    public void setReminderTime(String reminderTime) { this.reminderTime = reminderTime; }

    public boolean isReminderEnabled() { return reminderEnabled; }
    public void setReminderEnabled(boolean reminderEnabled) { this.reminderEnabled = reminderEnabled; }

    public LocalDate getCreatedLogicalDate() { return createdLogicalDate; }
    public void setCreatedLogicalDate(LocalDate createdLogicalDate) { this.createdLogicalDate = createdLogicalDate; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public TaskSchedule getSchedule() { return schedule; }
    public void setSchedule(TaskSchedule schedule) { this.schedule = schedule; }

    @Override
    public String toString() {
        return name;
    }
}
