package com.everyday.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DailyRecord {
    public enum DayStatus {
        EMPTY,
        INCOMPLETE,
        COMPLETED
    }

    public enum StreakState {
        NO_STREAK,
        CONTINUED,
        INCREASED,
        BROKEN,
        SKIPPED
    }

    private int id;
    private LocalDate logicalDate;
    private int totalActiveTasks;
    private int completedActiveTasks;
    private double completionPercentage;
    private DayStatus dayStatus = DayStatus.EMPTY;
    private StreakState streakState = StreakState.NO_STREAK;
    private LocalDateTime evaluatedAt;

    public DailyRecord() {}

    public DailyRecord(LocalDate logicalDate, int totalActiveTasks, int completedActiveTasks) {
        this.logicalDate = logicalDate;
        this.totalActiveTasks = totalActiveTasks;
        this.completedActiveTasks = completedActiveTasks;
        recalculate();
    }

    public void recalculate() {
        if (totalActiveTasks <= 0) {
            this.completionPercentage = 0.0;
            this.dayStatus = DayStatus.EMPTY;
        } else {
            this.completionPercentage = Math.round(((double) completedActiveTasks / totalActiveTasks * 100.0) * 10.0) / 10.0;
            if (completedActiveTasks >= totalActiveTasks) {
                this.dayStatus = DayStatus.COMPLETED;
            } else {
                this.dayStatus = DayStatus.INCOMPLETE;
            }
        }
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDate getLogicalDate() { return logicalDate; }
    public void setLogicalDate(LocalDate logicalDate) { this.logicalDate = logicalDate; }

    public int getTotalActiveTasks() { return totalActiveTasks; }
    public void setTotalActiveTasks(int totalActiveTasks) {
        this.totalActiveTasks = totalActiveTasks;
        recalculate();
    }

    public int getCompletedActiveTasks() { return completedActiveTasks; }
    public void setCompletedActiveTasks(int completedActiveTasks) {
        this.completedActiveTasks = completedActiveTasks;
        recalculate();
    }

    public double getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(double completionPercentage) { this.completionPercentage = completionPercentage; }

    public DayStatus getDayStatus() { return dayStatus; }
    public void setDayStatus(DayStatus dayStatus) { this.dayStatus = dayStatus; }

    public StreakState getStreakState() { return streakState; }
    public void setStreakState(StreakState streakState) { this.streakState = streakState; }

    public LocalDateTime getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(LocalDateTime evaluatedAt) { this.evaluatedAt = evaluatedAt; }
}
