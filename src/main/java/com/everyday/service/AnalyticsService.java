package com.everyday.service;

import com.everyday.model.DailyRecord;
import com.everyday.model.MissedRecord;
import com.everyday.model.TaskDailyInstance;
import com.everyday.repository.DailyRecordRepository;
import com.everyday.repository.MissedRecordRepository;
import com.everyday.repository.TaskDailyInstanceRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsService {

    public enum TimeFilter {
        DAYS_7(7),
        DAYS_30(30),
        MONTHS_3(90),
        YEAR_1(365);

        private final int days;
        TimeFilter(int days) { this.days = days; }
        public int getDays() { return days; }
    }

    public static class AnalyticsSummary {
        public double weeklyAverage;
        public double monthlyAverage;
        public double overallAverage;
        public int totalTasksCompleted;
        public int totalTasksMissed;
        public double overallCompletionRate;
        public LocalDate bestDay;
        public double bestDayRate;
        public LocalDate lowestDay;
        public double lowestDayRate;
    }

    public static class TrendPoint {
        public LocalDate date;
        public double percentage;
        public TrendPoint(LocalDate date, double percentage) {
            this.date = date;
            this.percentage = percentage;
        }
    }

    private final DailyRecordRepository dailyRecordRepository;
    private final TaskDailyInstanceRepository instanceRepository;
    private final MissedRecordRepository missedRecordRepository;

    public AnalyticsService(DailyRecordRepository dailyRecordRepository,
                            TaskDailyInstanceRepository instanceRepository,
                            MissedRecordRepository missedRecordRepository) {
        this.dailyRecordRepository = dailyRecordRepository;
        this.instanceRepository = instanceRepository;
        this.missedRecordRepository = missedRecordRepository;
    }

    public AnalyticsSummary getSummary(LocalDate upToDate) throws SQLException {
        if (upToDate == null) upToDate = LocalDate.now();
        List<DailyRecord> all = dailyRecordRepository.getAll();
        AnalyticsSummary summary = new AnalyticsSummary();

        if (all.isEmpty()) {
            return summary;
        }

        double sumAll = 0;
        int countAll = 0;
        double sum7 = 0;
        int count7 = 0;
        double sum30 = 0;
        int count30 = 0;

        LocalDate weekCutoff = upToDate.minusDays(7);
        LocalDate monthCutoff = upToDate.minusDays(30);

        LocalDate bestDay = null;
        double maxRate = -1.0;
        LocalDate lowestDay = null;
        double minRate = 101.0;

        int totalCompleted = 0;

        for (DailyRecord r : all) {
            if (r.getLogicalDate().isAfter(upToDate)) continue;
            if (r.getTotalActiveTasks() <= 0) continue; // ignore empty days without tasks

            double pct = r.getCompletionPercentage();
            sumAll += pct;
            countAll++;
            totalCompleted += r.getCompletedActiveTasks();

            if (!r.getLogicalDate().isBefore(weekCutoff)) {
                sum7 += pct;
                count7++;
            }
            if (!r.getLogicalDate().isBefore(monthCutoff)) {
                sum30 += pct;
                count30++;
            }

            if (pct > maxRate) {
                maxRate = pct;
                bestDay = r.getLogicalDate();
            }
            if (pct < minRate) {
                minRate = pct;
                lowestDay = r.getLogicalDate();
            }
        }

        summary.overallAverage = countAll > 0 ? Math.round((sumAll / countAll) * 10.0) / 10.0 : 0.0;
        summary.weeklyAverage = count7 > 0 ? Math.round((sum7 / count7) * 10.0) / 10.0 : 0.0;
        summary.monthlyAverage = count30 > 0 ? Math.round((sum30 / count30) * 10.0) / 10.0 : 0.0;

        summary.bestDay = bestDay;
        summary.bestDayRate = maxRate >= 0 ? maxRate : 0.0;
        summary.lowestDay = lowestDay;
        summary.lowestDayRate = minRate <= 100 ? minRate : 0.0;
        summary.totalTasksCompleted = totalCompleted;

        List<MissedRecord> missed = missedRecordRepository.getRecent(10000);
        summary.totalTasksMissed = missed.size();

        int denominator = summary.totalTasksCompleted + summary.totalTasksMissed;
        summary.overallCompletionRate = denominator > 0
                ? Math.round(((double) summary.totalTasksCompleted / denominator * 100.0) * 10.0) / 10.0
                : 0.0;

        return summary;
    }

    public List<TrendPoint> getTrend(TimeFilter filter, LocalDate upToDate) throws SQLException {
        if (upToDate == null) upToDate = LocalDate.now();
        LocalDate startDate = upToDate.minusDays(filter.getDays());
        List<DailyRecord> records = dailyRecordRepository.getBetween(startDate, upToDate);

        Map<LocalDate, Double> map = new HashMap<>();
        for (DailyRecord r : records) {
            map.put(r.getLogicalDate(), r.getCompletionPercentage());
        }

        List<TrendPoint> points = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(upToDate); d = d.plusDays(1)) {
            points.add(new TrendPoint(d, map.getOrDefault(d, 0.0)));
        }
        return points;
    }

    public List<TrendPoint> getActivityTrend(String activityName, TimeFilter filter, LocalDate upToDate) throws SQLException {
        if (upToDate == null) upToDate = LocalDate.now();
        LocalDate startDate = upToDate.minusDays(filter.getDays());

        List<TrendPoint> points = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(upToDate); d = d.plusDays(1)) {
            List<TaskDailyInstance> insts = instanceRepository.getInstancesForDate(d);
            long catTotal = insts.stream().filter(i -> activityName.equalsIgnoreCase(i.getCategoryNameSnapshot()) && !i.isDeletedOccurrence()).count();
            if (catTotal == 0) {
                points.add(new TrendPoint(d, 0.0));
            } else {
                long catCompleted = insts.stream().filter(i -> activityName.equalsIgnoreCase(i.getCategoryNameSnapshot()) && !i.isDeletedOccurrence() && i.isCompleted()).count();
                double pct = Math.round(((double) catCompleted / catTotal * 100.0) * 10.0) / 10.0;
                points.add(new TrendPoint(d, pct));
            }
        }
        return points;
    }
}
