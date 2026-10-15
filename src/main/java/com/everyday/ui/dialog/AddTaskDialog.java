package com.everyday.ui.dialog;

import com.everyday.model.Category;
import com.everyday.model.Project;
import com.everyday.model.Task;
import com.everyday.model.TaskSchedule;
import com.everyday.model.TaskSchedule.ScheduleType;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AddTaskDialog {

    public static class TaskFormData {
        public Task task;
        public TaskSchedule schedule;
    }

    public static TaskFormData show(Stage owner, List<Category> categories, List<Project> projects, Task existingTask, LocalDate defaultDate) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle(existingTask == null ? "Add Task" : "Edit Task");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        VBox root = new VBox(14);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #131D31; -fx-pref-width: 520px;");
        scrollPane.setContent(root);

        Label titleLabel = new Label(existingTask == null ? "Create New Task" : "Edit Task");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #F8FAFC;");

        // 1. Task Name
        Label nameLbl = new Label("Task Name *");
        nameLbl.setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: 600;");
        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Solve 2 LeetCode problems");
        if (existingTask != null) nameField.setText(existingTask.getName());

        // 2. Category & Project
        GridPane catProjGrid = new GridPane();
        catProjGrid.setHgap(12);
        catProjGrid.setVgap(6);

        Label catLbl = new Label("Category *");
        catLbl.setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: 600;");
        ComboBox<Category> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll(categories);
        if (!categories.isEmpty()) {
            if (existingTask != null && existingTask.getCategory() != null) {
                for (Category c : categories) {
                    if (c.getId() == existingTask.getCategoryId()) {
                        categoryCombo.setValue(c);
                        break;
                    }
                }
            }
            if (categoryCombo.getValue() == null) categoryCombo.setValue(categories.get(0));
        }
        categoryCombo.setMaxWidth(Double.MAX_VALUE);

        Label projLbl = new Label("Project (Optional)");
        projLbl.setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: 600;");
        ComboBox<Project> projectCombo = new ComboBox<>();
        Project noneProj = new Project();
        noneProj.setId(-1);
        noneProj.setName("None (Standalone)");
        projectCombo.getItems().add(noneProj);
        projectCombo.getItems().addAll(projects);
        projectCombo.setValue(noneProj);
        if (existingTask != null && existingTask.getProjectId() != null) {
            for (Project p : projects) {
                if (p.getId() == existingTask.getProjectId()) {
                    projectCombo.setValue(p);
                    break;
                }
            }
        }
        projectCombo.setMaxWidth(Double.MAX_VALUE);

        catProjGrid.add(catLbl, 0, 0);
        catProjGrid.add(categoryCombo, 0, 1);
        catProjGrid.add(projLbl, 1, 0);
        catProjGrid.add(projectCombo, 1, 1);

        // 3. Time & Duration
        GridPane timeGrid = new GridPane();
        timeGrid.setHgap(12);
        timeGrid.setVgap(6);

        Label timeLbl = new Label("Scheduled Time (HH:mm)");
        timeLbl.setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: 600;");
        TextField timeField = new TextField("09:00");
        if (existingTask != null && existingTask.getScheduledTime() != null) timeField.setText(existingTask.getScheduledTime());

        Label durLbl = new Label("Duration (Minutes)");
        durLbl.setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: 600;");
        Spinner<Integer> durationSpinner = new Spinner<>(5, 720, existingTask != null ? existingTask.getDurationMinutes() : 30, 5);
        durationSpinner.setEditable(true);
        durationSpinner.setMaxWidth(Double.MAX_VALUE);

        timeGrid.add(timeLbl, 0, 0);
        timeGrid.add(timeField, 0, 1);
        timeGrid.add(durLbl, 1, 0);
        timeGrid.add(durationSpinner, 1, 1);

        // 4. Start Date & Multi-day End Date
        GridPane dateGrid = new GridPane();
        dateGrid.setHgap(12);
        dateGrid.setVgap(6);

        Label startLbl = new Label("Start Date");
        startLbl.setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: 600;");
        DatePicker startDatePicker = new DatePicker(existingTask != null ? existingTask.getStartDate() : (defaultDate != null ? defaultDate : LocalDate.now()));
        startDatePicker.setMaxWidth(Double.MAX_VALUE);

        Label multiDayLbl = new Label("Multi-day End Date (Optional)");
        multiDayLbl.setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: 600;");
        DatePicker endDatePicker = new DatePicker(existingTask != null ? existingTask.getMultiDayEndDate() : null);
        endDatePicker.setMaxWidth(Double.MAX_VALUE);

        dateGrid.add(startLbl, 0, 0);
        dateGrid.add(startDatePicker, 0, 1);
        dateGrid.add(multiDayLbl, 1, 0);
        dateGrid.add(endDatePicker, 1, 1);

        // 5. Schedule Type
        Label schedTypeLbl = new Label("Schedule Pattern");
        schedTypeLbl.setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: 600;");
        ComboBox<ScheduleType> schedTypeCombo = new ComboBox<>();
        schedTypeCombo.getItems().addAll(ScheduleType.values());
        schedTypeCombo.setValue(existingTask != null && existingTask.getSchedule() != null ? existingTask.getSchedule().getScheduleType() : ScheduleType.EVERY_DAY);
        schedTypeCombo.setMaxWidth(Double.MAX_VALUE);

        // Sub-panels for weekdays and custom intervals
        VBox weekdaysBox = new VBox(6);
        Label wdLbl = new Label("Select Active Days:");
        wdLbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
        HBox daysRow = new HBox(8);
        List<CheckBox> dayChecks = new ArrayList<>();
        String[] dayNames = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
        DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY};
        for (int i = 0; i < 7; i++) {
            CheckBox cb = new CheckBox(dayNames[i]);
            cb.setUserData(days[i].name());
            cb.setSelected(true);
            dayChecks.add(cb);
            daysRow.getChildren().add(cb);
        }
        weekdaysBox.getChildren().addAll(wdLbl, daysRow);

        VBox intervalBox = new VBox(6);
        Label intLbl = new Label("Every N Days:");
        intLbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
        Spinner<Integer> intervalSpinner = new Spinner<>(1, 365, 2);
        intervalBox.getChildren().addAll(intLbl, intervalSpinner);

        VBox customDatesBox = new VBox(6);
        Label cdLbl = new Label("Specific Dates (comma-separated YYYY-MM-DD):");
        cdLbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
        TextField customDatesField = new TextField();
        customDatesField.setPromptText("e.g. 2026-10-12, 2026-10-15");
        customDatesBox.getChildren().addAll(cdLbl, customDatesField);

        Runnable updateSchedVisibility = () -> {
            ScheduleType st = schedTypeCombo.getValue();
            weekdaysBox.setVisible(st == ScheduleType.SPECIFIC_WEEKDAYS);
            weekdaysBox.setManaged(st == ScheduleType.SPECIFIC_WEEKDAYS);
            intervalBox.setVisible(st == ScheduleType.CUSTOM_INTERVAL);
            intervalBox.setManaged(st == ScheduleType.CUSTOM_INTERVAL);
            customDatesBox.setVisible(st == ScheduleType.CUSTOM_DATES);
            customDatesBox.setManaged(st == ScheduleType.CUSTOM_DATES);
        };
        schedTypeCombo.setOnAction(e -> updateSchedVisibility.run());
        updateSchedVisibility.run();

        // Populate existing schedule details if editing
        if (existingTask != null && existingTask.getSchedule() != null) {
            TaskSchedule s = existingTask.getSchedule();
            if (s.getWeekdays() != null) {
                List<String> activeDays = Arrays.asList(s.getWeekdays().split(","));
                for (CheckBox cb : dayChecks) {
                    cb.setSelected(activeDays.contains((String) cb.getUserData()));
                }
            }
            if (s.getIntervalDays() > 0) intervalSpinner.getValueFactory().setValue(s.getIntervalDays());
            if (s.getCustomDates() != null) customDatesField.setText(s.getCustomDates());
        }

        // 6. Reminder & ON/OFF
        HBox togglesBox = new HBox(20);
        togglesBox.setAlignment(Pos.CENTER_LEFT);

        CheckBox activeToggle = new CheckBox("Active (ON)");
        activeToggle.setSelected(existingTask == null || existingTask.isActive());
        activeToggle.setTooltip(new Tooltip("Temporarily pause task (OFF) without deleting it. OFF tasks are excluded from daily tracking."));

        CheckBox reminderCheck = new CheckBox("Enable In-App Reminder");
        reminderCheck.setSelected(existingTask != null && existingTask.isReminderEnabled());

        TextField reminderTimeField = new TextField(existingTask != null && existingTask.getReminderTime() != null ? existingTask.getReminderTime() : "09:00");
        reminderTimeField.setPrefWidth(70);
        reminderTimeField.setPromptText("HH:mm");
        reminderTimeField.disableProperty().bind(reminderCheck.selectedProperty().not());

        togglesBox.getChildren().addAll(activeToggle, reminderCheck, reminderTimeField);

        // 7. Notes
        Label notesLbl = new Label("Notes / Details");
        notesLbl.setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: 600;");
        TextArea notesArea = new TextArea();
        notesArea.setPrefRowCount(2);
        notesArea.setPromptText("Additional instructions or reference details...");
        if (existingTask != null && existingTask.getNotes() != null) notesArea.setText(existingTask.getNotes());

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px;");
        errorLabel.setVisible(false);

        // Buttons
        HBox btnBox = new HBox(12);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");

        Button saveBtn = new Button(existingTask == null ? "Create Task" : "Save Changes");
        saveBtn.getStyleClass().add("btn-primary");

        final TaskFormData[] result = {null};

        cancelBtn.setOnAction(e -> dialog.close());

        saveBtn.setOnAction(e -> {
            String name = nameField.getText();
            if (name == null || name.trim().isEmpty()) {
                errorLabel.setText("Please enter a task name.");
                errorLabel.setVisible(true);
                return;
            }
            if (categoryCombo.getValue() == null) {
                errorLabel.setText("Please select a category.");
                errorLabel.setVisible(true);
                return;
            }

            Task task = existingTask != null ? existingTask : new Task();
            task.setName(name.trim());
            task.setCategoryId(categoryCombo.getValue().getId());
            task.setCategory(categoryCombo.getValue());

            Project selProj = projectCombo.getValue();
            if (selProj != null && selProj.getId() > 0) {
                task.setProjectId(selProj.getId());
                task.setProject(selProj);
            } else {
                task.setProjectId(null);
                task.setProject(null);
            }

            task.setScheduledTime(timeField.getText());
            task.setDurationMinutes(durationSpinner.getValue());
            task.setStartDate(startDatePicker.getValue());
            task.setMultiDayEndDate(endDatePicker.getValue());
            task.setActive(activeToggle.isSelected());
            task.setReminderEnabled(reminderCheck.isSelected());
            task.setReminderTime(reminderCheck.isSelected() ? reminderTimeField.getText() : null);
            task.setNotes(notesArea.getText());

            TaskSchedule schedule = new TaskSchedule();
            schedule.setScheduleType(schedTypeCombo.getValue());
            schedule.setStartDate(startDatePicker.getValue());
            schedule.setEndDate(endDatePicker.getValue());

            if (schedTypeCombo.getValue() == ScheduleType.SPECIFIC_WEEKDAYS) {
                String weekdays = dayChecks.stream()
                        .filter(CheckBox::isSelected)
                        .map(cb -> (String) cb.getUserData())
                        .collect(Collectors.joining(","));
                schedule.setWeekdays(weekdays);
            } else if (schedTypeCombo.getValue() == ScheduleType.CUSTOM_INTERVAL) {
                schedule.setIntervalDays(intervalSpinner.getValue());
            } else if (schedTypeCombo.getValue() == ScheduleType.CUSTOM_DATES) {
                schedule.setCustomDates(customDatesField.getText());
            }

            TaskFormData fd = new TaskFormData();
            fd.task = task;
            fd.schedule = schedule;
            result[0] = fd;

            dialog.close();
        });

        btnBox.getChildren().addAll(cancelBtn, saveBtn);

        root.getChildren().addAll(
                titleLabel, nameLbl, nameField,
                catProjGrid, timeGrid, dateGrid,
                schedTypeLbl, schedTypeCombo,
                weekdaysBox, intervalBox, customDatesBox,
                togglesBox, notesLbl, notesArea,
                errorLabel, btnBox
        );

        Scene scene = new Scene(scrollPane, 550, 680);
        scene.getStylesheets().add(AddTaskDialog.class.getResource("/com/everyday/css/styles.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();

        return result[0];
    }
}
