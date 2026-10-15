package com.everyday.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy");

    /**
     * Computes the current logical date based on the current wall-clock time
     * and the configured day start time.
     *
     * Example: If day start is 04:00 AM and current time is 02:30 AM on Oct 9,
     * the logical date is still Oct 8.
     */
    public static LocalDate getLogicalDate(LocalDateTime dateTime, LocalTime dayStartTime) {
        if (dateTime == null) {
            dateTime = LocalDateTime.now();
        }
        if (dayStartTime == null || dayStartTime.equals(LocalTime.MIDNIGHT) || dayStartTime.equals(LocalTime.MIN)) {
            return dateTime.toLocalDate();
        }

        if (dateTime.toLocalTime().isBefore(dayStartTime)) {
            return dateTime.toLocalDate().minusDays(1);
        } else {
            return dateTime.toLocalDate();
        }
    }

    public static LocalDate getLogicalDate(LocalTime dayStartTime) {
        return getLogicalDate(LocalDateTime.now(), dayStartTime);
    }
}
