package com.everyday.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class MissedRecord {
    private int id;
    private LocalDate logicalDate;
    private int taskId;
    private String taskName;
    private String categoryName;
    private String projectName;
    private LocalDateTime createdAt;

    public MissedRecord() {}

    public MissedRecord(int id, LocalDate logicalDate, int taskId, String taskName,
                        String categoryName, String projectName, LocalDateTime createdAt) {
        this.id = id;
        this.logicalDate = logicalDate;
        this.taskId = taskId;
        this.taskName = taskName;
        this.categoryName = categoryName;
        this.projectName = projectName;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDate getLogicalDate() { return logicalDate; }
    public void setLogicalDate(LocalDate logicalDate) { this.logicalDate = logicalDate; }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
