package com.everyday.ui.view;

import com.everyday.model.DailyRecord;
import com.everyday.model.StreakRecord;
import com.everyday.model.TaskDailyInstance;
import com.everyday.model.UserProfile;
import com.everyday.service.AnalyticsService;
import com.everyday.service.CalendarService;
import com.everyday.service.ServiceRegistry;
import com.everyday.ui.component.GaugeWidget;
import com.everyday.ui.component.Icons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public class DashboardView extends ScrollPane {

    private final ServiceRegistry registry;
    private final Consumer<String> navigationHandler;

    private Label greetingLabel;
    private Label dateLabel;
    private Label completionStatLabel;
    private Label tasksRatioStatLabel;
    private Label streakStatLabel;
    private Label weeklyAvgStatLabel;
    private GaugeWidget gaugeWidget;
    private LineChart<String, Number> trendChart;
    private VBox tasksPreviewList;
    private Label streakLevelBadge;
    private Label streakThresholdDesc;
    private Label longestStreakLabel;

    public DashboardView(ServiceRegistry registry, Consumer<String> navigationHandler) {
        this.registry = registry;
        this.navigationHandler = navigationHandler;

        this.setFitToWidth(true);
        this.setStyle("-fx-background-color: transparent;");

        VBox content = new VBox(24);
        content.setPadding(new Insets(28, 36, 40, 36));

        // 1. Header (Greeting + Date)
        content.getChildren().add(buildHeader());

        // 2. Metric Stat Cards Row
        content.getChildren().add(buildStatCardsRow());

        // 3. Middle Grid: Hero Card + Gauge Card + Streak Card
        content.getChildren().add(buildMiddleRow());

        // 4. Bottom Grid: 7-Day Completion Trend + Today's Tasks Preview
        content.getChildren().add(buildBottomRow());

        this.setContent(content);
        refresh();
    }

    private Node buildHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        greetingLabel = new Label("Welcome back, Nikhil");
        greetingLabel.getStyleClass().add("section-title");

        dateLabel = new Label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        dateLabel.getStyleClass().add("section-subtitle");
        titleBox.getChildren().addAll(greetingLabel, dateLabel);

        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button quickTaskBtn = new Button("View Daily Tasks");
        quickTaskBtn.getStyleClass().add("btn-primary");
        quickTaskBtn.setGraphic(Icons.getIcon("tasks", 16, Color.WHITE));
        quickTaskBtn.setOnAction(e -> navigationHandler.accept("Daily Tasks"));

        header.getChildren().addAll(titleBox, quickTaskBtn);
        return header;
    }

    private Node buildStatCardsRow() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);

        completionStatLabel = new Label("0%");
        tasksRatioStatLabel = new Label("0 / 0");
        streakStatLabel = new Label("0 Days");
        weeklyAvgStatLabel = new Label("0%");

        grid.add(createStatCard("TODAY'S COMPLETION", completionStatLabel, "check-square", "#3B82F6"), 0, 0);
        grid.add(createStatCard("TASKS COMPLETED", tasksRatioStatLabel, "check", "#10B981"), 1, 0);
        grid.add(createStatCard("CURRENT STREAK", streakStatLabel, "flame", "#F59E0B"), 2, 0);
        grid.add(createStatCard("WEEKLY AVERAGE", weeklyAvgStatLabel, "chart", "#00D2FF"), 3, 0);

        for (int i = 0; i < 4; i++) {
            GridPane.setHgrow(grid.getChildren().get(i), Priority.ALWAYS);
        }

        return grid;
    }

    private Node createStatCard(String label, Label valueLabel, String iconName, String hexColor) {
        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("glass-card");
        card.setPadding(new Insets(16, 20, 16, 20));

        VBox text = new VBox(4);
        Label title = new Label(label);
        title.getStyleClass().add("stat-label");

        valueLabel.getStyleClass().add("stat-value");
        text.getChildren().addAll(title, valueLabel);
        HBox.setHgrow(text, Priority.ALWAYS);

        VBox iconWrapper = new VBox();
        iconWrapper.setAlignment(Pos.CENTER);
        iconWrapper.setStyle(String.format("-fx-background-color: %s22; -fx-padding: 10px; -fx-background-radius: 10px;", hexColor));
        iconWrapper.getChildren().add(Icons.getIcon(iconName, 20, hexColor));

        card.getChildren().addAll(text, iconWrapper);
        return card;
    }

    private Node buildMiddleRow() {
        GridPane grid = new GridPane();
        grid.setHgap(16);

        // Hero Card (Vision UI inspired)
        VBox heroCard = new VBox(12);
        heroCard.getStyleClass().add("glass-card");
        heroCard.setStyle("-fx-background-color: linear-gradient(to bottom right, #111e38, #0e172a); -fx-border-color: #233554;");
        heroCard.setPadding(new Insets(24));

        Label philosophyTag = new Label("CORE PHILOSOPHY");
        philosophyTag.setStyle("-fx-text-fill: #60A5FA; -fx-font-weight: 700; -fx-font-size: 11px; -fx-letter-spacing: 1px;");

        Label heroTitle = new Label("Plan → Do → Check → Analyze → Improve");
        heroTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: 800; -fx-text-fill: #F8FAFC;");

        Label heroDesc = new Label("Consistent daily progress compounds into monumental personal growth. Every active task counts equally.");
        heroDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: #94A3B8; -fx-wrap-text: true;");

        Button openDailyBtn = new Button("Go to Daily Execution →");
        openDailyBtn.getStyleClass().add("btn-secondary");
        openDailyBtn.setOnAction(e -> navigationHandler.accept("Daily Tasks"));

        heroCard.getChildren().addAll(philosophyTag, heroTitle, heroDesc, openDailyBtn);
        GridPane.setHgrow(heroCard, Priority.ALWAYS);

        // Gauge Widget
        gaugeWidget = new GaugeWidget("Daily Completion", "Active tasks progress");
        gaugeWidget.setPrefWidth(240);

        // Streak Status Card
        VBox streakCard = new VBox(10);
        streakCard.getStyleClass().add("glass-card");
        streakCard.setPrefWidth(260);

        Label streakCardTitle = new Label("Streak Status");
        streakCardTitle.getStyleClass().add("card-header-title");

        streakLevelBadge = new Label("Beginner Level");
        streakLevelBadge.getStyleClass().addAll("stat-badge");

        streakThresholdDesc = new Label("Requires ≥ 50% daily completion");
        streakThresholdDesc.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");

        longestStreakLabel = new Label("Longest streak: 0 days");
        longestStreakLabel.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 13px; -fx-font-weight: 600;");

        Button streaksDetailBtn = new Button("View Streaks");
        streaksDetailBtn.getStyleClass().add("btn-secondary");
        streaksDetailBtn.setOnAction(e -> navigationHandler.accept("Streaks"));

        streakCard.getChildren().addAll(streakCardTitle, streakLevelBadge, streakThresholdDesc, longestStreakLabel, streaksDetailBtn);

        grid.add(heroCard, 0, 0);
        grid.add(gaugeWidget, 1, 0);
        grid.add(streakCard, 2, 0);

        return grid;
    }

    private Node buildBottomRow() {
        GridPane grid = new GridPane();
        grid.setHgap(16);

        // Trend Chart Card
        VBox chartCard = new VBox(12);
        chartCard.getStyleClass().add("glass-card");
        chartCard.setPadding(new Insets(20));

        Label chartTitle = new Label("7-Day Completion Trend");
        chartTitle.getStyleClass().add("card-header-title");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis(0, 100, 25);
        yAxis.setLabel("%");
        trendChart = new LineChart<>(xAxis, yAxis);
        trendChart.setLegendVisible(false);
        trendChart.setPrefHeight(230);
        trendChart.setAnimated(false);
        trendChart.setStyle("-fx-background-color: transparent;");

        chartCard.getChildren().addAll(chartTitle, trendChart);
        GridPane.setHgrow(chartCard, Priority.ALWAYS);

        // Today's Tasks Preview Card
        VBox tasksPreviewCard = new VBox(12);
        tasksPreviewCard.getStyleClass().add("glass-card");
        tasksPreviewCard.setPrefWidth(380);
        tasksPreviewCard.setPadding(new Insets(20));

        HBox tasksCardHeader = new HBox();
        Label tTitle = new Label("Today's Tasks Preview");
        tTitle.getStyleClass().add("card-header-title");
        HBox.setHgrow(tTitle, Priority.ALWAYS);

        Button manageBtn = new Button("Manage");
        manageBtn.getStyleClass().add("btn-secondary");
        manageBtn.setStyle("-fx-padding: 4 10; -fx-font-size: 12px;");
        manageBtn.setOnAction(e -> navigationHandler.accept("Daily Tasks"));
        tasksCardHeader.getChildren().addAll(tTitle, manageBtn);

        tasksPreviewList = new VBox(8);
        ScrollPane previewScroll = new ScrollPane(tasksPreviewList);
        previewScroll.setFitToWidth(true);
        previewScroll.setStyle("-fx-background-color: transparent;");
        previewScroll.setPrefHeight(200);

        tasksPreviewCard.getChildren().addAll(tasksCardHeader, previewScroll);

        grid.add(chartCard, 0, 0);
        grid.add(tasksPreviewCard, 1, 0);

        return grid;
    }

    public void refresh() {
        try {
            LocalDate logicalDate = registry.getCurrentLogicalDate();

            // Profile greeting
            UserProfile profile = registry.getProfileService().getProfile();
            greetingLabel.setText("Welcome back, " + profile.getName());
            dateLabel.setText(logicalDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));

            // Prepare day instances
            List<TaskDailyInstance> instances = registry.getDailyEngineService().prepareDay(logicalDate);
            DailyRecord dailyRecord = registry.getDailyEngineService().calculateAndSaveDailyRecord(logicalDate, instances);

            // Streaks
            registry.getStreakService().recalculateAllStreaks(logicalDate);
            StreakRecord overallStreak = registry.getStreakService().getOverallStreak();

            // Stats
            completionStatLabel.setText(String.format("%.1f%%", dailyRecord.getCompletionPercentage()));
            tasksRatioStatLabel.setText(dailyRecord.getCompletedActiveTasks() + " / " + dailyRecord.getTotalActiveTasks());
            streakStatLabel.setText(overallStreak.getCurrentStreak() + " Days");

            // Weekly Average
            AnalyticsService.AnalyticsSummary summary = registry.getAnalyticsService().getSummary(logicalDate);
            weeklyAvgStatLabel.setText(String.format("%.1f%%", summary.weeklyAverage));

            // Gauge
            gaugeWidget.setValue(dailyRecord.getCompletionPercentage(), dailyRecord.getDayStatus().name());

            // Streak card info
            streakLevelBadge.setText(overallStreak.getStreakLevel().getDisplayName() + " Level (" + overallStreak.getCurrentStreak() + "d)");
            streakThresholdDesc.setText("Requires ≥ " + (int) overallStreak.getStreakLevel().getMinCompletionPercentage() + "% daily completion");
            longestStreakLabel.setText("Longest streak: " + overallStreak.getLongestStreak() + " days");

            // Trend Chart
            List<AnalyticsService.TrendPoint> trend = registry.getAnalyticsService().getTrend(AnalyticsService.TimeFilter.DAYS_7, logicalDate);
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            DateTimeFormatter df = DateTimeFormatter.ofPattern("E d");
            for (AnalyticsService.TrendPoint pt : trend) {
                series.getData().add(new XYChart.Data<>(pt.date.format(df), pt.percentage));
            }
            trendChart.getData().clear();
            trendChart.getData().add(series);

            // Tasks Preview List
            tasksPreviewList.getChildren().clear();
            if (instances.isEmpty()) {
                Label empty = new Label("No tasks planned for today.");
                empty.setStyle("-fx-text-fill: #64748B; -fx-font-style: italic; -fx-padding: 10;");
                tasksPreviewList.getChildren().add(empty);
            } else {
                for (TaskDailyInstance inst : instances) {
                    if (inst.isDeletedOccurrence()) continue;

                    HBox row = new HBox(10);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setStyle("-fx-background-color: #0D1527; -fx-padding: 8 12; -fx-background-radius: 8px; -fx-border-color: #1E293B; -fx-border-radius: 8px;");

                    CheckBox cb = new CheckBox();
                    cb.setSelected(inst.isCompleted());
                    cb.setOnAction(e -> {
                        try {
                            registry.getDailyEngineService().toggleTaskCompletion(logicalDate, inst.getTaskId(), cb.isSelected());
                            refresh();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });

                    Label nameLbl = new Label(inst.getTaskNameSnapshot());
                    nameLbl.setStyle(inst.isCompleted() ? "-fx-text-fill: #64748B; -fx-strikethrough: true;" : "-fx-text-fill: #F8FAFC; -fx-font-weight: 500;");
                    HBox.setHgrow(nameLbl, Priority.ALWAYS);

                    if (inst.isLateAddition()) {
                        Label lateBadge = new Label("Added today");
                        lateBadge.setStyle("-fx-font-size: 10px; -fx-text-fill: #F59E0B; -fx-background-color: rgba(245, 158, 11, 0.15); -fx-padding: 2 6; -fx-background-radius: 4px;");
                        row.getChildren().addAll(cb, nameLbl, lateBadge);
                    } else {
                        row.getChildren().addAll(cb, nameLbl);
                    }

                    tasksPreviewList.getChildren().add(row);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
