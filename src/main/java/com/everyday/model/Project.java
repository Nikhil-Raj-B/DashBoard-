package com.everyday.model;

import java.time.LocalDate;

public class Project {
    public enum ProjectStatus {
        ACTIVE,
        PAUSED,
        COMPLETED,
        CANCELLED
    }

    private int id;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate targetDate;
    private ProjectStatus status = ProjectStatus.ACTIVE;
    private String notes;
    private double progress; // 0.0 to 100.0 - INDEPENDENT OF DAILY PRODUCTIVITY!

    public Project() {
        this.startDate = LocalDate.now();
    }

    public Project(int id, String name, String description, LocalDate startDate, LocalDate targetDate,
                   ProjectStatus status, String notes, double progress) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startDate = startDate != null ? startDate : LocalDate.now();
        this.targetDate = targetDate;
        this.status = status != null ? status : ProjectStatus.ACTIVE;
        this.notes = notes;
        this.progress = progress;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public double getProgress() { return progress; }
    public void setProgress(double progress) {
        this.progress = Math.max(0.0, Math.min(100.0, progress));
    }

    @Override
    public String toString() {
        return name;
    }
}
