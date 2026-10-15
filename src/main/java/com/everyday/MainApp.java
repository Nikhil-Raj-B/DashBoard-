package com.everyday;

import com.everyday.database.DatabaseManager;
import com.everyday.model.Task;
import com.everyday.service.ServiceRegistry;
import com.everyday.ui.layout.MainLayout;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainApp extends Application {

    private ServiceRegistry registry;
    private Timeline reminderTimeline;

    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize Database
            DatabaseManager.initialize(null);

            // Initialize Services
            registry = ServiceRegistry.getInstance();

            // Build Main UI
            MainLayout mainLayout = new MainLayout(registry, primaryStage);

            Scene scene = new Scene(mainLayout, 1260, 820);
            scene.getStylesheets().add(getClass().getResource("/com/everyday/css/styles.css").toExternalForm());

            primaryStage.setTitle("EveryDay — Personal Productivity & Growth Dashboard");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1080);
            primaryStage.setMinHeight(720);

            // Setup reminder timer (polls every 30 seconds)
            setupReminderTimer();

            primaryStage.setOnCloseRequest(e -> {
                if (reminderTimeline != null) {
                    reminderTimeline.stop();
                }
                DatabaseManager.close();
            });

            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupReminderTimer() {
        reminderTimeline = new Timeline(new KeyFrame(Duration.seconds(30), e -> {
            try {
                if (registry != null && registry.getSettingsService().getSettings().isNotificationsEnabled()) {
                    String nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                    List<Task> allTasks = registry.getTaskService().getAllTasks();
                    registry.getNotificationService().checkReminders(allTasks, nowTime);
                }
            } catch (Exception ex) {
                // Ignore silent background check errors
            }
        }));
        reminderTimeline.setCycleCount(Timeline.INDEFINITE);
        reminderTimeline.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
