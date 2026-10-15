package com.everyday.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TaskDailyInstance {
    private int id;
    private LocalDate logicalDate;
    private int taskId;
    private String taskNameSnapshot;
    private String categoryNameSnapshot;
    private String projectNameSnapshot;
    private boolean isLateAddition; // If true: added after day started -> excluded from today's denominator & percentage!
    private boolean isCompleted;
    private LocalDateTime completedAt;
    private int displayOrder;
    private boolean isDeletedOccurrence;

    // Associated Task object in runtime
    private Task task;

    public TaskDailyInstance() {}

    public TaskDailyInstance(int id, LocalDate logicalDate, int taskId, String taskNameSnapshot,
                             String categoryNameSnapshot, String projectNameSnapshot,
                             boolean isLateAddition, boolean isCompleted, LocalDateTime completedAt,
                             int displayOrder, boolean isDeletedOccurrence) {
        this.id = id;
        this.logicalDate = logicalDate;
        this.taskId = taskId;
        this.taskNameSnapshot = taskNameSnapshot;
        this.categoryNameSnapshot = categoryNameSnapshot;
        this.projectNameSnapshot = projectNameSnapshot;
        this.isLateAddition = isLateAddition;
        this.isCompleted = isCompleted;
        this.completedAt = completedAt;
        this.displayOrder = displayOrder;
        this.isDeletedOccurrence = isDeletedOccurrence;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDate getLogicalDate() { return logicalDate; }
    public void setLogicalDate(LocalDate logicalDate) { this.logicalDate = logicalDate; }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    public String getTaskNameSnapshot() { return taskNameSnapshot; }
    public void setTaskNameSnapshot(String taskNameSnapshot) { this.taskNameSnapshot = taskNameSnapshot; }

    public String getCategoryNameSnapshot() { return categoryNameSnapshot; }
    public void setCategoryNameSnapshot(String categoryNameSnapshot) { this.categoryNameSnapshot = categoryNameSnapshot; }

    public String getProjectNameSnapshot() { return projectNameSnapshot; }
    public void setProjectNameSnapshot(String projectNameSnapshot) { this.projectNameSnapshot = projectNameSnapshot; }

    public boolean isLateAddition() { return isLateAddition; }
    public void setLateAddition(boolean lateAddition) { isLateAddition = lateAddition; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public boolean isDeletedOccurrence() { return isDeletedOccurrence; }
    public void setDeletedOccurrence(boolean deletedOccurrence) { isDeletedOccurrence = deletedOccurrence; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }
}
