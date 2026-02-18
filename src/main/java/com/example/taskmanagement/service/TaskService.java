package com.example.taskmanagement.service;

import com.example.taskmanagement.dto.TaskUpdateDTO;
import com.example.taskmanagement.model.Task;
import com.example.taskmanagement.model.TaskStatus;
import com.example.taskmanagement.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Objects;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;

    @Autowired
    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Optional<Task> getTaskById(@NonNull Long id) {
        return taskRepository.findById(id);
    }

    public List<Task> getTasksByStatus(@NonNull TaskStatus status) {
        return taskRepository.findByStatus(status);
    }

    public List<Task> searchTasksByTitle(@NonNull String title) {
        return taskRepository.findByTitleContainingIgnoreCase(title);
    }

    public List<Task> getFavoriteTasks() {
        return taskRepository.findByFavoriteTrue();
    }

    public Task createTask(@NonNull Task task) {
        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.PENDING);
        }
        return taskRepository.save(task);
    }

    public Optional<Task> updateTask(@NonNull Long id, @NonNull TaskUpdateDTO taskDetails) {
        return taskRepository.findById(id).map(task -> {
            if (taskDetails.getTitle() != null) {
                task.setTitle(taskDetails.getTitle());
            }
            if (taskDetails.getDescription() != null) {
                task.setDescription(taskDetails.getDescription());
            }
            if (taskDetails.getStatus() != null) {
                task.setStatus(taskDetails.getStatus());
            }
            if (taskDetails.getFavorite() != null) {
                task.setFavorite(taskDetails.getFavorite());
            }
            return taskRepository.save(Objects.requireNonNull(task));
        });
    }

    public Optional<Task> toggleFavorite(@NonNull Long id) {
        return taskRepository.findById(id).map(task -> {
            task.setFavorite(!task.getFavorite());
            return taskRepository.save(task);
        });
    }

    public boolean deleteTask(@NonNull Long id) {
        if (taskRepository.findById(id).isPresent()) {
            taskRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
