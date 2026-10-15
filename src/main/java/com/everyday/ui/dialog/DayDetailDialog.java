package com.everyday.ui.dialog;

import com.everyday.model.MissedRecord;
import com.everyday.model.TaskDailyInstance;
import com.everyday.service.CalendarService.DayDetail;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.format.DateTimeFormatter;

public class DayDetailDialog {

    public static void show(Stage owner, DayDetail detail) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle("Day History: " + detail.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")));

        VBox root = new VBox(14);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #131D31; -fx-pref-width: 480px;");

        // Header
        Label titleLabel = new Label(detail.date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #F8FAFC;");

        // Metrics Banner
        HBox banner = new HBox(16);
        banner.setPadding(new Insets(12));
        banner.setStyle("-fx-background-color: #0D1527; -fx-border-color: #1E293B; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        VBox col1 = new VBox(2, new Label("Completion"), new Label(String.format("%.1f%%", detail.completionPercentage)));
        col1.getChildren().get(0).setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
        col1.getChildren().get(1).setStyle("-fx-text-fill: #00D2FF; -fx-font-size: 18px; -fx-font-weight: 800;");

        VBox col2 = new VBox(2, new Label("Tasks Done"), new Label(detail.tasksCompleted + " / " + detail.totalTasks));
        col2.getChildren().get(0).setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
        col2.getChildren().get(1).setStyle("-fx-text-fill: #F8FAFC; -fx-font-size: 18px; -fx-font-weight: 800;");

        VBox col3 = new VBox(2, new Label("Streak State"), new Label(detail.streakState != null ? detail.streakState : "None"));
        col3.getChildren().get(0).setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
        col3.getChildren().get(1).setStyle("-fx-text-fill: #60A5FA; -fx-font-size: 16px; -fx-font-weight: 700;");

        banner.getChildren().addAll(col1, col2, col3);

        VBox contentBox = new VBox(12);

        // Skipped reason if present
        if (detail.skippedReason != null) {
            VBox skipBox = new VBox(4);
            skipBox.setPadding(new Insets(10));
            skipBox.setStyle("-fx-background-color: rgba(139, 92, 246, 0.15); -fx-border-color: #8B5CF6; -fx-border-radius: 6px; -fx-background-radius: 6px;");
            Label sTitle = new Label("Intentionally Skipped");
            sTitle.setStyle("-fx-text-fill: #C4B5FD; -fx-font-weight: 700;");
            Label sReason = new Label("Reason: " + detail.skippedReason);
            sReason.setStyle("-fx-text-fill: #EDE9FE; -fx-wrap-text: true;");
            skipBox.getChildren().addAll(sTitle, sReason);
            contentBox.getChildren().add(skipBox);
        }

        // Completed tasks section
        Label compHeader = new Label("Completed Tasks (" + detail.completedTasks.size() + ")");
        compHeader.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #10B981;");
        VBox compList = new VBox(6);
        if (detail.completedTasks.isEmpty()) {
            Label empty = new Label("No completed tasks.");
            empty.setStyle("-fx-text-fill: #64748B; -fx-font-style: italic;");
            compList.getChildren().add(empty);
        } else {
            for (TaskDailyInstance t : detail.completedTasks) {
                HBox row = new HBox(8);
                Label check = new Label("☑");
                check.setStyle("-fx-text-fill: #10B981;");
                Label name = new Label(t.getTaskNameSnapshot());
                name.setStyle("-fx-text-fill: #F8FAFC;");
                Label cat = new Label("[" + t.getCategoryNameSnapshot() + "]");
                cat.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
                row.getChildren().addAll(check, name, cat);
                compList.getChildren().add(row);
            }
        }
        contentBox.getChildren().addAll(compHeader, compList);

        // Missed tasks section
        Label missedHeader = new Label("Missed Tasks (" + detail.missedTasks.size() + ")");
        missedHeader.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #EF4444;");
        VBox missedList = new VBox(6);
        if (detail.missedTasks.isEmpty()) {
            Label empty = new Label("No missed tasks.");
            empty.setStyle("-fx-text-fill: #64748B; -fx-font-style: italic;");
            missedList.getChildren().add(empty);
        } else {
            for (MissedRecord m : detail.missedTasks) {
                HBox row = new HBox(8);
                Label cross = new Label("✕");
                cross.setStyle("-fx-text-fill: #EF4444;");
                Label name = new Label(m.getTaskName());
                name.setStyle("-fx-text-fill: #F8FAFC;");
                Label cat = new Label("[" + m.getCategoryName() + "]");
                cat.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
                row.getChildren().addAll(cross, name, cat);
                missedList.getChildren().add(row);
            }
        }
        contentBox.getChildren().addAll(missedHeader, missedList);

        ScrollPane scroll = new ScrollPane(contentBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        scroll.setPrefHeight(220);

        HBox btnBox = new HBox();
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-secondary");
        closeBtn.setOnAction(e -> dialog.close());
        btnBox.getChildren().add(closeBtn);

        root.getChildren().addAll(titleLabel, banner, scroll, btnBox);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(DayDetailDialog.class.getResource("/com/everyday/css/styles.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
