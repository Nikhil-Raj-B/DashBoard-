package com.everyday.service;

import com.everyday.model.Task;
import com.everyday.model.TaskSchedule;
import com.everyday.repository.TaskDailyInstanceRepository;
import com.everyday.repository.TaskRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskDailyInstanceRepository instanceRepository;
    private final DailyEngineService dailyEngineService;

    public TaskService(TaskRepository taskRepository,
                       TaskDailyInstanceRepository instanceRepository,
                       DailyEngineService dailyEngineService) {
        this.taskRepository = taskRepository;
        this.instanceRepository = instanceRepository;
        this.dailyEngineService = dailyEngineService;
    }

    public List<Task> getAllTasks() throws SQLException {
        return taskRepository.getAll();
    }

    public Task getTaskById(int id) throws SQLException {
        return taskRepository.getById(id);
    }

    /**
     * Creates a task with proper logical date tracking.
     * If created while the current day is already running, it becomes a late-addition for today.
     */
    public Task createTask(Task task, TaskSchedule schedule, LocalDate currentLogicalDate) throws SQLException {
        if (task.getName() == null || task.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Task name cannot be empty.");
        }
        if (task.getCreatedLogicalDate() == null) {
            task.setCreatedLogicalDate(currentLogicalDate);
        }
        Task created = taskRepository.insert(task, schedule);

        // Prepare/refresh today so the new task appears immediately in today's view if scheduled
        if (currentLogicalDate != null) {
            dailyEngineService.prepareDay(currentLogicalDate);
        }

        return created;
    }

    public void updateTask(Task task, TaskSchedule schedule, LocalDate currentLogicalDate) throws SQLException {
        if (task.getName() == null || task.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Task name cannot be empty.");
        }
        taskRepository.update(task, schedule);

        if (currentLogicalDate != null) {
            dailyEngineService.prepareDay(currentLogicalDate);
        }
    }

    public void deleteTask(int taskId, LocalDate currentLogicalDate) throws SQLException {
        taskRepository.delete(taskId);
        if (currentLogicalDate != null) {
            dailyEngineService.calculateAndSaveDailyRecord(currentLogicalDate, null);
        }
    }

    public void deleteOccurrence(int taskId, LocalDate logicalDate) throws SQLException {
        dailyEngineService.deleteOccurrence(logicalDate, taskId);
    }

    public void toggleActive(int taskId, boolean active, LocalDate currentLogicalDate) throws SQLException {
        taskRepository.updateActive(taskId, active);
        if (currentLogicalDate != null) {
            if (!active) {
                instanceRepository.deleteInstance(currentLogicalDate, taskId);
            }
            dailyEngineService.prepareDay(currentLogicalDate);
        }
    }

    public void updateDisplayOrder(int taskId, int newOrder) throws SQLException {
        taskRepository.updateDisplayOrder(taskId, newOrder);
    }

    public void updateTaskProgress(int taskId, double progress) throws SQLException {
        taskRepository.updateTaskProgress(taskId, progress);
    }
}
