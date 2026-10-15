package com.everyday.ui.view;

import com.everyday.model.SkippedDay;
import com.everyday.model.StreakLevel;
import com.everyday.model.StreakRecord;
import com.everyday.service.ServiceRegistry;
import com.everyday.ui.component.Icons;
import com.everyday.ui.dialog.SkipDayDialog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;

public class StreaksView extends ScrollPane {

    private final ServiceRegistry registry;
    private final Stage primaryStage;

    // Overall Streak widgets
    private Label currentStreakNumber;
    private Label longestStreakNumber;
    private Label levelNameLabel;
    private Label thresholdNoticeLabel;
    private ProgressBar levelProgressBar;

    // Level ladder container
    private HBox levelLadderBox;

    // Activity Streaks
    private GridPane activityGrid;

    // Skipped days history
    private VBox skippedDaysContainer;

    public StreaksView(ServiceRegistry registry, Stage primaryStage) {
        this.registry = registry;
        this.primaryStage = primaryStage;

        this.setFitToWidth(true);
        this.setStyle("-fx-background-color: transparent;");

        VBox content = new VBox(24);
        content.setPadding(new Insets(28, 36, 40, 36));

        // 1. Header with Skip Day Action
        content.getChildren().add(buildHeader());

        // 2. Main Overall Streak Hero Card
        content.getChildren().add(buildOverallStreakCard());

        // 3. Streak Level Ladder (Beginner -> Elite -> Epic -> Master -> Legend)
        content.getChildren().add(buildLevelLadderCard());

        // 4. Individual Activity Streaks Section
        content.getChildren().add(buildActivityStreaksSection());

        // 5. Intentionally Skipped Days Audit History
        content.getChildren().add(buildSkippedDaysAuditSection());

        this.setContent(content);
        refresh();
    }

    private Node buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Streaks & Consistency");
        title.getStyleClass().add("section-title");

        Label subtitle = new Label("Momentum tracking, milestone tiers, and protected rest periods");
        subtitle.getStyleClass().add("section-subtitle");
        titleBox.getChildren().addAll(title, subtitle);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button skipBtn = new Button("Intentionally Skip Day");
        skipBtn.getStyleClass().add("btn-secondary");
        skipBtn.setGraphic(Icons.getIcon("skip", 16, "#A78BFA"));
        skipBtn.setOnAction(e -> handleSkipDay());

