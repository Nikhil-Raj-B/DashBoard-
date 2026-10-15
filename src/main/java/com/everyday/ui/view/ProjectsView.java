package com.everyday.ui.view;

import com.everyday.model.Project;
import com.everyday.model.Project.ProjectStatus;
import com.everyday.model.Task;
import com.everyday.service.ServiceRegistry;
import com.everyday.ui.component.Icons;
import com.everyday.ui.dialog.AddProjectDialog;
import com.everyday.ui.dialog.ConfirmDialog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.List;

public class ProjectsView extends ScrollPane {

    private final ServiceRegistry registry;
    private final Stage primaryStage;
    private GridPane projectsGrid;

    public ProjectsView(ServiceRegistry registry, Stage primaryStage) {
        this.registry = registry;
        this.primaryStage = primaryStage;

        this.setFitToWidth(true);
        this.setStyle("-fx-background-color: transparent;");

        VBox content = new VBox(24);
        content.setPadding(new Insets(28, 36, 40, 36));

        // 1. Header
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Projects");
        title.getStyleClass().add("section-title");

        Label subtitle = new Label("Long-term initiatives • Independent of daily productivity metrics");
        subtitle.getStyleClass().add("section-subtitle");
        titleBox.getChildren().addAll(title, subtitle);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button newProjBtn = new Button("New Project");
        newProjBtn.getStyleClass().add("btn-primary");
        newProjBtn.setGraphic(Icons.getIcon("plus", 16, Color.WHITE));
        newProjBtn.setOnAction(e -> handleAddProject());

        header.getChildren().addAll(titleBox, newProjBtn);
        content.getChildren().add(header);

        // 2. Projects Grid
        projectsGrid = new GridPane();
        projectsGrid.setHgap(20);
        projectsGrid.setVgap(20);
        content.getChildren().add(projectsGrid);

        this.setContent(content);
        refresh();
    }

    public void refresh() {
        projectsGrid.getChildren().clear();
        try {
            List<Project> projects = registry.getProjectService().getAllProjects();
            if (projects.isEmpty()) {
                VBox empty = new VBox(8);
                empty.getStyleClass().add("empty-state-box");
                empty.setPrefWidth(700);

                Label emptyT = new Label("Create your first project.");
                emptyT.getStyleClass().add("empty-state-title");

                Label emptyS = new Label("Projects help you organize multi-week milestones and long-term goals.");
                emptyS.getStyleClass().add("empty-state-subtitle");

                empty.getChildren().addAll(emptyT, emptyS);
                projectsGrid.add(empty, 0, 0);
            } else {
                int col = 0;
                int row = 0;
                for (Project p : projects) {
                    Node card = buildProjectCard(p);
                    projectsGrid.add(card, col, row);
                    GridPane.setHgrow(card, Priority.ALWAYS);
                    col++;
                    if (col > 1) {
                        col = 0;
                        row++;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Node buildProjectCard(Project project) throws Exception {
        VBox card = new VBox(14);
        card.getStyleClass().add("glass-card");
        card.setPrefWidth(420);
        card.setPadding(new Insets(20));

        // Top Row: Title + Status Badge
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLbl = new Label(project.getName());
        nameLbl.setStyle("-fx-font-size: 17px; -fx-font-weight: 700; -fx-text-fill: #F8FAFC;");
        HBox.setHgrow(nameLbl, Priority.ALWAYS);

        Label statusBadge = new Label(project.getStatus().name());
        statusBadge.setStyle(getStatusBadgeStyle(project.getStatus()));

        topRow.getChildren().addAll(nameLbl, statusBadge);

        // Description
        Label descLbl = new Label(project.getDescription() != null && !project.getDescription().isBlank() ? project.getDescription() : "No description provided.");
        descLbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px; -fx-wrap-text: true;");

        // Independent Progress Section
        VBox progBox = new VBox(6);
        HBox progHeader = new HBox();
        Label pTitle = new Label("Project Progress");
        pTitle.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px; -fx-font-weight: 600;");
        HBox.setHgrow(pTitle, Priority.ALWAYS);

        Label pVal = new Label(String.format("%.0f%%", project.getProgress()));
        pVal.setStyle("-fx-text-fill: #00D2FF; -fx-font-weight: 700; -fx-font-size: 13px;");
        progHeader.getChildren().addAll(pTitle, pVal);

        ProgressBar pb = new ProgressBar(project.getProgress() / 100.0);
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setPrefHeight(8);
        pb.getStyleClass().add("progress-bar");

        progBox.getChildren().addAll(progHeader, pb);

        // Timeline & Associated Tasks
        HBox metaRow = new HBox(16);
        Label dates = new Label("📅 " + project.getStartDate() + (project.getTargetDate() != null ? " → " + project.getTargetDate() : " (Ongoing)"));
        dates.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        List<Task> tasks = registry.getProjectService().getAssociatedTasks(project.getId());
        Label taskCount = new Label("📋 " + tasks.size() + " tasks associated");
        taskCount.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        metaRow.getChildren().addAll(dates, taskCount);

        // Card Actions
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("btn-secondary");
        editBtn.setStyle("-fx-font-size: 12px; -fx-padding: 4 10;");
        editBtn.setOnAction(e -> handleEditProject(project));

        Button deleteBtn = new Button();
        deleteBtn.getStyleClass().add("btn-icon");
        deleteBtn.setGraphic(Icons.getIcon("trash", 14, "#EF4444"));
        deleteBtn.setTooltip(new Tooltip("Delete Project"));
        deleteBtn.setOnAction(e -> handleDeleteProject(project));

        actions.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(topRow, descLbl, progBox, metaRow, actions);
        return card;
    }

    private String getStatusBadgeStyle(ProjectStatus status) {
        switch (status) {
            case ACTIVE:
                return "-fx-background-color: rgba(16, 185, 129, 0.15); -fx-text-fill: #34D399; -fx-font-weight: 700; -fx-font-size: 10px; -fx-padding: 3 8; -fx-background-radius: 6px;";
            case COMPLETED:
                return "-fx-background-color: rgba(59, 130, 246, 0.15); -fx-text-fill: #60A5FA; -fx-font-weight: 700; -fx-font-size: 10px; -fx-padding: 3 8; -fx-background-radius: 6px;";
            case PAUSED:
                return "-fx-background-color: rgba(245, 158, 11, 0.15); -fx-text-fill: #FBBF24; -fx-font-weight: 700; -fx-font-size: 10px; -fx-padding: 3 8; -fx-background-radius: 6px;";
            case CANCELLED:
                return "-fx-background-color: rgba(239, 68, 68, 0.15); -fx-text-fill: #F87171; -fx-font-weight: 700; -fx-font-size: 10px; -fx-padding: 3 8; -fx-background-radius: 6px;";
            default:
                return "-fx-background-color: #1E293B; -fx-text-fill: #94A3B8; -fx-font-size: 10px; -fx-padding: 3 8;";
        }
    }

    private void handleAddProject() {
        Project p = AddProjectDialog.show(primaryStage, null);
        if (p != null) {
            try {
                registry.getProjectService().createProject(p);
                refresh();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleEditProject(Project existing) {
        Project p = AddProjectDialog.show(primaryStage, existing);
        if (p != null) {
            try {
                registry.getProjectService().updateProject(p);
                refresh();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleDeleteProject(Project project) {
        if (ConfirmDialog.confirm(primaryStage, "Delete Project", "Are you sure you want to delete '" + project.getName() + "'? Associated tasks will remain standalone.")) {
            try {
                registry.getProjectService().deleteProject(project.getId());
                refresh();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
