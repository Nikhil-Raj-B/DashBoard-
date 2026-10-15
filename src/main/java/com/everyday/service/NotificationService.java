package com.everyday.service;

import com.everyday.model.Task;
import javafx.application.Platform;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Simple V1 in-app notification and reminder dispatcher.
 */
public class NotificationService {

    private final Set<Integer> notifiedTasksToday = new HashSet<>();
    private Consumer<String> reminderCallback;

    public void setReminderCallback(Consumer<String> callback) {
        this.reminderCallback = callback;
    }

    public void checkReminders(List<Task> activeTasks, String currentTime) {
        if (reminderCallback == null || activeTasks == null || currentTime == null) {
            return;
        }

        for (Task task : activeTasks) {
            if (task.isReminderEnabled() && task.getReminderTime() != null) {
                if (task.getReminderTime().equals(currentTime) && !notifiedTasksToday.contains(task.getId())) {
                    notifiedTasksToday.add(task.getId());
                    String msg = "Reminder: " + task.getName() + " is scheduled for " + task.getScheduledTime();
                    Platform.runLater(() -> reminderCallback.accept(msg));
                }
            }
        }
    }

    public void resetForNewDay() {
        notifiedTasksToday.clear();
    }
}
