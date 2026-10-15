package com.everyday.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class TaskSchedule {
    public enum ScheduleType {
        EVERY_DAY,
        SPECIFIC_WEEKDAYS,
        CUSTOM_INTERVAL, // every N days
        CUSTOM_DATES,    // specific comma-separated dates
        MULTI_DAY        // continuous range
    }

    private int id;
    private int taskId;
    private ScheduleType scheduleType = ScheduleType.EVERY_DAY;
    private String weekdays; // e.g., "MONDAY,WEDNESDAY,FRIDAY"
    private int intervalDays = 1;
    private String customDates; // e.g., "2026-10-10,2026-10-15"
    private LocalDate startDate;
    private LocalDate endDate;

    public TaskSchedule() {}

    public TaskSchedule(ScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }

    public boolean isScheduledFor(LocalDate date, LocalDate taskStartDate) {
        if (date == null) return false;
        LocalDate effectiveStart = startDate != null ? startDate : (taskStartDate != null ? taskStartDate : date);
        if (date.isBefore(effectiveStart)) {
            return false;
        }

        switch (scheduleType) {
            case EVERY_DAY:
                if (endDate != null && date.isAfter(endDate)) {
                    return false;
                }
                return true;

            case SPECIFIC_WEEKDAYS:
                if (endDate != null && date.isAfter(endDate)) {
                    return false;
                }
                if (weekdays == null || weekdays.isBlank()) return true;
                Set<String> activeDays = Arrays.stream(weekdays.split(","))
                        .map(String::trim)
                        .map(String::toUpperCase)
                        .collect(Collectors.toSet());
                return activeDays.contains(date.getDayOfWeek().name());

            case CUSTOM_INTERVAL:
                if (endDate != null && date.isAfter(endDate)) {
                    return false;
                }
                if (intervalDays <= 1) return true;
                long daysBetween = ChronoUnit.DAYS.between(effectiveStart, date);
                return (daysBetween % intervalDays) == 0;

            case CUSTOM_DATES:
                if (customDates == null || customDates.isBlank()) return false;
                Set<String> dates = Arrays.stream(customDates.split(","))
                        .map(String::trim)
                        .collect(Collectors.toSet());
                return dates.contains(date.toString());

            case MULTI_DAY:
                if (endDate == null) return true;
                return !date.isBefore(effectiveStart) && !date.isAfter(endDate);

            default:
                return true;
        }
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    public ScheduleType getScheduleType() { return scheduleType; }
    public void setScheduleType(ScheduleType scheduleType) { this.scheduleType = scheduleType; }

    public String getWeekdays() { return weekdays; }
    public void setWeekdays(String weekdays) { this.weekdays = weekdays; }

    public int getIntervalDays() { return intervalDays; }
    public void setIntervalDays(int intervalDays) { this.intervalDays = intervalDays; }

    public String getCustomDates() { return customDates; }
    public void setCustomDates(String customDates) { this.customDates = customDates; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}
