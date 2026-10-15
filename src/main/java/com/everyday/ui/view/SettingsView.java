package com.everyday.ui.view;

import com.everyday.model.AppSettings;
import com.everyday.model.Category;
import com.everyday.model.UserProfile;
import com.everyday.service.ServiceRegistry;
import com.everyday.ui.component.Icons;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

public class SettingsView extends ScrollPane {

    private final ServiceRegistry registry;
    private final Stage primaryStage;
    private final Runnable profileUpdateNotifier;

    private TextField dayStartTimeField;
    private CheckBox notificationsToggle;
    private VBox categoriesList;
    private TextField newCatNameField;
    private ColorPicker newCatColorPicker;

    public SettingsView(ServiceRegistry registry, Stage primaryStage, Runnable profileUpdateNotifier) {
        this.registry = registry;
        this.primaryStage = primaryStage;
        this.profileUpdateNotifier = profileUpdateNotifier;

        this.setFitToWidth(true);
        this.setStyle("-fx-background-color: transparent;");

        VBox content = new VBox(24);
        content.setPadding(new Insets(28, 36, 40, 36));

        // Header
        VBox titleBox = new VBox(4);
        Label title = new Label("Settings & Preferences");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Configure logical day boundary, categories, and backup management");
        subtitle.getStyleClass().add("section-subtitle");
        titleBox.getChildren().addAll(title, subtitle);
        content.getChildren().add(titleBox);

        // Sections
        content.getChildren().add(buildGeneralSettingsCard());
        content.getChildren().add(buildCategoriesCard());
        content.getChildren().add(buildDataBackupCard());
        content.getChildren().add(buildAboutCard());

        this.setContent(content);
        refresh();
    }

    private Node buildGeneralSettingsCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("glass-card");
        card.setPadding(new Insets(20));

