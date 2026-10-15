package com.everyday.model;

public enum StreakLevel {
    BEGINNER("Beginner", 0, 99, 50.0),
    ELITE("Elite", 100, 199, 60.0),
    EPIC("Epic", 200, 299, 70.0),
    MASTER("Master", 300, 399, 80.0),
    LEGEND("Legend", 400, Integer.MAX_VALUE, 90.0);

    private final String displayName;
    private final int minDays;
    private final int maxDays;
    private final double minCompletionPercentage;

    StreakLevel(String displayName, int minDays, int maxDays, double minCompletionPercentage) {
        this.displayName = displayName;
        this.minDays = minDays;
        this.maxDays = maxDays;
        this.minCompletionPercentage = minCompletionPercentage;
    }

    public static StreakLevel forDays(int days) {
        if (days < 0) days = 0;
        for (StreakLevel level : values()) {
            if (days >= level.minDays && days <= level.maxDays) {
                return level;
            }
        }
        return LEGEND;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMinDays() {
        return minDays;
    }

    public int getMaxDays() {
        return maxDays;
    }

    public double getMinCompletionPercentage() {
        return minCompletionPercentage;
    }

    public boolean meetsThreshold(double completionPercentage) {
        // Exact threshold is always accepted: e.g. 50.0% meets 50%
        return completionPercentage >= this.minCompletionPercentage;
    }
}
