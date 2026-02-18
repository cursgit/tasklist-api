package com.example.taskmanagement.service;

import com.example.taskmanagement.dto.TaskUpdateDTO;
import com.example.taskmanagement.model.Task;
import com.example.taskmanagement.model.TaskStatus;
import com.example.taskmanagement.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @NonNull
    @SuppressWarnings("null")
    private static <T> T anyNonNull(Class<T> type) {
        return any(type);
    }

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createTask_setsDefaultStatusWhenNull() {
        Task input = new Task();
        input.setTitle("Write tests");
        input.setDescription("Add unit tests for service");
        input.setStatus(null);

        Task saved = new Task("Write tests", "Add unit tests for service", TaskStatus.PENDING);
        when(taskRepository.save(anyNonNull(Task.class))).thenReturn(saved);

        Task result = taskService.createTask(input);

        assertNotNull(result);
        assertEquals(TaskStatus.PENDING, result.getStatus());
        verify(taskRepository).save(anyNonNull(Task.class));
    }

    @Test
    void updateTask_updatesProvidedFieldsOnly() {
        Task existing = new Task("Old title", "Old description", TaskStatus.PENDING);
        existing.setId(1L);

        TaskUpdateDTO updates = new TaskUpdateDTO();
        updates.setTitle("New title");
        updates.setStatus(TaskStatus.IN_PROGRESS);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(anyNonNull(Task.class)))
            .thenAnswer(invocation -> invocation.getArgument(0, Task.class));

        Optional<Task> result = taskService.updateTask(1L, updates);

        assertTrue(result.isPresent());
        assertEquals("New title", result.get().getTitle());
        assertEquals("Old description", result.get().getDescription());
        assertEquals(TaskStatus.IN_PROGRESS, result.get().getStatus());
        verify(taskRepository).save(existing);
    }

    @Test
    void deleteTask_returnsFalseWhenMissing() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        boolean deleted = taskService.deleteTask(99L);

        assertFalse(deleted);
        verify(taskRepository, never()).delete(anyNonNull(Task.class));
    }

    @Test
    void getTasksByStatus_returnsRepositoryResults() {
        Task task = new Task("Task", "Desc", TaskStatus.COMPLETED);
        when(taskRepository.findByStatus(TaskStatus.COMPLETED)).thenReturn(List.of(task));

        List<Task> results = taskService.getTasksByStatus(TaskStatus.COMPLETED);

        assertEquals(1, results.size());
        assertEquals(TaskStatus.COMPLETED, results.get(0).getStatus());
    }

    @Test
    void getAllTasks_returnsAllTasks() {
        Task task1 = new Task("Task 1", "Desc 1", TaskStatus.PENDING);
        Task task2 = new Task("Task 2", "Desc 2", TaskStatus.COMPLETED);
        when(taskRepository.findAll()).thenReturn(List.of(task1, task2));

        List<Task> results = taskService.getAllTasks();

        assertEquals(2, results.size());
        verify(taskRepository).findAll();
    }

    @Test
    void getTaskById_returnsTask() {
        Task task = new Task("Task", "Desc", TaskStatus.PENDING);
        task.setId(1L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        Optional<Task> result = taskService.getTaskById(1L);

        assertTrue(result.isPresent());
        assertEquals("Task", result.get().getTitle());
        verify(taskRepository).findById(1L);
    }

    @Test
    void getTaskById_returnsEmptyWhenNotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Task> result = taskService.getTaskById(99L);

        assertFalse(result.isPresent());
        verify(taskRepository).findById(99L);
    }

    @Test
    void searchTasksByTitle_returnsMatchingTasks() {
        Task task1 = new Task("Write tests", "Unit tests", TaskStatus.PENDING);
        Task task2 = new Task("Write docs", "Documentation", TaskStatus.PENDING);
        when(taskRepository.findByTitleContainingIgnoreCase("write"))
            .thenReturn(List.of(task1, task2));

        List<Task> results = taskService.searchTasksByTitle("write");

        assertEquals(2, results.size());
        verify(taskRepository).findByTitleContainingIgnoreCase("write");
    }

    @Test
    void searchTasksByTitle_returnsEmptyWhenNoMatches() {
        when(taskRepository.findByTitleContainingIgnoreCase("nonexistent"))
            .thenReturn(List.of());

        List<Task> results = taskService.searchTasksByTitle("nonexistent");

        assertTrue(results.isEmpty());
        verify(taskRepository).findByTitleContainingIgnoreCase("nonexistent");
    }

    @Test
    void createTask_preservesProvidedStatus() {
        Task input = new Task("Task", "Desc", TaskStatus.IN_PROGRESS);
        when(taskRepository.save(anyNonNull(Task.class))).thenReturn(input);

        Task result = taskService.createTask(input);

        assertEquals(TaskStatus.IN_PROGRESS, result.getStatus());
        verify(taskRepository).save(input);
    }

    @Test
    void updateTask_returnsEmptyWhenTaskNotFound() {
        TaskUpdateDTO updates = new TaskUpdateDTO("New", "Desc", TaskStatus.COMPLETED);
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Task> result = taskService.updateTask(99L, updates);

        assertFalse(result.isPresent());
        verify(taskRepository, never()).save(anyNonNull(Task.class));
    }

    @Test
    void updateTask_doesNotUpdateNullFields() {
        Task existing = new Task("Old title", "Old description", TaskStatus.PENDING);
        existing.setId(1L);

        TaskUpdateDTO updates = new TaskUpdateDTO();
        updates.setTitle(null);
        updates.setDescription(null);
        updates.setStatus(null);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(anyNonNull(Task.class)))
            .thenAnswer(invocation -> invocation.getArgument(0, Task.class));

        Optional<Task> result = taskService.updateTask(1L, updates);

        assertTrue(result.isPresent());
        assertEquals("Old title", result.get().getTitle());
        assertEquals("Old description", result.get().getDescription());
        assertEquals(TaskStatus.PENDING, result.get().getStatus());
    }

    @Test
    void updateTask_updatesOnlyTitle() {
        Task existing = new Task("Old", "Desc", TaskStatus.PENDING);
        existing.setId(1L);

        TaskUpdateDTO updates = new TaskUpdateDTO();
        updates.setTitle("New Title");

        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(anyNonNull(Task.class)))
            .thenAnswer(invocation -> invocation.getArgument(0, Task.class));

        Optional<Task> result = taskService.updateTask(1L, updates);

        assertTrue(result.isPresent());
        assertEquals("New Title", result.get().getTitle());
        assertEquals("Desc", result.get().getDescription());
        assertEquals(TaskStatus.PENDING, result.get().getStatus());
    }

    @Test
    void deleteTask_returnsTrueWhenExists() {
        Task task = new Task("Task", "Desc", TaskStatus.PENDING);
        task.setId(1L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        boolean deleted = taskService.deleteTask(1L);

        assertTrue(deleted);
        verify(taskRepository).deleteById(1L);
    }

    @Test
    void getTasksByStatus_returnsEmptyListWhenNoTasks() {
        when(taskRepository.findByStatus(TaskStatus.CANCELLED)).thenReturn(List.of());

        List<Task> results = taskService.getTasksByStatus(TaskStatus.CANCELLED);

        assertTrue(results.isEmpty());
        verify(taskRepository).findByStatus(TaskStatus.CANCELLED);
    }

    @Test
    void getFavoriteTasks_returnsOnlyFavoriteTasks() {
        Task task1 = new Task("Favorite Task", "Important", TaskStatus.PENDING);
        task1.setFavorite(true);
        Task task2 = new Task("Another Favorite", "Also important", TaskStatus.COMPLETED);
        task2.setFavorite(true);
        when(taskRepository.findByFavoriteTrue()).thenReturn(List.of(task1, task2));

        List<Task> results = taskService.getFavoriteTasks();

        assertEquals(2, results.size());
        assertTrue(results.get(0).getFavorite());
        assertTrue(results.get(1).getFavorite());
        verify(taskRepository).findByFavoriteTrue();
    }

    @Test
    void getFavoriteTasks_returnsEmptyWhenNoFavorites() {
        when(taskRepository.findByFavoriteTrue()).thenReturn(List.of());

        List<Task> results = taskService.getFavoriteTasks();

        assertTrue(results.isEmpty());
        verify(taskRepository).findByFavoriteTrue();
    }

    @Test
    void toggleFavorite_togglesFalseToTrue() {
        Task task = new Task("Task", "Desc", TaskStatus.PENDING);
        task.setId(1L);
        task.setFavorite(false);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(anyNonNull(Task.class)))
            .thenAnswer(invocation -> invocation.getArgument(0, Task.class));

        Optional<Task> result = taskService.toggleFavorite(1L);

        assertTrue(result.isPresent());
        assertTrue(result.get().getFavorite());
        verify(taskRepository).save(task);
    }

    @Test
    void toggleFavorite_togglesTrueToFalse() {
        Task task = new Task("Task", "Desc", TaskStatus.PENDING);
        task.setId(1L);
        task.setFavorite(true);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(anyNonNull(Task.class)))
            .thenAnswer(invocation -> invocation.getArgument(0, Task.class));

        Optional<Task> result = taskService.toggleFavorite(1L);

        assertTrue(result.isPresent());
        assertFalse(result.get().getFavorite());
        verify(taskRepository).save(task);
    }

    @Test
    void toggleFavorite_returnsEmptyWhenTaskNotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Task> result = taskService.toggleFavorite(99L);

        assertFalse(result.isPresent());
        verify(taskRepository, never()).save(anyNonNull(Task.class));
    }

    @Test
    void updateTask_updatesFavoriteField() {
        Task existing = new Task("Task", "Desc", TaskStatus.PENDING);
        existing.setId(1L);
        existing.setFavorite(false);

        TaskUpdateDTO updates = new TaskUpdateDTO();
        updates.setFavorite(true);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(anyNonNull(Task.class)))
            .thenAnswer(invocation -> invocation.getArgument(0, Task.class));

        Optional<Task> result = taskService.updateTask(1L, updates);

        assertTrue(result.isPresent());
        assertTrue(result.get().getFavorite());
        verify(taskRepository).save(existing);
    }
}
