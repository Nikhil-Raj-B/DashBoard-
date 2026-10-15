package com.everyday.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class SkippedDay {
    private int id;
    private LocalDate logicalDate;
    private String reason; // REQUIRED
    private LocalDateTime createdAt;

    public SkippedDay() {}

    public SkippedDay(int id, LocalDate logicalDate, String reason, LocalDateTime createdAt) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("A skip reason is required.");
        }
        this.id = id;
        this.logicalDate = logicalDate;
        this.reason = reason.trim();
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDate getLogicalDate() { return logicalDate; }
    public void setLogicalDate(LocalDate logicalDate) { this.logicalDate = logicalDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("A skip reason is required.");
        }
        this.reason = reason.trim();
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