        header.getChildren().addAll(titleBox, skipBtn);
        return header;
    }

    private Node buildOverallStreakCard() {
        HBox card = new HBox(28);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("glass-card");
        card.setStyle("-fx-background-color: linear-gradient(to right, #111D35, #0E1628); -fx-border-color: #1E293B;");
        card.setPadding(new Insets(28, 32, 28, 32));

        // Fire Icon Large
        VBox fireBox = new VBox();
        fireBox.setAlignment(Pos.CENTER);
        fireBox.setStyle("-fx-background-color: rgba(245, 158, 11, 0.15); -fx-padding: 24px; -fx-background-radius: 20px;");
        fireBox.getChildren().add(Icons.getIcon("flame", 48, "#F59E0B"));

        // Main Stats
        VBox stats = new VBox(6);
        Label heading = new Label("OVERALL PRODUCTIVITY STREAK");
        heading.getStyleClass().add("stat-label");

        currentStreakNumber = new Label("0 Days");
        currentStreakNumber.setStyle("-fx-font-size: 38px; -fx-font-weight: 900; -fx-text-fill: #F8FAFC; -fx-letter-spacing: -1px;");

        HBox metaRow = new HBox(16);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        levelNameLabel = new Label("Beginner Level");
        levelNameLabel.getStyleClass().addAll("stat-badge");

        longestStreakNumber = new Label("All-time Longest: 0 Days");
        longestStreakNumber.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px; -fx-font-weight: 600;");

        metaRow.getChildren().addAll(levelNameLabel, longestStreakNumber);

        thresholdNoticeLabel = new Label("To preserve this streak: Must complete ≥ 50% daily active tasks.");
        thresholdNoticeLabel.setStyle("-fx-text-fill: #60A5FA; -fx-font-size: 12px;");

        stats.getChildren().addAll(heading, currentStreakNumber, metaRow, thresholdNoticeLabel);
        HBox.setHgrow(stats, Priority.ALWAYS);

        card.getChildren().addAll(fireBox, stats);
        return card;
    }

    private Node buildLevelLadderCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("glass-card");
        card.setPadding(new Insets(20));

        Label title = new Label("Streak Tier Ladder");
        title.getStyleClass().add("card-header-title");

        levelProgressBar = new ProgressBar(0.0);
        levelProgressBar.setMaxWidth(Double.MAX_VALUE);
        levelProgressBar.setPrefHeight(8);
        levelProgressBar.getStyleClass().add("progress-bar");

        levelLadderBox = new HBox(12);
        levelLadderBox.setAlignment(Pos.CENTER);

        for (StreakLevel lvl : StreakLevel.values()) {
            VBox lvlCard = new VBox(4);
            lvlCard.setAlignment(Pos.CENTER);
            lvlCard.setPadding(new Insets(10, 14, 10, 14));
            lvlCard.setStyle("-fx-background-color: #0D1527; -fx-border-color: #1E293B; -fx-border-radius: 8px; -fx-background-radius: 8px;");

            Label name = new Label(lvl.getDisplayName());
            name.setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: #F8FAFC;");

            Label range = new Label(lvl.getMinDays() + (lvl.getMaxDays() == Integer.MAX_VALUE ? "+ days" : "–" + lvl.getMaxDays() + " days"));
            range.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");

            Label threshold = new Label("Min: " + (int) lvl.getMinCompletionPercentage() + "%");
            threshold.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #00D2FF;");

            lvlCard.getChildren().addAll(name, range, threshold);
            HBox.setHgrow(lvlCard, Priority.ALWAYS);
            levelLadderBox.getChildren().add(lvlCard);
        }

        card.getChildren().addAll(title, levelProgressBar, levelLadderBox);
        return card;
    }

    private Node buildActivityStreaksSection() {
        VBox section = new VBox(12);

        Label title = new Label("Individual Activity Streaks");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #F8FAFC;");

        activityGrid = new GridPane();
        activityGrid.setHgap(16);
        activityGrid.setVgap(16);

        section.getChildren().addAll(title, activityGrid);
        return section;
    }

    private Node buildSkippedDaysAuditSection() {
        VBox section = new VBox(12);

        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Intentionally Skipped Days");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #C4B5FD;");

        Label notice = new Label("• Permanently stored reasons. Skipped days do not break nor increase streaks.");
        notice.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");

        titleRow.getChildren().addAll(title, notice);

        skippedDaysContainer = new VBox(8);

        section.getChildren().addAll(titleRow, skippedDaysContainer);
        return section;
    }

    public void refresh() {
        try {
            LocalDate logicalDate = registry.getCurrentLogicalDate();
            registry.getStreakService().recalculateAllStreaks(logicalDate);

            // 1. Overall Streak
            StreakRecord overall = registry.getStreakService().getOverallStreak();
            currentStreakNumber.setText(overall.getCurrentStreak() + " Days");
            longestStreakNumber.setText("All-time Longest: " + overall.getLongestStreak() + " Days");

            StreakLevel currentLevel = overall.getStreakLevel();
            levelNameLabel.setText(currentLevel.getDisplayName() + " Tier");
            thresholdNoticeLabel.setText("To preserve this streak: Must complete ≥ " + (int) currentLevel.getMinCompletionPercentage() + "% daily active tasks.");

            // Progress bar to next level (out of 400 for legend)
            levelProgressBar.setProgress(Math.min(1.0, (double) overall.getCurrentStreak() / 400.0));

            // Highlight current ladder tier
            int levelIndex = currentLevel.ordinal();
            for (int i = 0; i < levelLadderBox.getChildren().size(); i++) {
                Node node = levelLadderBox.getChildren().get(i);
                if (i == levelIndex) {
                    node.setStyle("-fx-background-color: rgba(59, 130, 246, 0.2); -fx-border-color: #3B82F6; -fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px;");
                } else if (i < levelIndex) {
                    node.setStyle("-fx-background-color: rgba(16, 185, 129, 0.1); -fx-border-color: rgba(16, 185, 129, 0.3); -fx-border-radius: 8px; -fx-background-radius: 8px;");
                } else {
                    node.setStyle("-fx-background-color: #0D1527; -fx-border-color: #1E293B; -fx-border-radius: 8px; -fx-background-radius: 8px;");
                }
            }

            // 2. Activity Streaks
            activityGrid.getChildren().clear();
            List<StreakRecord> allStreaks = registry.getStreakService().getAllStreaks();
            int col = 0;
            int row = 0;
            for (StreakRecord sr : allStreaks) {
                if ("OVERALL".equalsIgnoreCase(sr.getStreakType())) continue;

                HBox card = new HBox(12);
                card.setAlignment(Pos.CENTER_LEFT);
                card.getStyleClass().add("glass-card");
                card.setPrefWidth(260);
                card.setPadding(new Insets(14, 18, 14, 18));

                VBox textBox = new VBox(4);
                Label aName = new Label(sr.getStreakType());
                aName.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #F8FAFC;");

                Label aStreak = new Label(sr.getCurrentStreak() + " Days (Best: " + sr.getLongestStreak() + "d)");
                aStreak.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #00D2FF;");

                textBox.getChildren().addAll(aName, aStreak);
                HBox.setHgrow(textBox, Priority.ALWAYS);

                Label icon = new Label("🔥");
                icon.setStyle("-fx-font-size: 18px;");

                card.getChildren().addAll(textBox, icon);
                activityGrid.add(card, col, row);

                col++;
                if (col > 2) {
                    col = 0;
                    row++;
                }
            }

            // 3. Skipped Days History
            skippedDaysContainer.getChildren().clear();
            List<SkippedDay> skippedList = registry.getSkippedDayRepository().getAll();
            if (skippedList.isEmpty()) {
                Label noSkips = new Label("No skipped days recorded.");
                noSkips.setStyle("-fx-text-fill: #64748B; -fx-font-style: italic; -fx-padding: 8 0;");
                skippedDaysContainer.getChildren().add(noSkips);
            } else {
                for (SkippedDay sd : skippedList) {
                    HBox sRow = new HBox(12);
                    sRow.setAlignment(Pos.CENTER_LEFT);
                    sRow.setStyle("-fx-background-color: rgba(139, 92, 246, 0.08); -fx-border-color: rgba(139, 92, 246, 0.2); -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 10 16;");

                    Label tag = new Label("SKIPPED");
                    tag.setStyle("-fx-background-color: rgba(139, 92, 246, 0.25); -fx-text-fill: #DDD6FE; -fx-font-weight: 800; -fx-font-size: 10px; -fx-padding: 2 6; -fx-background-radius: 4px;");

                    Label date = new Label(sd.getLogicalDate().toString());
                    date.setStyle("-fx-text-fill: #F8FAFC; -fx-font-weight: 600;");

                    Label reason = new Label("Reason: " + sd.getReason());
                    reason.setStyle("-fx-text-fill: #CBD5E1; -fx-wrap-text: true;");
                    HBox.setHgrow(reason, Priority.ALWAYS);

                    sRow.getChildren().addAll(tag, date, reason);
                    skippedDaysContainer.getChildren().add(sRow);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleSkipDay() {
        LocalDate logicalDate = registry.getCurrentLogicalDate();
        SkipDayDialog.SkipResult res = SkipDayDialog.show(primaryStage, logicalDate);
        if (res != null) {
            try {
                registry.getStreakService().skipDay(res.date, res.reason);
                refresh();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
