package com.everyday.ui.component;

import com.everyday.model.SkippedDay;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Consumer;

/**
 * GitHub-like contribution heatmap showing consistency over time.
 * Cell intensity reflects daily completion percentage.
 */
public class ContributionHeatMap extends VBox {

    private final GridPane grid;
    private Consumer<LocalDate> onDateClicked;

    public ContributionHeatMap() {
        this.setSpacing(10);
        this.getStyleClass().add("glass-card");

        Label title = new Label("Consistency Calendar");
        title.getStyleClass().add("card-header-title");

        grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);
        grid.setAlignment(Pos.CENTER_LEFT);

        // Legend at bottom
        HBox legend = new HBox(8);
        legend.setAlignment(Pos.CENTER_RIGHT);
        legend.setPadding(new Insets(6, 0, 0, 0));

        Label less = new Label("Less");
        less.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
        Label more = new Label("More");
        more.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        Rectangle l0 = createCell(null, 0.0, false);
        Rectangle l1 = createCell(null, 25.0, false);
        Rectangle l2 = createCell(null, 50.0, false);
        Rectangle l3 = createCell(null, 75.0, false);
        Rectangle l4 = createCell(null, 100.0, false);

        Label skippedLbl = new Label("  Skipped:");
        skippedLbl.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
        Rectangle lSkip = createCell(null, -1.0, true);

        legend.getChildren().addAll(less, l0, l1, l2, l3, l4, more, skippedLbl, lSkip);

        this.getChildren().addAll(title, grid, legend);
    }

    public void setOnDateClicked(Consumer<LocalDate> onDateClicked) {
        this.onDateClicked = onDateClicked;
    }

    public void render(LocalDate endDate, int weeksBack, Map<LocalDate, Double> completionData, Map<LocalDate, SkippedDay> skippedData) {
        grid.getChildren().clear();

        if (endDate == null) endDate = LocalDate.now();
        LocalDate startDate = endDate.minusWeeks(weeksBack).with(DayOfWeek.MONDAY);

        // Column index
        int col = 0;
        LocalDate cur = startDate;

        while (!cur.isAfter(endDate)) {
            for (int row = 0; row < 7; row++) {
                LocalDate cellDate = cur.plusDays(row);
                if (cellDate.isAfter(endDate)) break;

                boolean isSkipped = skippedData != null && skippedData.containsKey(cellDate);
                double pct = completionData != null ? completionData.getOrDefault(cellDate, 0.0) : 0.0;
                boolean hasData = completionData != null && completionData.containsKey(cellDate);

                Rectangle rect = createCell(cellDate, isSkipped ? -1.0 : (hasData ? pct : 0.0), isSkipped);

                // Tooltip
                String tooltipText;
                if (isSkipped) {
                    tooltipText = cellDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) + " - Skipped (" + skippedData.get(cellDate).getReason() + ")";
                } else if (hasData) {
                    tooltipText = cellDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) + " - " + pct + "% completed";
                } else {
                    tooltipText = cellDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) + " - No activity";
                }
                Tooltip.install(rect, new Tooltip(tooltipText));

                LocalDate finalDate = cellDate;
                rect.setOnMouseClicked(e -> {
                    if (onDateClicked != null) {
                        onDateClicked.accept(finalDate);
                    }
                });

                grid.add(rect, col, row);
            }
            cur = cur.plusWeeks(1);
            col++;
        }
    }

    private Rectangle createCell(LocalDate date, double pct, boolean isSkipped) {
        Rectangle rect = new Rectangle(13, 13);
        rect.setArcWidth(4);
        rect.setArcHeight(4);

        if (isSkipped) {
            rect.setFill(Color.web("#8B5CF6")); // Purple for skipped
        } else if (pct <= 0.0) {
            rect.setFill(Color.web("#141E33"));
        } else if (pct <= 25.0) {
            rect.setFill(Color.web("#1E3A5F"));
        } else if (pct <= 50.0) {
            rect.setFill(Color.web("#2563EB"));
        } else if (pct <= 75.0) {
            rect.setFill(Color.web("#3B82F6"));
        } else {
            rect.setFill(Color.web("#00D2FF")); // Cyan glow for 100%
        }

        rect.setStroke(Color.web("#1E293B"));
        rect.setStrokeWidth(0.5);
        rect.setStyle("-fx-cursor: hand;");

        rect.setOnMouseEntered(e -> {
            rect.setStroke(Color.web("#60A5FA"));
            rect.setStrokeWidth(1.5);
        });
        rect.setOnMouseExited(e -> {
            rect.setStroke(Color.web("#1E293B"));
            rect.setStrokeWidth(0.5);
        });

        return rect;
    }
}
