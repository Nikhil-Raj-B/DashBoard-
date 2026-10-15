package com.everyday;

import com.everyday.database.DatabaseManager;
import com.everyday.model.AppSettings;
import com.everyday.model.Category;
import com.everyday.model.StreakRecord;
import com.everyday.model.UserProfile;
import com.everyday.service.ServiceRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ApplicationIntegrationTest {

    private File tempDb;

    @BeforeEach
    public void setup() throws SQLException {
        tempDb = new File(System.getProperty("java.io.tmpdir"), "everyday_integ_" + System.nanoTime() + ".db");
        DatabaseManager.initialize(tempDb.getAbsolutePath());
    }

    @AfterEach
    public void cleanup() {
        DatabaseManager.close();
        if (tempDb != null && tempDb.exists()) tempDb.delete();
    }

    @Test
    public void testApplicationInitializationAndSeedData() throws SQLException {
        ServiceRegistry registry = new ServiceRegistry();

        // Profile seed check
        UserProfile profile = registry.getProfileService().getProfile();
        assertNotNull(profile);
        assertEquals("Nikhil Raj", profile.getName());
        assertEquals("Personal", profile.getTagline());

        // Settings seed check
        AppSettings settings = registry.getSettingsService().getSettings();
        assertNotNull(settings);
        assertEquals("00:00", settings.getDayStartTime());
        assertTrue(settings.isDarkTheme());

        // Categories seed check
        List<Category> categories = registry.getCategoryRepository().getAll();
        assertFalse(categories.isEmpty());
        assertTrue(categories.stream().anyMatch(c -> "Coding".equalsIgnoreCase(c.getName())));
        assertTrue(categories.stream().anyMatch(c -> "DSA".equalsIgnoreCase(c.getName())));
        assertTrue(categories.stream().anyMatch(c -> "Gym".equalsIgnoreCase(c.getName())));
        assertTrue(categories.stream().anyMatch(c -> "Study".equalsIgnoreCase(c.getName())));

        // Streaks initial overall record
        StreakRecord streak = registry.getStreakService().getOverallStreak();
        assertNotNull(streak);
        assertEquals(0, streak.getCurrentStreak());

        // Logical date resolution
        LocalDate logicalDate = registry.getCurrentLogicalDate();
        assertNotNull(logicalDate);
    }
}
