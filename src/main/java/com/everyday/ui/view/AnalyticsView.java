package com.everyday.ui.view;

import com.everyday.model.Category;
import com.everyday.service.AnalyticsService;
import com.everyday.service.AnalyticsService.AnalyticsSummary;
import com.everyday.service.AnalyticsService.TimeFilter;
import com.everyday.service.AnalyticsService.TrendPoint;
import com.everyday.service.ServiceRegistry;
import com.everyday.ui.component.Icons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AnalyticsView extends ScrollPane {

    private final ServiceRegistry registry;

    // Stat Cards
    private Label weeklyAvgVal;
    private Label monthlyAvgVal;
    private Label overallAvgVal;
    private Label completedCountVal;
    private Label missedCountVal;
    private Label compRateVal;
    private Label bestDayVal;
    private Label lowestDayVal;

    // Charts
    private LineChart<String, Number> overallTrendChart;
    private LineChart<String, Number> activityChart;
    private ComboBox<String> activityCombo;
    private TimeFilter selectedFilter = TimeFilter.DAYS_30;

    public AnalyticsView(ServiceRegistry registry) {
        this.registry = registry;

        this.setFitToWidth(true);
        this.setStyle("-fx-background-color: transparent;");

        VBox content = new VBox(24);
        content.setPadding(new Insets(28, 36, 40, 36));

        // 1. Header & Filter Bar
        content.getChildren().add(buildHeaderAndFilterBar());

        // 2. Summary Stat Cards (Weekly, Monthly, Overall, Best Day, etc.)
        content.getChildren().add(buildSummaryGrid());

        // 3. Overall Trend Chart
        content.getChildren().add(buildOverallTrendCard());

        // 4. Activity Analytics (Reusable single graph with dropdown)
        content.getChildren().add(buildActivityAnalyticsCard());

        this.setContent(content);
        refresh();
    }

    private Node buildHeaderAndFilterBar() {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Analytics & Growth");
        title.getStyleClass().add("section-title");

        Label subtitle = new Label("Productivity telemetry, time horizons, and consistency trends");
        subtitle.getStyleClass().add("section-subtitle");
        titleBox.getChildren().addAll(title, subtitle);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        // Filter Pills: 7 days, 30 days, 3 months, 1 year
        HBox filterBox = new HBox(8);
        filterBox.setAlignment(Pos.CENTER_RIGHT);
        ToggleGroup tg = new ToggleGroup();

        RadioButton r7 = createFilterRadio("7 Days", TimeFilter.DAYS_7, tg, false);
        RadioButton r30 = createFilterRadio("30 Days", TimeFilter.DAYS_30, tg, true);
        RadioButton r90 = createFilterRadio("3 Months", TimeFilter.MONTHS_3, tg, false);
        RadioButton r365 = createFilterRadio("1 Year", TimeFilter.YEAR_1, tg, false);

        filterBox.getChildren().addAll(r7, r30, r90, r365);
        bar.getChildren().addAll(titleBox, filterBox);
        return bar;
    }

    private RadioButton createFilterRadio(String label, TimeFilter filter, ToggleGroup tg, boolean selected) {
        RadioButton rb = new RadioButton(label);
        rb.setToggleGroup(tg);
        rb.setSelected(selected);
        rb.getStyleClass().add("btn-secondary");
        rb.setOnAction(e -> {
            this.selectedFilter = filter;
            refreshCharts();
        });
        return rb;
    }

    private Node buildSummaryGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);

        weeklyAvgVal = new Label("0%");
        monthlyAvgVal = new Label("0%");
        overallAvgVal = new Label("0%");
        compRateVal = new Label("0%");
        completedCountVal = new Label("0");
        missedCountVal = new Label("0");
        bestDayVal = new Label("-");
        lowestDayVal = new Label("-");

        grid.add(createMiniCard("WEEKLY AVERAGE", weeklyAvgVal, "#3B82F6"), 0, 0);
        grid.add(createMiniCard("MONTHLY AVERAGE", monthlyAvgVal, "#60A5FA"), 1, 0);
        grid.add(createMiniCard("OVERALL AVERAGE", overallAvgVal, "#00D2FF"), 2, 0);
        grid.add(createMiniCard("COMPLETION RATE", compRateVal, "#10B981"), 3, 0);

        grid.add(createMiniCard("TASKS COMPLETED", completedCountVal, "#34D399"), 0, 1);
        grid.add(createMiniCard("TASKS MISSED", missedCountVal, "#EF4444"), 1, 1);
        grid.add(createMiniCard("BEST DAY", bestDayVal, "#F59E0B"), 2, 1);
        grid.add(createMiniCard("LOWEST DAY", lowestDayVal, "#94A3B8"), 3, 1);

        for (int i = 0; i < 4; i++) {
            GridPane.setHgrow(grid.getChildren().get(i), Priority.ALWAYS);
        }

        return grid;
    }

    private Node createMiniCard(String label, Label valLabel, String hexColor) {
        VBox card = new VBox(4);
        card.getStyleClass().add("glass-card");
        card.setPadding(new Insets(14, 18, 14, 18));

        Label t = new Label(label);
        t.getStyleClass().add("stat-label");

        valLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: " + hexColor + ";");
        card.getChildren().addAll(t, valLabel);
        return card;
    }

    private Node buildOverallTrendCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("glass-card");
        card.setPadding(new Insets(20));

        Label title = new Label("Daily Completion Horizon Trend");
        title.getStyleClass().add("card-header-title");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis(0, 100, 20);
        yAxis.setLabel("% Completed");

        overallTrendChart = new LineChart<>(xAxis, yAxis);
        overallTrendChart.setLegendVisible(false);
        overallTrendChart.setPrefHeight(260);
        overallTrendChart.setAnimated(false);
        overallTrendChart.setStyle("-fx-background-color: transparent;");

        card.getChildren().addAll(title, overallTrendChart);
        return card;
    }

    private Node buildActivityAnalyticsCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("glass-card");
        card.setPadding(new Insets(20));

        HBox topRow = new HBox(16);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Activity Consistency Analytics");
        title.getStyleClass().add("card-header-title");
        Label subtitle = new Label("Track focus and dedication across individual domains");
        subtitle.getStyleClass().add("card-subtitle");
        titleBox.getChildren().addAll(title, subtitle);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        // Activity dropdown
        activityCombo = new ComboBox<>();
        activityCombo.setPrefWidth(160);
        activityCombo.setOnAction(e -> refreshActivityChart());

        topRow.getChildren().addAll(titleBox, activityCombo);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis(0, 100, 20);
        yAxis.setLabel("% Activity Done");

        activityChart = new LineChart<>(xAxis, yAxis);
        activityChart.setLegendVisible(false);
        activityChart.setPrefHeight(260);
        activityChart.setAnimated(false);
        activityChart.setStyle("-fx-background-color: transparent;");

        card.getChildren().addAll(topRow, activityChart);
        return card;
    }

    public void refresh() {
        try {
            LocalDate logicalDate = registry.getCurrentLogicalDate();

            // Populate summary metrics
            AnalyticsSummary summary = registry.getAnalyticsService().getSummary(logicalDate);
            weeklyAvgVal.setText(String.format("%.1f%%", summary.weeklyAverage));
            monthlyAvgVal.setText(String.format("%.1f%%", summary.monthlyAverage));
            overallAvgVal.setText(String.format("%.1f%%", summary.overallAverage));
            compRateVal.setText(String.format("%.1f%%", summary.overallCompletionRate));
            completedCountVal.setText(String.valueOf(summary.totalTasksCompleted));
            missedCountVal.setText(String.valueOf(summary.totalTasksMissed));

            bestDayVal.setText(summary.bestDay != null ? summary.bestDay.format(DateTimeFormatter.ofPattern("MMM d")) + " (" + (int) summary.bestDayRate + "%)" : "-");
            lowestDayVal.setText(summary.lowestDay != null ? summary.lowestDay.format(DateTimeFormatter.ofPattern("MMM d")) + " (" + (int) summary.lowestDayRate + "%)" : "-");

            // Populate category dropdown
            String currentSelected = activityCombo.getValue();
            activityCombo.getItems().clear();
            List<Category> cats = registry.getCategoryRepository().getAll();
            for (Category c : cats) {
                activityCombo.getItems().add(c.getName());
            }
            if (!activityCombo.getItems().isEmpty()) {
                if (currentSelected != null && activityCombo.getItems().contains(currentSelected)) {
                    activityCombo.setValue(currentSelected);
                } else {
                    activityCombo.setValue(activityCombo.getItems().get(0));
                }
            }

            refreshCharts();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshCharts() {
        try {
            LocalDate logicalDate = registry.getCurrentLogicalDate();

            // Overall Trend
            List<TrendPoint> trend = registry.getAnalyticsService().getTrend(selectedFilter, logicalDate);
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            DateTimeFormatter df = selectedFilter.getDays() <= 30 ? DateTimeFormatter.ofPattern("MMM d") : DateTimeFormatter.ofPattern("M/d");

            // Subsample if 365 days to prevent dense axis
            int step = selectedFilter == TimeFilter.YEAR_1 ? 7 : (selectedFilter == TimeFilter.MONTHS_3 ? 3 : 1);
            for (int i = 0; i < trend.size(); i += step) {
                TrendPoint pt = trend.get(i);
                series.getData().add(new XYChart.Data<>(pt.date.format(df), pt.percentage));
            }
            overallTrendChart.getData().clear();
            overallTrendChart.getData().add(series);

            refreshActivityChart();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshActivityChart() {
        String selActivity = activityCombo.getValue();
        if (selActivity == null) return;

        try {
            LocalDate logicalDate = registry.getCurrentLogicalDate();
            List<TrendPoint> trend = registry.getAnalyticsService().getActivityTrend(selActivity, selectedFilter, logicalDate);

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            DateTimeFormatter df = selectedFilter.getDays() <= 30 ? DateTimeFormatter.ofPattern("MMM d") : DateTimeFormatter.ofPattern("M/d");

            int step = selectedFilter == TimeFilter.YEAR_1 ? 7 : (selectedFilter == TimeFilter.MONTHS_3 ? 3 : 1);
            for (int i = 0; i < trend.size(); i += step) {
                TrendPoint pt = trend.get(i);
                series.getData().add(new XYChart.Data<>(pt.date.format(df), pt.percentage));
            }
            activityChart.getData().clear();
            activityChart.getData().add(series);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
