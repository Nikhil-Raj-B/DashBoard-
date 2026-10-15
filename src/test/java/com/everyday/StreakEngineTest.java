package com.everyday;

import com.everyday.database.DatabaseManager;
import com.everyday.model.DailyRecord;
import com.everyday.model.DailyRecord.DayStatus;
import com.everyday.model.DailyRecord.StreakState;
import com.everyday.model.SkippedDay;
import com.everyday.model.StreakLevel;
import com.everyday.model.StreakRecord;
import com.everyday.repository.DailyRecordRepository;
import com.everyday.repository.SkippedDayRepository;
import com.everyday.repository.StreakRepository;
import com.everyday.repository.TaskDailyInstanceRepository;
import com.everyday.service.StreakService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class StreakEngineTest {

    private File tempDb;
    private StreakRepository streakRepository;
    private DailyRecordRepository dailyRecordRepository;
    private SkippedDayRepository skippedDayRepository;
    private TaskDailyInstanceRepository instanceRepository;
    private StreakService streakService;

    @BeforeEach
    public void setup() throws SQLException {
        tempDb = new File(System.getProperty("java.io.tmpdir"), "everyday_streak_test_" + System.nanoTime() + ".db");
        DatabaseManager.initialize(tempDb.getAbsolutePath());

        streakRepository = new StreakRepository();
        dailyRecordRepository = new DailyRecordRepository();
        skippedDayRepository = new SkippedDayRepository();
        instanceRepository = new TaskDailyInstanceRepository();

        streakService = new StreakService(
                streakRepository, dailyRecordRepository, skippedDayRepository, instanceRepository
        );
    }

    @AfterEach
    public void cleanup() {
        DatabaseManager.close();
        if (tempDb != null && tempDb.exists()) {
            tempDb.delete();
        }
    }

    @Test
    public void testStreakThresholdBoundaries() {
        // Beginner: 0–99 days, minimum completion = 50%
        StreakLevel beginner = StreakLevel.BEGINNER;
        assertFalse(beginner.meetsThreshold(49.0), "49% must break Beginner streak");
        assertFalse(beginner.meetsThreshold(49.9));
        assertTrue(beginner.meetsThreshold(50.0), "Exact threshold 50% must be accepted");
        assertTrue(beginner.meetsThreshold(51.0));

        // Elite: 100–199 days, minimum completion = 60%
        StreakLevel elite = StreakLevel.ELITE;
        assertFalse(elite.meetsThreshold(59.0), "59% must break Elite streak");
        assertFalse(elite.meetsThreshold(59.9));
        assertTrue(elite.meetsThreshold(60.0), "Exact threshold 60% must be accepted");
        assertTrue(elite.meetsThreshold(65.0));

        // Epic: 200–299 days, minimum completion = 70%
        StreakLevel epic = StreakLevel.EPIC;
        assertFalse(epic.meetsThreshold(69.0), "69% must break Epic streak");
        assertFalse(epic.meetsThreshold(69.9));
        assertTrue(epic.meetsThreshold(70.0), "Exact threshold 70% must be accepted");
        assertTrue(epic.meetsThreshold(75.0));

        // Master: 300–399 days, minimum completion = 80%
        StreakLevel master = StreakLevel.MASTER;
        assertFalse(master.meetsThreshold(79.0), "79% must break Master streak");
        assertFalse(master.meetsThreshold(79.9));
        assertTrue(master.meetsThreshold(80.0), "Exact threshold 80% must be accepted");
        assertTrue(master.meetsThreshold(85.0));

        // Legend: 400+ days, minimum completion = 90%
        StreakLevel legend = StreakLevel.LEGEND;
        assertFalse(legend.meetsThreshold(89.0), "89% must break Legend streak");
        assertFalse(legend.meetsThreshold(89.9));
        assertTrue(legend.meetsThreshold(90.0), "Exact threshold 90% must be accepted");
        assertTrue(legend.meetsThreshold(95.0));
    }

    @Test
    public void testStreakDayStatusSeparation() {
        // 7/8 = 87.5%
        // Day Status: Incomplete
        // Streak: Continues (meets threshold)
        LocalDate date = LocalDate.of(2026, 10, 8);
        DailyRecord rec = new DailyRecord(date, 8, 7);

        assertEquals(DayStatus.INCOMPLETE, rec.getDayStatus(), "Day status must be Incomplete");
        assertEquals(87.5, rec.getCompletionPercentage());

        // Check against beginner threshold
        assertTrue(StreakLevel.BEGINNER.meetsThreshold(rec.getCompletionPercentage()), "Streak must continue!");
    }

    @Test
    public void testSkipDayRequiresReason() {
        LocalDate date = LocalDate.of(2026, 10, 8);

        // Null reason
        assertThrows(IllegalArgumentException.class, () -> {
            streakService.skipDay(date, null);
        });

        // Empty reason
        assertThrows(IllegalArgumentException.class, () -> {
            streakService.skipDay(date, "   ");
        });
    }

    @Test
    public void testSkipDayDoesNotBreakAndDoesNotIncreaseStreak() throws SQLException {
        LocalDate day1 = LocalDate.of(2026, 10, 1);
        LocalDate day2 = LocalDate.of(2026, 10, 2);
        LocalDate day3 = LocalDate.of(2026, 10, 3);

        // Day 1: 100% completion
        DailyRecord r1 = new DailyRecord(day1, 4, 4);
        dailyRecordRepository.insertOrUpdate(r1);

        streakService.recalculateAllStreaks(day1);
        StreakRecord s1 = streakService.getOverallStreak();
        assertEquals(1, s1.getCurrentStreak());

        // Day 2: User skips day with reason "Travelling"
        streakService.skipDay(day2, "Travelling");

        // Day 2 DailyRecord has 0 tasks done, but is skipped
        DailyRecord r2 = new DailyRecord(day2, 4, 0);
        dailyRecordRepository.insertOrUpdate(r2);

        streakService.recalculateAllStreaks(day2);
        StreakRecord s2 = streakService.getOverallStreak();

        // Streak MUST NOT increase, and MUST NOT break!
        assertEquals(1, s2.getCurrentStreak(), "Skipped day must preserve streak without increasing or breaking!");

        // Day 3: User completes 100%
        DailyRecord r3 = new DailyRecord(day3, 4, 4);
        dailyRecordRepository.insertOrUpdate(r3);

        streakService.recalculateAllStreaks(day3);
        StreakRecord s3 = streakService.getOverallStreak();

        // Now streak increases to 2!
        assertEquals(2, s3.getCurrentStreak());
    }

    @Test
    public void testCurrentAndLongestStreakAreSeparate() throws SQLException {
        LocalDate day1 = LocalDate.of(2026, 10, 1);
        LocalDate day2 = LocalDate.of(2026, 10, 2);
        LocalDate day3 = LocalDate.of(2026, 10, 3);
        LocalDate day4 = LocalDate.of(2026, 10, 4);

        // Days 1 & 2: Success (streak = 2)
        dailyRecordRepository.insertOrUpdate(new DailyRecord(day1, 4, 4));
        dailyRecordRepository.insertOrUpdate(new DailyRecord(day2, 4, 4));

        streakService.recalculateAllStreaks(day2);
        StreakRecord s = streakService.getOverallStreak();
        assertEquals(2, s.getCurrentStreak());
        assertEquals(2, s.getLongestStreak());

        // Day 3: Fails (< 50%) -> Streak breaks to 0
        dailyRecordRepository.insertOrUpdate(new DailyRecord(day3, 4, 1)); // 25%

        streakService.recalculateAllStreaks(day3);
        StreakRecord sBroken = streakService.getOverallStreak();
        assertEquals(0, sBroken.getCurrentStreak(), "Current streak must reset to 0 on break");
        assertEquals(2, sBroken.getLongestStreak(), "Longest streak must remain 2");

        // Day 4: Success -> Streak becomes 1, longest remains 2
        dailyRecordRepository.insertOrUpdate(new DailyRecord(day4, 4, 4));

        streakService.recalculateAllStreaks(day4);
        StreakRecord sRecovered = streakService.getOverallStreak();
        assertEquals(1, sRecovered.getCurrentStreak());
        assertEquals(2, sRecovered.getLongestStreak(), "Longest streak must remain preserved as 2");
    }
}
