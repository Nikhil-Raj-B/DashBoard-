package com.everyday.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class StreakRecord {
    private int id;
    private String streakType; // "OVERALL" or category name
    private int currentStreak;
    private int longestStreak;
    private LocalDate lastEvaluatedDate;
    private LocalDateTime updatedAt;

    public StreakRecord() {}

    public StreakRecord(int id, String streakType, int currentStreak, int longestStreak,
                        LocalDate lastEvaluatedDate, LocalDateTime updatedAt) {
        this.id = id;
        this.streakType = streakType;
        this.currentStreak = currentStreak;
        this.longestStreak = longestStreak;
        this.lastEvaluatedDate = lastEvaluatedDate;
        this.updatedAt = updatedAt;
    }

    public StreakLevel getStreakLevel() {
        return StreakLevel.forDays(currentStreak);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getStreakType() { return streakType; }
    public void setStreakType(String streakType) { this.streakType = streakType; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
        if (currentStreak > longestStreak) {
            this.longestStreak = currentStreak;
        }
    }

    public int getLongestStreak() { return longestStreak; }
    public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }

    public LocalDate getLastEvaluatedDate() { return lastEvaluatedDate; }
    public void setLastEvaluatedDate(LocalDate lastEvaluatedDate) { this.lastEvaluatedDate = lastEvaluatedDate; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
