package com.everyday.ui.dialog;

import com.everyday.model.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.LocalDate;

public class TaskDetailsDialog {

    public enum DetailAction {
        NONE,
        EDIT,
        DELETE,
        TOGGLE_ACTIVE,
        UPDATE_PROGRESS
    }

    public static class DetailResult {
        public DetailAction action = DetailAction.NONE;
        public double newProgress;
    }

    public static DetailResult show(Stage owner, Task task, LocalDate currentLogicalDate) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle("Task Details: " + task.getName());

        VBox root = new VBox(14);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #131D31; -fx-pref-width: 480px;");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(task.getName());
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #F8FAFC;");

        Label catBadge = new Label(task.getCategory() != null ? task.getCategory().getName() : "General");
        catBadge.setStyle(String.format("-fx-background-color: %s22; -fx-text-fill: %s; -fx-font-weight: 600; -fx-padding: 3 8; -fx-background-radius: 6px;",
                task.getCategory() != null ? task.getCategory().getColor() : "#3B82F6",
                task.getCategory() != null ? task.getCategory().getColor() : "#3B82F6"));

        header.getChildren().addAll(titleLabel, catBadge);

        // Details Grid
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);

        addRow(grid, 0, "Time / Duration:", (task.getScheduledTime() != null ? task.getScheduledTime() : "Unset") + " (" + task.getDurationMinutes() + " mins)");
        addRow(grid, 1, "Project:", task.getProject() != null ? task.getProject().getName() : "None (Standalone)");
        addRow(grid, 2, "Start Date:", task.getStartDate().toString());
        addRow(grid, 3, "Schedule:", task.getSchedule() != null ? task.getSchedule().getScheduleType().name() : "EVERY_DAY");
        addRow(grid, 4, "Status:", task.isActive() ? "ON (Active in daily schedule)" : "OFF (Paused / Inactive)");

        if (task.isMultiDay()) {
            String multiDayStr = task.getMultiDayDayProgressString(currentLogicalDate);
            addRow(grid, 5, "Multi-day Period:", (task.getStartDate() + " → " + task.getMultiDayEndDate()) + " [" + multiDayStr + "]");
        }

        if (task.isReminderEnabled()) {
            addRow(grid, 6, "Reminder:", "Active at " + task.getReminderTime());
        }

        // Independent Task Progress
        VBox progBox = new VBox(6);
        Label progTitle = new Label("Task Independent Progress:");
        progTitle.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px; -fx-font-weight: 600;");

        Slider progSlider = new Slider(0, 100, task.getTaskProgress());
        progSlider.setShowTickLabels(true);
        progSlider.setShowTickMarks(true);
        Label progVal = new Label(String.format("%.0f%%", progSlider.getValue()));
        progVal.setStyle("-fx-text-fill: #00D2FF; -fx-font-weight: 700;");
        progSlider.valueProperty().addListener((obs, oldV, newV) -> progVal.setText(String.format("%.0f%%", newV.doubleValue())));

        HBox pRow = new HBox(10, progSlider, progVal);
        pRow.setAlignment(Pos.CENTER_LEFT);
        progBox.getChildren().addAll(progTitle, pRow);

        // Notes
        VBox notesBox = new VBox(4);
        Label notesTitle = new Label("Notes:");
        notesTitle.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px; -fx-font-weight: 600;");
        Label notesContent = new Label(task.getNotes() != null && !task.getNotes().isBlank() ? task.getNotes() : "No notes attached.");
        notesContent.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 13px; -fx-wrap-text: true;");
        notesBox.getChildren().addAll(notesTitle, notesContent);

        // Actions
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button toggleBtn = new Button(task.isActive() ? "Turn OFF" : "Turn ON");
        toggleBtn.getStyleClass().add("btn-secondary");

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("btn-secondary");

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("btn-danger");

        Button closeBtn = new Button("Save & Close");
        closeBtn.getStyleClass().add("btn-primary");

        final DetailResult result = new DetailResult();

        toggleBtn.setOnAction(e -> {
            result.action = DetailAction.TOGGLE_ACTIVE;
            result.newProgress = progSlider.getValue();
            dialog.close();
        });

        editBtn.setOnAction(e -> {
            result.action = DetailAction.EDIT;
            result.newProgress = progSlider.getValue();
            dialog.close();
        });

        deleteBtn.setOnAction(e -> {
            result.action = DetailAction.DELETE;
            dialog.close();
        });

        closeBtn.setOnAction(e -> {
            result.action = DetailAction.UPDATE_PROGRESS;
            result.newProgress = progSlider.getValue();
            dialog.close();
        });

        actions.getChildren().addAll(toggleBtn, editBtn, deleteBtn, closeBtn);

        root.getChildren().addAll(header, grid, progBox, notesBox, actions);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(TaskDetailsDialog.class.getResource("/com/everyday/css/styles.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();

        return result;
    }

    private static void addRow(GridPane grid, int row, String label, String value) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px; -fx-font-weight: 600;");
        Label val = new Label(value != null ? value : "-");
        val.setStyle("-fx-text-fill: #F8FAFC; -fx-font-size: 13px;");
        grid.add(lbl, 0, row);
        grid.add(val, 1, row);
    }
}
