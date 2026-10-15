package com.everyday.service;

import com.everyday.model.Project;
import com.everyday.model.Project.ProjectStatus;
import com.everyday.model.Task;
import com.everyday.repository.ProjectRepository;
import com.everyday.repository.TaskRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public ProjectService(ProjectRepository projectRepository, TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    public List<Project> getAllProjects() throws SQLException {
        return projectRepository.getAll();
    }

    public Project getProjectById(int id) throws SQLException {
        return projectRepository.getById(id);
    }

    public Project createProject(Project project) throws SQLException {
        if (project.getName() == null || project.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be empty.");
        }
        return projectRepository.insert(project);
    }

    public void updateProject(Project project) throws SQLException {
        if (project.getName() == null || project.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be empty.");
        }
        projectRepository.update(project);
    }

    public void updateProgress(int projectId, double progress) throws SQLException {
        projectRepository.updateProgress(projectId, progress);
    }

    public void updateStatus(int projectId, ProjectStatus status) throws SQLException {
        Project project = projectRepository.getById(projectId);
        if (project != null) {
            project.setStatus(status);
            projectRepository.update(project);
        }
    }

    public void deleteProject(int projectId) throws SQLException {
        projectRepository.delete(projectId);
    }

    public List<Task> getAssociatedTasks(int projectId) throws SQLException {
        return taskRepository.getAll().stream()
                .filter(t -> t.getProjectId() != null && t.getProjectId() == projectId)
                .collect(Collectors.toList());
    }
}
