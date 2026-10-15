package com.everyday.ui.view;

import com.everyday.model.*;
import com.everyday.service.ServiceRegistry;
import com.everyday.ui.component.Icons;
import com.everyday.ui.dialog.AddTaskDialog;
import com.everyday.ui.dialog.ConfirmDialog;
import com.everyday.ui.dialog.ConfirmDialog.DeleteChoice;
import com.everyday.ui.dialog.TaskDetailsDialog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DailyTasksView extends ScrollPane {

    private final ServiceRegistry registry;
    private final Stage primaryStage;

    private Label dateHeadingLabel;
    private Label completionRatioLabel;
    private Label completionPercentLabel;
    private ProgressBar dailyProgressBar;
    private VBox tasksContainer;
    private VBox missedContainer;
    private Label missedCountBadge;

    public DailyTasksView(ServiceRegistry registry, Stage primaryStage) {
        this.registry = registry;
        this.primaryStage = primaryStage;

        this.setFitToWidth(true);
        this.setStyle("-fx-background-color: transparent;");

        VBox content = new VBox(24);
        content.setPadding(new Insets(28, 36, 40, 36));

        // 1. Header & Actions
        content.getChildren().add(buildHeader());

        // 2. Daily Completion Progress Banner
        content.getChildren().add(buildDailyProgressBanner());

        // 3. Active Tasks Section
        content.getChildren().add(buildTasksSection());

        // 4. Missed Tasks Section (Historical, never auto carried over)
        content.getChildren().add(buildMissedTasksSection());

        this.setContent(content);
        refresh();
    }

    private Node buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Daily Tasks");
        title.getStyleClass().add("section-title");

        dateHeadingLabel = new Label("Execution Day");
        dateHeadingLabel.getStyleClass().add("section-subtitle");
        titleBox.getChildren().addAll(title, dateHeadingLabel);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button addTaskBtn = new Button("Add Task");
        addTaskBtn.getStyleClass().add("btn-primary");
        addTaskBtn.setGraphic(Icons.getIcon("plus", 16, Color.WHITE));
        addTaskBtn.setOnAction(e -> handleAddTask());

        Button refreshBtn = new Button();
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setGraphic(Icons.getIcon("refresh", 16, "#94A3B8"));
        refreshBtn.setTooltip(new Tooltip("Refresh day evaluation"));
        refreshBtn.setOnAction(e -> refresh());

        header.getChildren().addAll(titleBox, refreshBtn, addTaskBtn);
        return header;
    }

    private Node buildDailyProgressBanner() {
        VBox banner = new VBox(12);
        banner.getStyleClass().add("glass-card");
        banner.setPadding(new Insets(20));

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label bannerTitle = new Label("Today's Daily Completion");
        bannerTitle.getStyleClass().add("card-header-title");
        HBox.setHgrow(bannerTitle, Priority.ALWAYS);

        completionRatioLabel = new Label("0 / 0 tasks");
        completionRatioLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #94A3B8;");

        completionPercentLabel = new Label("0.0%");
        completionPercentLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #00D2FF;");

        topRow.getChildren().addAll(bannerTitle, completionRatioLabel, completionPercentLabel);

        dailyProgressBar = new ProgressBar(0.0);
        dailyProgressBar.setMaxWidth(Double.MAX_VALUE);
        dailyProgressBar.setPrefHeight(10);
        dailyProgressBar.getStyleClass().add("progress-bar");

        Label ruleNotice = new Label("Every active daily task has equal weight. Late additions do not affect today's percentage.");
        ruleNotice.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

        banner.getChildren().addAll(topRow, dailyProgressBar, ruleNotice);
        return banner;
    }

    private Node buildTasksSection() {
        VBox section = new VBox(12);

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label sectionTitle = new Label("Active Plan");
        sectionTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #F8FAFC;");
        titleRow.getChildren().add(sectionTitle);

        tasksContainer = new VBox(10);

        section.getChildren().addAll(titleRow, tasksContainer);
        return section;
    }

    private Node buildMissedTasksSection() {
        VBox section = new VBox(12);
        section.setStyle("-fx-padding: 8 0 0 0;");

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label sectionTitle = new Label("Missed Tasks (Historical)");
        sectionTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #F87171;");

        missedCountBadge = new Label("0");
        missedCountBadge.setStyle("-fx-background-color: rgba(239, 68, 68, 0.15); -fx-text-fill: #F87171; -fx-padding: 2 8; -fx-background-radius: 10px; -fx-font-size: 11px; -fx-font-weight: 700;");

        Label explanation = new Label("• Unfinished tasks do not automatically carry over. They remain archived here.");
        explanation.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        titleRow.getChildren().addAll(sectionTitle, missedCountBadge, explanation);

        missedContainer = new VBox(8);

        section.getChildren().addAll(titleRow, missedContainer);
        return section;
    }

    public void refresh() {
        try {
            LocalDate logicalDate = registry.getCurrentLogicalDate();
            dateHeadingLabel.setText("Logical Day: " + logicalDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")));

            // Archive past unfinished tasks as missed
            registry.getDailyEngineService().archiveMissedTasksForPastDays(logicalDate);

            // Prepare instances for today
            List<TaskDailyInstance> instances = registry.getDailyEngineService().prepareDay(logicalDate);
            DailyRecord record = registry.getDailyEngineService().calculateAndSaveDailyRecord(logicalDate, instances);

            // Update banner
            completionPercentLabel.setText(String.format("%.1f%%", record.getCompletionPercentage()));
            completionRatioLabel.setText(record.getCompletedActiveTasks() + " / " + record.getTotalActiveTasks() + " tasks completed");
            dailyProgressBar.setProgress(record.getTotalActiveTasks() > 0 ? (record.getCompletionPercentage() / 100.0) : 0.0);

            // Populate tasks
            tasksContainer.getChildren().clear();
            if (instances.isEmpty()) {
                VBox empty = new VBox(8);
                empty.getStyleClass().add("empty-state-box");
                Label emptyT = new Label("No tasks planned for today.");
                emptyT.getStyleClass().add("empty-state-title");
                Label emptyS = new Label("Click 'Add Task' above to schedule activities for today.");
                emptyS.getStyleClass().add("empty-state-subtitle");
                empty.getChildren().addAll(emptyT, emptyS);
                tasksContainer.getChildren().add(empty);
            } else {
                for (int i = 0; i < instances.size(); i++) {
                    TaskDailyInstance inst = instances.get(i);
                    tasksContainer.getChildren().add(buildTaskItemNode(inst, logicalDate, i, instances.size()));
                }
            }

            // Populate missed tasks
            missedContainer.getChildren().clear();
            List<MissedRecord> missed = registry.getDailyEngineService().getRecentMissedTasks(10);
            missedCountBadge.setText(String.valueOf(missed.size()));
            if (missed.isEmpty()) {
                Label noMissed = new Label("No recent missed tasks. Great job keeping consistent!");
                noMissed.setStyle("-fx-text-fill: #64748B; -fx-font-style: italic; -fx-padding: 8 0;");
                missedContainer.getChildren().add(noMissed);
            } else {
                for (MissedRecord mr : missed) {
                    HBox mRow = new HBox(12);
                    mRow.setAlignment(Pos.CENTER_LEFT);
                    mRow.setStyle("-fx-background-color: rgba(239, 68, 68, 0.08); -fx-border-color: rgba(239, 68, 68, 0.2); -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 10 16;");

                    Label icon = new Label("✕");
                    icon.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: 800;");

                    VBox mText = new VBox(2);
                    Label mName = new Label(mr.getTaskName());
                    mName.setStyle("-fx-text-fill: #F8FAFC; -fx-font-weight: 600; -fx-font-size: 13px;");

                    Label mMeta = new Label("Missed on " + mr.getLogicalDate() + " • " + mr.getCategoryName() + (mr.getProjectName() != null ? " • " + mr.getProjectName() : ""));
                    mMeta.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");

                    mText.getChildren().addAll(mName, mMeta);
                    HBox.setHgrow(mText, Priority.ALWAYS);

                    mRow.getChildren().addAll(icon, mText);
                    missedContainer.getChildren().add(mRow);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Node buildTaskItemNode(TaskDailyInstance inst, LocalDate logicalDate, int index, int totalCount) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("task-item-card");
        if (inst.isCompleted()) {
            row.getStyleClass().add("task-item-completed");
        }

        // 1. Completion Checkbox
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

        // 2. Task Details Info
        VBox infoBox = new VBox(4);
        Label titleLbl = new Label(inst.getTaskNameSnapshot());
        titleLbl.getStyleClass().add(inst.isCompleted() ? "task-title-completed" : "task-title");

        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label catBadge = new Label(inst.getCategoryNameSnapshot());
        catBadge.getStyleClass().add("task-category-pill");
        catBadge.setStyle("-fx-background-color: rgba(59, 130, 246, 0.15); -fx-text-fill: #60A5FA;");
        metaRow.getChildren().add(catBadge);

        if (inst.getProjectNameSnapshot() != null && !inst.getProjectNameSnapshot().isBlank()) {
            Label projBadge = new Label("📁 " + inst.getProjectNameSnapshot());
            projBadge.setStyle("-fx-font-size: 11px; -fx-text-fill: #A78BFA; -fx-background-color: rgba(139, 92, 246, 0.15); -fx-padding: 2 6; -fx-background-radius: 4px;");
            metaRow.getChildren().add(projBadge);
        }

        Task task = inst.getTask();
        if (task != null) {
            if (task.getScheduledTime() != null) {
                Label timeLbl = new Label("⏰ " + task.getScheduledTime() + " (" + task.getDurationMinutes() + "m)");
                timeLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
                metaRow.getChildren().add(timeLbl);
            }

            // Multi-day task badge & progress
            if (task.isMultiDay()) {
                String multiDayStr = task.getMultiDayDayProgressString(logicalDate);
                Label multiDayBadge = new Label("🗓 " + multiDayStr);
                multiDayBadge.setStyle("-fx-font-size: 11px; -fx-text-fill: #34D399; -fx-background-color: rgba(16, 185, 129, 0.15); -fx-padding: 2 6; -fx-background-radius: 4px;");
                metaRow.getChildren().add(multiDayBadge);

                Label taskProgBadge = new Label(String.format("Progress: %.0f%%", task.getTaskProgress()));
                taskProgBadge.setStyle("-fx-font-size: 11px; -fx-text-fill: #00D2FF;");
                metaRow.getChildren().add(taskProgBadge);
            }
        }

        // Late addition indicator
        if (inst.isLateAddition()) {
            Label lateBadge = new Label("Added today (Excluded from % denominator)");
            lateBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: 600; -fx-text-fill: #F59E0B; -fx-background-color: rgba(245, 158, 11, 0.15); -fx-padding: 2 6; -fx-background-radius: 4px;");
            metaRow.getChildren().add(lateBadge);
        }

        infoBox.getChildren().addAll(titleLbl, metaRow);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // 3. Move Up / Down Buttons (Order Persistence)
        Button upBtn = new Button("▲");
        upBtn.getStyleClass().add("btn-icon");
        upBtn.setStyle("-fx-font-size: 9px; -fx-text-fill: #94A3B8; -fx-padding: 3 6;");
        upBtn.setDisable(index == 0);
        upBtn.setOnAction(e -> handleReorder(inst.getTaskId(), index - 1));

        Button downBtn = new Button("▼");
        downBtn.getStyleClass().add("btn-icon");
        downBtn.setStyle("-fx-font-size: 9px; -fx-text-fill: #94A3B8; -fx-padding: 3 6;");
        downBtn.setDisable(index == totalCount - 1);
        downBtn.setOnAction(e -> handleReorder(inst.getTaskId(), index + 1));

        VBox orderBox = new VBox(2, upBtn, downBtn);
        orderBox.setAlignment(Pos.CENTER);

        // 4. Details / Actions button
        Button detailsBtn = new Button("Details");
        detailsBtn.getStyleClass().add("btn-secondary");
        detailsBtn.setStyle("-fx-font-size: 12px; -fx-padding: 5 10;");
        detailsBtn.setOnAction(e -> handleTaskDetails(inst));

        Button deleteBtn = new Button();
        deleteBtn.getStyleClass().add("btn-icon");
        deleteBtn.setGraphic(Icons.getIcon("trash", 14, "#EF4444"));
        deleteBtn.setTooltip(new Tooltip("Delete Task"));
        deleteBtn.setOnAction(e -> handleDeleteTask(inst));

        row.getChildren().addAll(cb, infoBox, orderBox, detailsBtn, deleteBtn);
        return row;
    }

    private void handleAddTask() {
        try {
            List<Category> categories = registry.getCategoryRepository().getAll();
            List<Project> projects = registry.getProjectRepository().getAll();
            LocalDate logicalDate = registry.getCurrentLogicalDate();

            AddTaskDialog.TaskFormData fd = AddTaskDialog.show(primaryStage, categories, projects, null, logicalDate);
            if (fd != null && fd.task != null) {
                registry.getTaskService().createTask(fd.task, fd.schedule, logicalDate);
                refresh();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleTaskDetails(TaskDailyInstance inst) {
        try {
            Task task = registry.getTaskService().getTaskById(inst.getTaskId());
            if (task == null) return;

            LocalDate logicalDate = registry.getCurrentLogicalDate();
            TaskDetailsDialog.DetailResult res = TaskDetailsDialog.show(primaryStage, task, logicalDate);

            if (res.action == TaskDetailsDialog.DetailAction.UPDATE_PROGRESS) {
                registry.getTaskService().updateTaskProgress(task.getId(), res.newProgress);
                refresh();
            } else if (res.action == TaskDetailsDialog.DetailAction.TOGGLE_ACTIVE) {
                registry.getTaskService().toggleActive(task.getId(), !task.isActive(), logicalDate);
                refresh();
            } else if (res.action == TaskDetailsDialog.DetailAction.EDIT) {
                List<Category> categories = registry.getCategoryRepository().getAll();
                List<Project> projects = registry.getProjectRepository().getAll();
                AddTaskDialog.TaskFormData fd = AddTaskDialog.show(primaryStage, categories, projects, task, logicalDate);
                if (fd != null && fd.task != null) {
                    registry.getTaskService().updateTask(fd.task, fd.schedule, logicalDate);
                    refresh();
                }
            } else if (res.action == TaskDetailsDialog.DetailAction.DELETE) {
                handleDeleteTask(inst);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleDeleteTask(TaskDailyInstance inst) {
        try {
            LocalDate logicalDate = registry.getCurrentLogicalDate();
            Task task = registry.getTaskService().getTaskById(inst.getTaskId());

            boolean isRecurring = task != null && task.getSchedule() != null && task.getSchedule().getScheduleType() != TaskSchedule.ScheduleType.EVERY_DAY;

            DeleteChoice choice = ConfirmDialog.confirmRecurringTaskDelete(primaryStage, inst.getTaskNameSnapshot());
            if (choice == DeleteChoice.DELETE_OCCURRENCE) {
                registry.getTaskService().deleteOccurrence(inst.getTaskId(), logicalDate);
                refresh();
            } else if (choice == DeleteChoice.DELETE_ENTIRE_TASK) {
                registry.getTaskService().deleteTask(inst.getTaskId(), logicalDate);
                refresh();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleReorder(int taskId, int newPosition) {
        try {
            LocalDate logicalDate = registry.getCurrentLogicalDate();
            registry.getTaskDailyInstanceRepository().updateOrder(logicalDate, taskId, newPosition);
            registry.getTaskService().updateDisplayOrder(taskId, newPosition);
            refresh();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
