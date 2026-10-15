package com.everyday.ui.dialog;

import com.everyday.model.Project;
import com.everyday.model.Project.ProjectStatus;
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

public class AddProjectDialog {

    public static Project show(Stage owner, Project existingProject) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle(existingProject == null ? "New Project" : "Edit Project");

        VBox root = new VBox(14);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #131D31; -fx-pref-width: 480px;");

        Label titleLabel = new Label(existingProject == null ? "Create New Project" : "Edit Project");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #F8FAFC;");

        // 1. Name
        Label nameLbl = new Label("Project Name *");
        nameLbl.setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: 600;");
        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Build EveryDay App V1");
        if (existingProject != null) nameField.setText(existingProject.getName());

        // 2. Dates
        GridPane dateGrid = new GridPane();
        dateGrid.setHgap(12);
        dateGrid.setVgap(6);

        Label startLbl = new Label("Start Date");
        startLbl.setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: 600;");
        DatePicker startDatePicker = new DatePicker(existingProject != null ? existingProject.getStartDate() : LocalDate.now());
        startDatePicker.setMaxWidth(Double.MAX_VALUE);

        Label targetLbl = new Label("Target Date");
        targetLbl.setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: 600;");
        DatePicker targetDatePicker = new DatePicker(existingProject != null ? existingProject.getTargetDate() : null);
        targetDatePicker.setMaxWidth(Double.MAX_VALUE);

        dateGrid.add(startLbl, 0, 0);
        dateGrid.add(startDatePicker, 0, 1);
        dateGrid.add(targetLbl, 1, 0);
        dateGrid.add(targetDatePicker, 1, 1);

        // 3. Status & Progress
        GridPane statGrid = new GridPane();
        statGrid.setHgap(12);
        statGrid.setVgap(6);

        Label statusLbl = new Label("Status");
        statusLbl.setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: 600;");
        ComboBox<ProjectStatus> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(ProjectStatus.values());
        statusCombo.setValue(existingProject != null ? existingProject.getStatus() : ProjectStatus.ACTIVE);
        statusCombo.setMaxWidth(Double.MAX_VALUE);

        Label progLbl = new Label("Project Progress (%) - Independent of daily tasks");
        progLbl.setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: 600;");
        Slider progSlider = new Slider(0, 100, existingProject != null ? existingProject.getProgress() : 0);
        progSlider.setShowTickLabels(true);
        progSlider.setShowTickMarks(true);
        Label progValLbl = new Label(String.format("%.0f%%", progSlider.getValue()));
        progValLbl.setStyle("-fx-text-fill: #00D2FF; -fx-font-weight: 700;");
        progSlider.valueProperty().addListener((obs, oldV, newV) -> {
            progValLbl.setText(String.format("%.0f%%", newV.doubleValue()));
        });

        HBox progBox = new HBox(10, progSlider, progValLbl);
        progBox.setAlignment(Pos.CENTER_LEFT);

        statGrid.add(statusLbl, 0, 0);
        statGrid.add(statusCombo, 0, 1);

        // 4. Description & Notes
        Label descLbl = new Label("Description");
        descLbl.setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: 600;");
        TextField descField = new TextField();
        descField.setPromptText("Short goal summary");
        if (existingProject != null) descField.setText(existingProject.getDescription());

        Label notesLbl = new Label("Notes");
        notesLbl.setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: 600;");
        TextArea notesArea = new TextArea();
        notesArea.setPrefRowCount(2);
        if (existingProject != null) notesArea.setText(existingProject.getNotes());

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px;");
        errorLabel.setVisible(false);

        // Buttons
        HBox btnBox = new HBox(12);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");

        Button saveBtn = new Button(existingProject == null ? "Create Project" : "Save Changes");
        saveBtn.getStyleClass().add("btn-primary");

        final Project[] result = {null};

        cancelBtn.setOnAction(e -> dialog.close());

        saveBtn.setOnAction(e -> {
            String name = nameField.getText();
            if (name == null || name.trim().isEmpty()) {
                errorLabel.setText("Please enter a project name.");
                errorLabel.setVisible(true);
                return;
            }

            Project p = existingProject != null ? existingProject : new Project();
            p.setName(name.trim());
            p.setDescription(descField.getText());
            p.setStartDate(startDatePicker.getValue());
            p.setTargetDate(targetDatePicker.getValue());
            p.setStatus(statusCombo.getValue());
            p.setProgress(progSlider.getValue());
            p.setNotes(notesArea.getText());

            result[0] = p;
            dialog.close();
        });

        btnBox.getChildren().addAll(cancelBtn, saveBtn);

        root.getChildren().addAll(
                titleLabel, nameLbl, nameField,
                dateGrid, statGrid,
                progLbl, progBox,
                descLbl, descField,
                notesLbl, notesArea,
                errorLabel, btnBox
        );

        Scene scene = new Scene(root);
        scene.getStylesheets().add(AddProjectDialog.class.getResource("/com/everyday/css/styles.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();

        return result[0];
    }
}
