package com.everyday.ui.view;

import com.everyday.model.SkippedDay;
import com.everyday.service.CalendarService;
import com.everyday.service.CalendarService.DayDetail;
import com.everyday.service.CalendarService.DayVisualState;
import com.everyday.service.ServiceRegistry;
import com.everyday.ui.component.ContributionHeatMap;
import com.everyday.ui.component.Icons;
import com.everyday.ui.dialog.DayDetailDialog;
import com.everyday.ui.dialog.SkipDayDialog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarView extends ScrollPane {

    private final ServiceRegistry registry;
    private final Stage primaryStage;

    private YearMonth currentYearMonth = YearMonth.now();
    private Label monthTitleLabel;
    private GridPane calendarGrid;
    private ContributionHeatMap heatMap;

    public CalendarView(ServiceRegistry registry, Stage primaryStage) {
        this.registry = registry;
        this.primaryStage = primaryStage;

        this.setFitToWidth(true);
        this.setStyle("-fx-background-color: transparent;");

        VBox content = new VBox(24);
        content.setPadding(new Insets(28, 36, 40, 36));

        // 1. Header with Month Nav & Skip Day Button
        content.getChildren().add(buildHeader());

        // 2. Contribution Heatmap (GitHub-like)
        heatMap = new ContributionHeatMap();
        heatMap.setOnDateClicked(this::openDayDetails);
        content.getChildren().add(heatMap);

        // 3. Month Navigation Card & Calendar Grid
        content.getChildren().add(buildMonthCalendarSection());

        this.setContent(content);
        refresh();
    }

    private Node buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Calendar & History");
        title.getStyleClass().add("section-title");

        Label subtitle = new Label("Visual daily consistency, month history, and audit log");
        subtitle.getStyleClass().add("section-subtitle");
        titleBox.getChildren().addAll(title, subtitle);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button skipDayBtn = new Button("Skip a Day");
        skipDayBtn.getStyleClass().add("btn-secondary");
        skipDayBtn.setGraphic(Icons.getIcon("skip", 16, "#A78BFA"));
        skipDayBtn.setOnAction(e -> handleSkipDay());

        header.getChildren().addAll(titleBox, skipDayBtn);
        return header;
    }

    private Node buildMonthCalendarSection() {
        VBox card = new VBox(16);
        card.getStyleClass().add("glass-card");
        card.setPadding(new Insets(20));

        // Month Nav Controls
        HBox navRow = new HBox(14);
        navRow.setAlignment(Pos.CENTER_LEFT);

        monthTitleLabel = new Label();
        monthTitleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #F8FAFC;");
        HBox.setHgrow(monthTitleLabel, Priority.ALWAYS);

        Button prevBtn = new Button("◀");
        prevBtn.getStyleClass().add("btn-secondary");
        prevBtn.setOnAction(e -> {
            currentYearMonth = currentYearMonth.minusMonths(1);
            refreshMonthGrid();
        });

        Button todayBtn = new Button("Today");
        todayBtn.getStyleClass().add("btn-secondary");
        todayBtn.setOnAction(e -> {
            currentYearMonth = YearMonth.now();
            refreshMonthGrid();
        });

        Button nextBtn = new Button("▶");
        nextBtn.getStyleClass().add("btn-secondary");
        nextBtn.setOnAction(e -> {
            currentYearMonth = currentYearMonth.plusMonths(1);
            refreshMonthGrid();
        });

        navRow.getChildren().addAll(monthTitleLabel, prevBtn, todayBtn, nextBtn);

        // Day of week headers
        GridPane dayOfWeekHeaders = new GridPane();
        dayOfWeekHeaders.setHgap(8);
        String[] days = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
        for (int i = 0; i < 7; i++) {
            Label d = new Label(days[i]);
            d.setStyle("-fx-text-fill: #64748B; -fx-font-weight: 700; -fx-font-size: 11px; -fx-alignment: CENTER;");
            d.setPrefWidth(90);
            d.setAlignment(Pos.CENTER);
            dayOfWeekHeaders.add(d, i, 0);
        }

        // Calendar Day Grid
        calendarGrid = new GridPane();
        calendarGrid.setHgap(8);
        calendarGrid.setVgap(8);

        card.getChildren().addAll(navRow, dayOfWeekHeaders, calendarGrid);
        return card;
    }

    public void refresh() {
        try {
            LocalDate today = registry.getCurrentLogicalDate();

            // Refresh Heatmap
            Map<LocalDate, Double> heatData = registry.getCalendarService().getHeatMapData(today.minusWeeks(24), today);
            List<SkippedDay> skippedList = registry.getSkippedDayRepository().getAll();
            Map<LocalDate, SkippedDay> skippedMap = new HashMap<>();
            for (SkippedDay s : skippedList) {
                skippedMap.put(s.getLogicalDate(), s);
            }
            heatMap.render(today, 24, heatData, skippedMap);

            // Refresh Month Grid
            refreshMonthGrid();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshMonthGrid() {
        calendarGrid.getChildren().clear();
        monthTitleLabel.setText(currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));

        try {
            Map<LocalDate, DayVisualState> states = registry.getCalendarService().getMonthVisualStates(currentYearMonth);
            LocalDate firstDay = currentYearMonth.atDay(1);
            int dayOfWeekOffset = firstDay.getDayOfWeek().getValue() - 1; // 0 for Monday

            int daysInMonth = currentYearMonth.lengthOfMonth();
            LocalDate today = registry.getCurrentLogicalDate();

            int col = dayOfWeekOffset;
            int row = 0;

            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate date = currentYearMonth.atDay(day);
                DayVisualState state = states.getOrDefault(date, DayVisualState.EMPTY_OR_FUTURE);

                Node cell = buildDateCell(date, state, date.equals(today));
                calendarGrid.add(cell, col, row);

                col++;
                if (col > 6) {
                    col = 0;
                    row++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Node buildDateCell(LocalDate date, DayVisualState state, boolean isToday) {
        VBox cell = new VBox(4);
        cell.setAlignment(Pos.TOP_LEFT);
        cell.setPrefSize(90, 75);
        cell.setPadding(new Insets(8));
        cell.setStyle("-fx-background-color: #0E1628; -fx-background-radius: 8px; -fx-border-color: #1E293B; -fx-border-radius: 8px; -fx-cursor: hand;");

        if (isToday) {
            cell.setStyle("-fx-background-color: #132242; -fx-background-radius: 8px; -fx-border-color: #3B82F6; -fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-cursor: hand;");
        }

        HBox topRow = new HBox();
        Label dayNum = new Label(String.valueOf(date.getDayOfMonth()));
        dayNum.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: " + (isToday ? "#60A5FA" : "#F8FAFC") + ";");
        HBox.setHgrow(dayNum, Priority.ALWAYS);

        topRow.getChildren().add(dayNum);

        Label stateBadge = new Label(getStateBadgeText(state));
        stateBadge.setStyle(getStateBadgeStyle(state));

        cell.getChildren().addAll(topRow, stateBadge);

        cell.setOnMouseClicked(e -> openDayDetails(date));

        cell.setOnMouseEntered(e -> {
            if (!isToday) {
                cell.setStyle("-fx-background-color: #16243E; -fx-background-radius: 8px; -fx-border-color: #334155; -fx-border-radius: 8px; -fx-cursor: hand;");
            }
        });
        cell.setOnMouseExited(e -> {
            if (!isToday) {
                cell.setStyle("-fx-background-color: #0E1628; -fx-background-radius: 8px; -fx-border-color: #1E293B; -fx-border-radius: 8px; -fx-cursor: hand;");
            }
        });

        return cell;
    }

    private String getStateBadgeText(DayVisualState state) {
        switch (state) {
            case COMPLETED: return "100%";
            case INCOMPLETE_STREAK_CONTINUED: return "Streak ✓";
            case SKIPPED: return "Skipped";
            case STREAK_BROKEN: return "Broken";
            default: return "";
        }
    }

    private String getStateBadgeStyle(DayVisualState state) {
        switch (state) {
            case COMPLETED:
                return "-fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #34D399; -fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 2 4; -fx-background-radius: 4px;";
            case INCOMPLETE_STREAK_CONTINUED:
                return "-fx-background-color: rgba(59, 130, 246, 0.2); -fx-text-fill: #60A5FA; -fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 2 4; -fx-background-radius: 4px;";
            case SKIPPED:
                return "-fx-background-color: rgba(139, 92, 246, 0.2); -fx-text-fill: #C4B5FD; -fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 2 4; -fx-background-radius: 4px;";
            case STREAK_BROKEN:
                return "-fx-background-color: rgba(239, 68, 68, 0.2); -fx-text-fill: #F87171; -fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 2 4; -fx-background-radius: 4px;";
            default:
                return "-fx-background-color: transparent;";
        }
    }

    private void openDayDetails(LocalDate date) {
        try {
            DayDetail detail = registry.getCalendarService().getDayDetail(date);
            DayDetailDialog.show(primaryStage, detail);
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