        Label title = new Label("General & Day Boundary");
        title.getStyleClass().add("card-header-title");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);

        Label startLbl = new Label("Logical Day Start Time (HH:mm):");
        startLbl.setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: 600;");

        dayStartTimeField = new TextField("00:00");
        dayStartTimeField.setPrefWidth(100);

        Label noteLbl = new Label("• Default is 12:00 AM (00:00). Changing this boundary takes effect immediately without rewriting historical records.");
        noteLbl.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        notificationsToggle = new CheckBox("Enable In-App Reminders");
        notificationsToggle.setSelected(true);

        grid.add(startLbl, 0, 0);
        grid.add(dayStartTimeField, 1, 0);

        Button saveGeneralBtn = new Button("Save General Settings");
        saveGeneralBtn.getStyleClass().add("btn-primary");
        saveGeneralBtn.setOnAction(e -> handleSaveGeneralSettings());

        card.getChildren().addAll(title, grid, noteLbl, notificationsToggle, saveGeneralBtn);
        return card;
    }

    private Node buildCategoriesCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("glass-card");
        card.setPadding(new Insets(20));

        Label title = new Label("Custom Categories");
        title.getStyleClass().add("card-header-title");

        // Add Category row
        HBox addRow = new HBox(10);
        addRow.setAlignment(Pos.CENTER_LEFT);

        newCatNameField = new TextField();
        newCatNameField.setPromptText("New category name...");
        newCatNameField.setPrefWidth(220);

        newCatColorPicker = new ColorPicker(Color.web("#3B82F6"));

        Button addCatBtn = new Button("Add Category");
        addCatBtn.getStyleClass().add("btn-secondary");
        addCatBtn.setOnAction(e -> handleAddCategory());

        addRow.getChildren().addAll(newCatNameField, newCatColorPicker, addCatBtn);

        categoriesList = new VBox(8);

        card.getChildren().addAll(title, addRow, categoriesList);
        return card;
    }

    private Node buildDataBackupCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("glass-card");
        card.setPadding(new Insets(20));

        Label title = new Label("Data, Backup & Portability");
        title.getStyleClass().add("card-header-title");

        Label desc = new Label("EveryDay is local-first and offline-capable. Export backups as JSON or copy the SQLite database.");
        desc.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px;");

        HBox btnRow = new HBox(12);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        Button exportJsonBtn = new Button("Export Backup (JSON)");
        exportJsonBtn.getStyleClass().add("btn-primary");
        exportJsonBtn.setOnAction(e -> handleExportBackup());

        Button restoreJsonBtn = new Button("Restore Backup (JSON)");
        restoreJsonBtn.getStyleClass().add("btn-secondary");
        restoreJsonBtn.setOnAction(e -> handleRestoreBackup());

        Button exportDbBtn = new Button("Backup SQLite File (.db)");
        exportDbBtn.getStyleClass().add("btn-secondary");
        exportDbBtn.setOnAction(e -> handleExportDatabase());

        btnRow.getChildren().addAll(exportJsonBtn, restoreJsonBtn, exportDbBtn);

        card.getChildren().addAll(title, desc, btnRow);
        return card;
    }

    private Node buildAboutCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("glass-card");
        card.setPadding(new Insets(20));

        Label title = new Label("About EveryDay");
        title.getStyleClass().add("card-header-title");

        Label appInfo = new Label("EveryDay Dashboard v1.0.0 — Standalone Windows Productivity Suite\nBuilt with Java 26, JavaFX, SQLite & Clean Architecture\nPhilosophy: Plan → Do → Check → Analyze → Improve.");
        appInfo.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px; -fx-line-spacing: 4px;");

        card.getChildren().addAll(title, appInfo);
        return card;
    }

    public void refresh() {
        try {
            AppSettings settings = registry.getSettingsService().getSettings();
            dayStartTimeField.setText(settings.getDayStartTime());
            notificationsToggle.setSelected(settings.isNotificationsEnabled());

            // Refresh categories
            categoriesList.getChildren().clear();
            List<Category> cats = registry.getCategoryRepository().getAll();
            for (Category c : cats) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-background-color: #0D1527; -fx-padding: 8 12; -fx-border-color: #1E293B; -fx-border-radius: 6px; -fx-background-radius: 6px;");

                Label colorBadge = new Label("●");
                colorBadge.setStyle("-fx-text-fill: " + c.getColor() + "; -fx-font-size: 16px;");

                Label name = new Label(c.getName() + (c.isDefault() ? " (Default)" : ""));
                name.setStyle("-fx-text-fill: #F8FAFC; -fx-font-weight: 600;");
                HBox.setHgrow(name, Priority.ALWAYS);

                if (!c.isDefault()) {
                    Button delBtn = new Button();
                    delBtn.getStyleClass().add("btn-icon");
                    delBtn.setGraphic(Icons.getIcon("trash", 14, "#EF4444"));
                    delBtn.setOnAction(e -> handleDeleteCategory(c));
                    row.getChildren().addAll(colorBadge, name, delBtn);
                } else {
                    row.getChildren().addAll(colorBadge, name);
                }

                categoriesList.getChildren().add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleSaveGeneralSettings() {
        try {
            registry.getSettingsService().updateSettings(
                    dayStartTimeField.getText().trim(),
                    true,
                    notificationsToggle.isSelected()
            );
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Settings updated successfully.", ButtonType.OK);
            alert.initOwner(primaryStage);
            alert.showAndWait();
            refresh();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleAddCategory() {
        String name = newCatNameField.getText();
        if (name == null || name.trim().isEmpty()) return;

        try {
            Color color = newCatColorPicker.getValue();
            String hexColor = String.format("#%02X%02X%02X",
                    (int) (color.getRed() * 255),
                    (int) (color.getGreen() * 255),
                    (int) (color.getBlue() * 255));

            Category cat = new Category(0, name.trim(), hexColor, false);
            registry.getCategoryRepository().insert(cat);
            newCatNameField.clear();
            refresh();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleDeleteCategory(Category c) {
        if (ConfirmDialog.confirm(primaryStage, "Delete Category", "Delete category '" + c.getName() + "'?")) {
            try {
                registry.getCategoryRepository().delete(c.getId());
                refresh();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleExportBackup() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export EveryDay Backup");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("EveryDay Backup (*.json)", "*.json"));
        fc.setInitialFileName("everyday_backup.json");
        File file = fc.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                registry.getBackupService().exportBackupToJson(file);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Backup exported successfully to " + file.getName(), ButtonType.OK);
                alert.initOwner(primaryStage);
                alert.showAndWait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleRestoreBackup() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select EveryDay Backup File");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("EveryDay Backup (*.json)", "*.json"));
        File file = fc.showOpenDialog(primaryStage);
        if (file != null) {
            // Warn user first as required!
            boolean proceed = ConfirmDialog.confirm(primaryStage, "Restore Backup Warning",
                    "Restoring a backup will merge and update your current configuration, categories, tasks, and streaks. Are you sure you want to proceed?");
            if (proceed) {
                try {
                    registry.getBackupService().restoreBackupFromJson(file);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Backup restored successfully!", ButtonType.OK);
                    alert.initOwner(primaryStage);
                    alert.showAndWait();
                    refresh();
                    if (profileUpdateNotifier != null) profileUpdateNotifier.run();
                } catch (Exception e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Restore failed: " + e.getMessage(), ButtonType.OK);
                    alert.initOwner(primaryStage);
                    alert.showAndWait();
                }
            }
        }
    }

    private void handleExportDatabase() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export SQLite Database File");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite Database (*.db)", "*.db"));
        fc.setInitialFileName("everyday_backup.db");
        File file = fc.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                registry.getBackupService().exportDatabaseFile(file);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Database backup saved to " + file.getName(), ButtonType.OK);
                alert.initOwner(primaryStage);
                alert.showAndWait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
